(ns bulk-google-translate.background.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [goog.string :as gstring]
            [goog.string.format]
            [cljs.core.async :refer [<! chan >!]]
            [chromex.logging :refer-macros [log info warn error group group-end]]
            [chromex.chrome-event-channel :refer [make-chrome-event-channel]]
            [chromex.protocols.chrome-port :refer [on-disconnect! post-message! get-sender]]
            [chromex.ext.web-request :as web-request]
            [chromex.ext.tabs :as tabs]
            [chromex.ext.runtime :as runtime]
            [chromex.ext.downloads :refer-macros [download]]
            [bulk-google-translate.content-script.common :as common]
            [bulk-google-translate.background.storage :refer [store-words! next-word update-storage] :as storage]
            [cljs-time.core :as tt]
            [cljs-time.coerce :as tc]
            [reagent.session]
            [cemerick.url :refer [url]]
            ))

(def clients (atom []))

; -- clients manipulation ---------------------------------------------------------------------------------------------------

(defn add-client! [client]
  (log "BACKGROUND: client connected" (get-sender client))
  (on-disconnect! client (fn []
                           ;; https://github.com/binaryage/chromex/blob/master/src/lib/chromex/protocols/chrome_port.cljs
                           (prn "on disconnect callback !!!")
                           ;; cleanup
                           (swap! clients (fn [curr c] (->> curr (remove #(= % c)))) client)))
  (swap! clients conj client))

(defn remove-client! [client]
  (log "BACKGROUND: client disconnected" (get-sender client))
  (let [remove-item (fn [coll item] (remove #(identical? item %) coll))]
    (swap! clients remove-item client)))

(defn popup-predicate [client]
  (re-find #"popup.html" (-> client
                             get-sender
                             js->clj
                             (get "url"))))

(defn get-popup-client []
  (->> @clients
       (filter popup-predicate)
       first ;;this should only be one popup
       ))

(defn get-content-client []
  (->> @clients
       (filter (complement popup-predicate))
       first ;;this should only be one popup
       ))

; -- client event loop ------------------------------------------------------------------------------------------------------
(defn fetch-next-word [client]
  (go
    (let [_ (prn "inside fetch-next-word")
          [word word-entry] (<! (next-word))
          _ (prn "BACKGROUND: word: " word)
          _ (prn "BACKGROUND: word-entry: " word-entry)]
      (cond (= word storage/*DONE-FLAG*)
            (do
              (post-message! (get-popup-client)
                             (common/marshall {:type :done}))
              (post-message! client
                             (common/marshall {:type :done})))
            (and word word-entry)
            (do (prn "fetch-next-word: " word)
                (post-message! client
                               (common/marshall {:type :translate
                                                 :word word
                                                 :source (reagent.session/get :source)
                                                 :target (reagent.session/get :target)
                                                 })))
            ))))

(defn run-client-message-loop! [client]
  (prn "BACKGROUND: starting event loop for client:" (get-sender client))
  (go-loop []
    (when-some [message (<! client)]
      (prn  "BACKGROUND: got client message:" message "from" (get-sender client))
      (let [{:keys [type] :as whole-edn} (common/unmarshall message)]
        (cond (= type :init-translations) (do (prn "background: init-translations")
                                              (<! (store-words! whole-edn))
                                              (reagent.session/put! :source (:source whole-edn))
                                              (reagent.session/put! :target (:target whole-edn))
                                              (post-message! (get-content-client) (common/marshall {:type :done-init-translations}))
                                              )
              (= type :next-word) (do
                                    (prn "handling :next-word")
                                    (<! (fetch-next-word client)))
              (= type :success) (go
                                  (prn "handling success!! :" whole-edn)
                                  (let [{:keys [word]} whole-edn]
                                    (<! (update-storage word
                                                        "status" "translated"
                                                        "translated-ts" (tc/to-long (tt/now))))
                                    (prn "handling success: done with update-storage")
                                    (<! (fetch-next-word client)))
                                  )
              ))
      (recur))
    (log "BACKGROUND: leaving event loop for client:" (get-sender client))
    (remove-client! client)))

; -- event handlers ---------------------------------------------------------------------------------------------------------

(defn handle-client-connection! [client]
  (add-client! client)
  (run-client-message-loop! client))

(defn tell-clients-about-new-tab! []
  (doseq [client @clients]
    (post-message! client "a new tab was created")))

; -- main event loop --------------------------------------------------------------------------------------------------------
(def download-history (atom #{}))

(defn url->word [url]
  (-> url cemerick.url/url (get-in [:query "q"])))

(defn url->tl [url]
  (-> url cemerick.url/url (get-in [:query "tl"])))

(defn download-audio [url]
  (go
    (let [word (url->word url)]
      (download (clj->js {:url url
                          :filename (str word ".mp3")
                          :saveAs false
                          }))
      )))

(defn process-chrome-event [event-num event]
  (log (gstring/format "BACKGROUND: got chrome event (%05d)" event-num) event)
  (let [[event-id event-args] event]
    (case event-id
      ::runtime/on-connect (apply handle-client-connection! event-args)
      ::tabs/on-created (tell-clients-about-new-tab!)
      ::web-request/on-completed (let [url (-> event-args
                                               first
                                               js->clj
                                               (get "url"))
                                       ]
                                   (cond (clojure.string/includes? url "translate_tts")
                                         (if (not (contains? @download-history url))
                                           (do
                                             (prn ">> event-args" event-args)
                                             (prn ">> url: " url)
                                             (swap! download-history conj url)
                                             (download-audio url)
                                             (post-message! (get-content-client)
                                                            (common/marshall {:type :audio-downloaded
                                                                              :word (url->word url)})))
                                           (post-message! (get-content-client)
                                                          (common/marshall {:type :audio-downloaded
                                                                            :word (url->word url)})))

                                         (clojure.string/includes? (-> (cemerick.url/url url)
                                                                       :path)
                                                                   "single")
                                         (post-message! (get-content-client)
                                                        (common/marshall {:type :done-translating
                                                                          :word (url->word url)
                                                                          :tl (url->tl url)}))
                                         ))
      nil)))

(defn run-chrome-event-loop! [chrome-event-channel]
  (log "BACKGROUND: starting main event loop...")
  (go-loop [event-num 1]
    (when-some [event (<! chrome-event-channel)]
      (process-chrome-event event-num event)
      (recur (inc event-num)))
    (log "BACKGROUND: leaving main event loop")))

(defn boot-chrome-event-loop! []
  (let [chrome-event-channel (make-chrome-event-channel (chan))]
    (tabs/tap-all-events chrome-event-channel)
    (runtime/tap-all-events chrome-event-channel)
    (web-request/tap-on-completed-events chrome-event-channel (clj->js {"urls" ["<all_urls>"]}))
    (run-chrome-event-loop! chrome-event-channel)))

; -- main entry point -------------------------------------------------------------------------------------------------------

(defn init! []
  (prn "BACKGROUND: init")
  (boot-chrome-event-loop!))
