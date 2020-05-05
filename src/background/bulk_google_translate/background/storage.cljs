(ns bulk-google-translate.background.storage
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.core.async :refer [<! chan >!]]
            [chromex.logging :refer-macros [log info warn error group group-end]]
            [chromex.protocols.chrome-storage-area :as storage-area]
            [cognitect.transit :as t]
            [chromex.ext.storage :as storage]))

#_(defn test-storage! []
  (let [local-storage (storage/get-local)]
    (set local-storage #js {"key1" "string"
                            "key2" #js [1 2 3]
                            "key3" true
                            "key4" nil})
    (go
      (let [[[items] error] (<! (get local-storage))]
        (if error
          (error "fetch all error:" error)
          (log "fetch all:" items))))
    (go
      (let [[[items] error] (<! (get local-storage "key1"))]
        (if error
          (error "fetch key1 error:" error)
          (log "fetch key1:" items))))
    (go
      (let [[[items] error] (<! (get local-storage #js ["key2" "key3"]))]
        (if error
          (error "fetch key2 and key3 error:" error)
          (log "fetch key2 and key3:" items))))))

(def default-state {:target #{}
                    :source #{}})

(defn get-ui-state []
  (go
    (let [local-storage (storage/get-local)
          [[items] error] (<! (storage-area/get local-storage "ui-state"))
          ]
      (if error
        default-state
        (let [ui-state (js->clj items)
              rdr (t/reader :json)
              _ (prn ">> items: " items)]
          (if (empty? ui-state) default-state
              (t/read rdr (get ui-state "ui-state")))
          )))))

(defn set-ui-state [new-state]
  (let [local-storage (storage/get-local)
        w (t/writer :json)]
    (storage-area/set local-storage #js{"ui-state" (t/write w new-state)})
    ))

(defn clear-victims! []
  (let [local-storage (storage/get-local)]
    (storage-area/clear local-storage)))
