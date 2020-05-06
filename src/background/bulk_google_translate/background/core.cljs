(ns bulk-google-translate.background.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [goog.string :as gstring]
            [goog.string.format]
            [cljs.core.async :refer [<! chan]]
            [chromex.logging :refer-macros [log info warn error group group-end]]
            [chromex.chrome-event-channel :refer [make-chrome-event-channel]]
            [chromex.protocols.chrome-port :refer [post-message! get-sender]]
            [chromex.ext.web-request :as web-request]
            [chromex.ext.tabs :as tabs]
            [chromex.ext.runtime :as runtime]
            [chromex.ext.downloads :refer-macros [download]]
            [bulk-google-translate.content-script.common :as common]
            [bulk-google-translate.background.storage :as storage]
            ))

(def clients (atom []))

; -- clients manipulation ---------------------------------------------------------------------------------------------------

(defn add-client! [client]
  (log "BACKGROUND: client connected" (get-sender client))
  (swap! clients conj client))

(defn remove-client! [client]
  (log "BACKGROUND: client disconnected" (get-sender client))
  (let [remove-item (fn [coll item] (remove #(identical? item %) coll))]
    (swap! clients remove-item client)))

; -- client event loop ------------------------------------------------------------------------------------------------------

(defn run-client-message-loop! [client]
  (prn "BACKGROUND: starting event loop for client:" (get-sender client))
  (go-loop []
    (when-some [message (<! client)]
      (prn  "BACKGROUND: got client message:" message "from" (get-sender client))
      (let [{:keys [type]} (common/unmarshall message)]
        (cond (= type :init-translations) (prn "background: init-translations")
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

(defn download-audio [url]
  (go
    (let [;resp (<! (http/get url))
          ;; data-blob (js/Blob. (clj->js [(:body resp)])
          ;;                     (clj->js {:type "audio/mpeg-3"}))
          ;; url (js/URL.createObjectURL data-blob)
          ]
      (download (clj->js {:url url
                          :filename "zhui1.mp3"
                          :saveAs false
                          }))
      ;; (prn ">> audio: " (:body resp))
      ;; (:body resp)

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
                                   (when (and (clojure.string/includes? url "translate_tts")
                                              (not (contains? @download-history url)))
                                     (prn ">> event-args" event-args)
                                     (prn ">> url: " url)
                                     (swap! download-history conj url)
                                     (download-audio url)
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
  (log "BACKGROUND: init")
  (boot-chrome-event-loop!))
