(ns bulk-google-translate.background.storage
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.core.async :refer [<! chan >! close!]]
            [cljs-time.core :as t]
            [cljs-time.coerce :as tc]
            [chromex.logging :refer-macros [log info warn error group group-end]]
            [chromex.protocols.chrome-storage-area :as storage-area]
            [cognitect.transit :as transit]
            [chromex.ext.storage :as storage]))

(def ^:dynamic *DONE-FLAG* "**D0N3-FL@G**")

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

(defn store-words!
  [{:keys [data]}]
  (let [local-storage (storage/get-local)
        words (concat data (list *DONE-FLAG*))]
    (go-loop [[word & more] data
              idx 0]
      (if (nil? word)
        (prn "DONE storing source words")
        (let [[[items] error] (<! (storage-area/get local-storage word))]
          (if error
            (error (str "storing " word " :") error)
            (do (storage-area/set local-storage (clj->js {word {"submit-ts" (tc/to-long (t/now))
                                                                "status" "pending"
                                                                "idx" idx}}))
                (recur more (inc idx)))
            )))
      )))

(defn current-translation-attempt
  []
  (let [local-storage (storage/get-local)]
    (go
      (let [[[items] error] (<! (storage-area/get local-storage))]
        (->> items
             js->clj
             (filter (fn [[k v]]
                       (= "translating" (get v "status"))))
             first)
        ))
    ))

(defn update-storage [word & args]
  {:pre [(even? (count args))]}
  (let [kv-pairs (partition 2 args)
        local-storage (storage/get-local)
        ch (chan)]
    (go
      (let [[[items] error] (<! (storage-area/get local-storage word))]
        (if error
          (error (str "fetching " word ":") error)
          (let [entry (->> (js->clj items) vals first)
                r {word (->> kv-pairs
                            (reduce (fn [accum [k v]]
                                      (assoc accum k v))
                                    entry))
                   }]
            (storage-area/set local-storage (clj->js r))
            (>! ch r)
            ))))
    ch))

(defn fresh-new-translation []
  (let [local-storage (storage/get-local)
        ch (chan)]
    (go
      (let [[[items] error] (<! (storage-area/get local-storage))
            [word word-entry] (->> (or items '())
                                   js->clj
                                   (filter (fn [[k v]]
                                             (let [status (get v "status")]
                                               (= "pending" status))))
                                   (sort-by (fn [[_ v]] (get v "idx")))
                                   first)
            _ (when-not (nil? word-entry) (<! (update-storage word "status" "translating")))
            word-data (<! (current-translation-attempt))]
        (if (nil? word-data)
          (close! ch)
          (>! ch word-data))
        ))
    ch))

(defn next-word []
  (let [ch (chan)]
    (go
      (let [word-data (<! (current-translation-attempt))
            word-data (if (empty? word-data)
                        (<! (fresh-new-translation))
                        word-data)]
        (if (nil? word-data)
          (close! ch)
          (>! ch word-data))))
    ch
    ))

(defn get-ui-state []
  (go
    (let [local-storage (storage/get-local)
          [[items] error] (<! (storage-area/get local-storage "ui-state"))
          ]
      (if error
        default-state
        (let [ui-state (js->clj items)
              rdr (transit/reader :json)
              _ (prn ">> items: " items)]
          (if (empty? ui-state) default-state
              (transit/read rdr (get ui-state "ui-state")))
          )))))

(defn set-ui-state [new-state]
  (let [local-storage (storage/get-local)
        w (transit/writer :json)]
    (storage-area/set local-storage #js{"ui-state" (transit/write w new-state)})
    ))

(defn clear-words! []
  (let [local-storage (storage/get-local)]
    (storage-area/clear local-storage)))
