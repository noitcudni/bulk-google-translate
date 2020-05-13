(ns bulk-google-translate.content-script.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.core.async :refer [<! >! chan] :as async]
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

(defn exec-translation [http-sync-chan source target word]
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
            _ (prn ">> terse-translation: " terse-translation)
            parsed-data (->> (nodes (xpath "//table[@class='gt-baf-table']//tr"))
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
            _ (prn "parsed-data: " parsed-data)

            play-btn (sel1 ".src-tts")
            mouse-down-evt (js/MouseEvent. "mousedown" #js{:bubbles true})
            mouse-up-evt (js/MouseEvent. "mouseup" #js{:bubbles true})]
        (doto play-btn
          (.dispatchEvent mouse-down-evt)
          (.dispatchEvent mouse-up-evt))
        (<! (sync-http-audio-download http-sync-chan word)) ;; wait for audio-downloaded
        true)
      )))


(defn batch-exec-translation [http-sync-chan source targets word]
  (go
    (loop [[curr & more] targets]
      (if-not (nil? curr)
        (do (<! (exec-translation http-sync-chan source curr word))
            (recur more))
        true
        ))))

; -- a message loop ---------------------------------------------------------------------------------------------------------
(defn process-message! [http-sync-chan chan message]
  (let [_ (log "CONTENT SCRIPT: got message:" message)
        {:keys [type] :as whole-msg} (common/unmarshall message)]
    (cond (= type :done-init-translations) (do
                                             (post-message! chan (common/marshall {:type :next-word}))
                                             )
          (= type :translate) (do (let [{:keys [word source target]} whole-msg]
                                    (go
                                      ;; TODO how to handle erroring out of batch-exec-translation
                                      (prn ">>>>>>>>>>>>>>>>>>>>>>>>> start batch-exec-translation: " word)
                                      (<! (batch-exec-translation http-sync-chan source target word))
                                      (prn "<<<<<<<<<<<<<<<<<<<<<<<<< done batch-exec-translation: " word)
                                      (post-message! chan (common/marshall {:type :success :word word}))
                                      )))
          (= type :audio-downloaded) (go (>! http-sync-chan whole-msg))
          (= type :done-translating) (go (>! http-sync-chan whole-msg))
          )))

(defn run-message-loop! [message-channel]
  (log "CONTENT SCRIPT: starting message loop...")
  (let [http-sync-chan (chan 1)]
    (go-loop []
      (when-some [message (<! message-channel)]
        (process-message! http-sync-chan message-channel message)
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
