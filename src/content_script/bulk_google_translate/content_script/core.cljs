(ns bulk-google-translate.content-script.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.core.async :refer [<! >! chan] :as async]
            [chromex.logging :refer-macros [log info warn error group group-end]]
            [chromex.protocols.chrome-port :refer [post-message!]]
            [chromex.ext.runtime :as runtime :refer-macros [connect]]
            [dommy.core :refer-macros [sel sel1] :as dommy]
            [domina :refer [single-node nodes style styles]]
            [bulk-google-translate.content-script.common :as common]
            [domina.xpath :refer [xpath]]
            ))

(defn exec-translation [source target word]
  ;; (prn ">> single-node: " (dommy/text (single-node (xpath "//div[contains(@class,'result')]"))))
  ;; (prn ">> single-node: " (dommy/text (single-node (xpath "//span[contains(@class,'translation')]"))))
  ;; TODO: need to loop thru target
  (let [_ (prn ">> calling exec-translation")
        input (str "https://translate.google.com/#view=home&op=translate&sl=" source "&tl=" target "&text=" word)
        _ (set! (.. js/window -location -href) input)]
    (go
      (<! (async/timeout 2000))
      (let [_ (prn ">> terse translation" (dommy/text (sel1 ".translation")))
            play-btn (sel1 ".src-tts")
            mouse-down-evt (js/MouseEvent. "mousedown" #js{:bubbles true})
            mouse-up-evt (js/MouseEvent. "mouseup" #js{:bubbles true})]
        (doto play-btn
          (.dispatchEvent mouse-down-evt)
          (.dispatchEvent mouse-up-evt)))
      )))

; -- a message loop ---------------------------------------------------------------------------------------------------------
(defn process-message! [chan message]
  (let [_ (log "CONTENT SCRIPT: got message:" message)
        {:keys [type] :as whole-msg} (common/unmarshall message)]
    (cond (= type :done-init-translations) (do
                                             (post-message! chan (common/marshall {:type :next-word}))
                                             )
          (= type :translate) (do (prn "handling :translate : " whole-msg)
                                  (let [{:keys [word source target]} whole-msg]
                                    (exec-translation source target word)))
          )))

(defn run-message-loop! [message-channel]
  (log "CONTENT SCRIPT: starting message loop...")
  (go-loop []
    (when-some [message (<! message-channel)]
      (process-message! message-channel message)
      (recur))
    (log "CONTENT SCRIPT: leaving message loop")))

; -- a simple page analysis  ------------------------------------------------------------------------------------------------

(defn connect-to-background-page! []
  (let [background-port (runtime/connect)]
    ;; (post-message! background-port "hello from CONTENT SCRIPT!")
    (run-message-loop! background-port)))

; -- main entry point -------------------------------------------------------------------------------------------------------


(defn init! []
  (log "CONTENT SCRIPT: init")
  (connect-to-background-page!)
  #_(exec-translation))
