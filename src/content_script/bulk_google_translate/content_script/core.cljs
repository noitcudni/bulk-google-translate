(ns bulk-google-translate.content-script.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.core.async :refer [<! >! chan] :as async]
            [chromex.logging :refer-macros [log info warn error group group-end]]
            [chromex.protocols.chrome-port :refer [post-message!]]
            [chromex.ext.runtime :as runtime :refer-macros [connect]]
            [dommy.core :refer-macros [sel sel1] :as dommy]
            [domina :refer [single-node nodes style styles]]
            [domina.xpath :refer [xpath]]
            ))

; -- a message loop ---------------------------------------------------------------------------------------------------------
(defn process-message! [message]
  (log "CONTENT SCRIPT: got message:" message))

(defn run-message-loop! [message-channel]
  (log "CONTENT SCRIPT: starting message loop...")
  (go-loop []
    (when-some [message (<! message-channel)]
      (process-message! message)
      (recur))
    (log "CONTENT SCRIPT: leaving message loop")))

; -- a simple page analysis  ------------------------------------------------------------------------------------------------

(defn do-page-analysis! [background-port]
  (let [script-elements (.getElementsByTagName js/document "script")
        script-count (.-length script-elements)
        title (.-title js/document)
        msg (str "CONTENT SCRIPT: document '" title "' contains " script-count " script tags.")]
    (log msg)
    (post-message! background-port msg)))

(defn connect-to-background-page! []
  (let [background-port (runtime/connect)]
    (post-message! background-port "hello from CONTENT SCRIPT!")
    (run-message-loop! background-port)
    (do-page-analysis! background-port)))

; -- main entry point -------------------------------------------------------------------------------------------------------
(defn exec-translation []
  ;; (prn ">> single-node: " (dommy/text (single-node (xpath "//div[contains(@class,'result')]"))))
  ;; (prn ">> single-node: " (dommy/text (single-node (xpath "//span[contains(@class,'translation')]"))))

  (let [_ (prn ">> calling exec-translation")
        input "https://translate.google.com/#view=home&op=translate&sl=zh-CN&tl=en&text=錐"
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

(defn init! []
  (log "CONTENT SCRIPT: init")
  (connect-to-background-page!)
  (exec-translation))
