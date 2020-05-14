(ns bulk-google-translate.content-script.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.core.async :refer [<! >! chan alts!] :as async]
            [chromex.logging :refer-macros [log info warn error group group-end]]
            [chromex.protocols.chrome-port :refer [post-message!]]
            [chromex.ext.runtime :as runtime :refer-macros [connect]]
            [dommy.core :refer-macros [sel sel1] :as dommy]
            [domina :refer [single-node nodes style styles]]
            [domina.css]
            [bulk-google-translate.content-script.common :as common]
            [domina.xpath :refer [xpath]]
            [goog.dom :as dom]
            ))

(defn sync-http [msg-type ch word & {:keys [target]}]
  (go-loop []
    (let [data (<! ch)]
      (if (and (= word (:word data))
               (= msg-type (:type data))
               (= target (:tl data)))
        data
        (recur)
        ))))

(def sync-http-translate (partial sync-http :done-translating))
(def sync-http-audio-download (partial sync-http :audio-downloaded))

(defn sync-node-helper
  "This is unfortunate. alts! doens't close other channels"
  [dom-fn & xpath-strs]
  (go-loop []
    (let [_ (prn ">> sync-node-helper..")
          n (->> xpath-strs
                 (map (fn [xpath-str]
                        (dom-fn (xpath xpath-str))
                        ))
                 (filter #(some? %))
                 first)]
      (if (nil? n)
        (do (<! (async/timeout 300))
            (recur))
        n)
      )))

(def sync-single-node (partial sync-node-helper single-node))
(def sync-nodes (partial sync-node-helper nodes))

(defn exec-translation [http-sync-chan mp3-sync-chan source target word]
  ;; (prn ">> single-node: " (dommy/text (single-node (xpath "//div[contains(@class,'result')]"))))
  ;; (prn ">> single-node: " (dommy/text (single-node (xpath "//span[contains(@class,'translation')]"))))
  ;; TODO: download the audio only once.
  ;; Don't proceed until the audio is downloaded
  (let [_ (prn ">> calling exec-translation : word: " word " | target: " target)
        input (str "https://translate.google.com/#view=home&op=translate&sl=" source "&tl=" target "&text=" word)
        _ (set! (.. js/window -location -href) input)]
    (go
      (let [sync-data (<! (sync-http-translate http-sync-chan word :target target))
            terse-translation (dommy/text (sel1 ".translation"))
            detailed-translation-table-el (single-node (xpath "//div[contains(@class, 'gt-cd-baf')]"))
            detailed-translation? (= "block" (-> detailed-translation-table-el
                                                 js/window.getComputedStyle
                                                 (aget "display")))
            parsed-data (if detailed-translation?
                          (->> (nodes (xpath "//table[@class='gt-baf-table']//tr"))
                               reverse
                               (map #(dom/getElementsByTagNameAndClass "div" "gt-baf-cell" %))
                               (reduce (fn [accum cells]
                                         (if-let [header-cell (dom/getElementByClass "gt-cd-pos" (first cells))]
                                           ;; header
                                           (conj accum {(dom/getTextContent header-cell) []})
                                           ;; details
                                           (let [detailed-translation (dom/getElementByClass "gt-baf-word-clickable" (first cells))
                                                 source-lang-words (dom/getElementsByClass "gt-baf-back" (second cells))
                                                 k (->> accum last keys first)]
                                             (conj (->> accum
                                                        butlast
                                                        (into []))
                                                   (update (last accum) k conj {(dom/getTextContent detailed-translation)
                                                                                (->> source-lang-words (mapv #(dom/getTextContent %)))}))
                                             ))) []))
                          [])
            play-btn (<! (sync-single-node "//div[contains(@class, 'src-tts') and @aria-pressed='false']"))

            mouse-down-evt (js/MouseEvent. "mousedown" #js{:bubbles true})
            mouse-up-evt (js/MouseEvent. "mouseup" #js{:bubbles true})
            download-audio-ch (sync-http-audio-download mp3-sync-chan word)

            _ (doto play-btn
                (.dispatchEvent mouse-down-evt)
                (.dispatchEvent mouse-up-evt))

            ;; wait for audio-downloaded
            ;; try again if it gets stuck
            audio-meta (loop [[v _] (alts! [(async/timeout 1000) download-audio-ch])]
                         (if (= (:type v) :audio-downloaded)
                           v
                           (do
                             (doto play-btn
                               (.dispatchEvent mouse-down-evt)
                               (.dispatchEvent mouse-up-evt))
                             (recur (alts! [(async/timeout 1000) download-audio-ch])))))
            r {:word word
               :source source
               :target target
               :filename (:filename audio-meta)
               :translation {:abridged terse-translation
                             :detailed parsed-data}
               }
            _ (prn ">> r: " r)
            ]
        r
        )
      )))


(defn batch-exec-translation [http-sync-chan mp3-sync-chan source targets word]
  (go
    (loop [[curr & more] targets]
      (if-not (nil? curr)
        (do (<! (exec-translation http-sync-chan mp3-sync-chan source curr word))
            (recur more))
        true
        ))))

; -- a message loop ---------------------------------------------------------------------------------------------------------
(defn process-message! [http-sync-chan mp3-sync-chan chan message]
  (let [_ (log "CONTENT SCRIPT: got message:" message)
        {:keys [type] :as whole-msg} (common/unmarshall message)]
    (cond (= type :done-init-translations) (do
                                             (post-message! chan (common/marshall {:type :next-word}))
                                             (set! (.. js/window -location -href) "https://translate.google.com/#/view=home")
                                             )
          (= type :translate) (do (let [{:keys [word source target]} whole-msg]
                                    (go
                                      ;; TODO how to handle erroring out of batch-exec-translation
                                      (prn ">>>>>>>>>>>>>>>>>>>>>>>>> start batch-exec-translation: " word)
                                      (<! (batch-exec-translation http-sync-chan mp3-sync-chan source target word))
                                      (prn "<<<<<<<<<<<<<<<<<<<<<<<<< done batch-exec-translation: " word)
                                      (post-message! chan (common/marshall {:type :success :word word}))
                                      )))
          (= type :audio-downloaded) (go (>! mp3-sync-chan whole-msg))
          (= type :done-translating) (go (>! http-sync-chan whole-msg))
          (= type :done) (prn "all done!")
          )))

(defn run-message-loop! [message-channel]
  (log "CONTENT SCRIPT: starting message loop...")
  (let [http-sync-chan (chan)
        mp3-sync-chan (chan)]
    (go-loop []
      (when-some [message (<! message-channel)]
        (process-message! http-sync-chan mp3-sync-chan message-channel message)
        (recur))
      (log "CONTENT SCRIPT: leaving message loop"))))

; -- a simple page analysis  ------------------------------------------------------------------------------------------------

(defn connect-to-background-page! []
  (let [background-port (runtime/connect)]
    ;; (post-message! background-port "hello from CONTENT SCRIPT!")
    (run-message-loop! background-port)))

; -- main entry point -------------------------------------------------------------------------------------------------------


(defn init! []
  (log "CONTENT SCRIPT: init")
  (connect-to-background-page!)
  ;; (exec-translation "zh-CN" "en" "母親節擁抱老媽安全嗎")
  )
