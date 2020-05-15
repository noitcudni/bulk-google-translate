(ns bulk-google-translate.popup.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [reagent.ratom :refer [reaction]])
  (:require [cljs.core.async :refer [<! chan put!] :as async]
            [reagent.core :as reagent :refer [atom]]
            [chromex.logging :refer-macros [log info warn error group group-end]]
            [chromex.protocols.chrome-port :refer [post-message!]]
            [testdouble.cljs.csv :as csv]
            [re-com.core :as recom]
            [reagent.core :as reagent :refer [atom]]
            [reagent.session]
            [domina.xpath :refer [xpath]]
            [domina :refer [single-node nodes]]
            [bulk-google-translate.background.storage :as storage]
            [bulk-google-translate.content-script.common :as common]
            [chromex.ext.runtime :as runtime :refer-macros [connect]]))

(def upload-chan (chan 1 (map (fn [e]
                                (let [target (.-currentTarget e)
                                      file (-> target .-files (aget 0))]
                                  (set! (.-value target) "")
                                  file
                                  )))))
(def read-chan (chan 1 (map #(-> % .-target .-result js->clj))))

; -- a message loop ---------------------------------------------------------------------------------------------------------

(defn process-message! [message]
  (log "POPUP: got message:" message))

(defn run-message-loop! [message-channel]
  (log "POPUP: starting message loop...")
  (go-loop []
    (when-some [message (<! message-channel)]
      (process-message! message)
      (recur))
    (log "POPUP: leaving message loop")))

(defn connect-to-background-page! [background-port]
  ;; (post-message! background-port "hello from POPUP!")
  (run-message-loop! background-port))

(defn cb-handler-fn [type iso]
  (fn [checked?]
    (cond (= type :target)
          (if checked?
            (reagent.session/update! :target conj iso)
            (reagent.session/update! :target disj iso))
          (= type :source)
          (if checked?
            (reagent.session/put! :source #{iso})
            (reagent.session/put! :source #{})))
    ))

(defn lang-option-pane [type]
  (let [all-iso-ratom [{
                        :label "Afrikaans"
                        :iso "af"
                        :ratom (reaction (contains? (reagent.session/get type) "af"))}
                       {:label "Albanian"
                        :iso "sq"
                        :ratom (reaction (contains? (reagent.session/get type) "sq"))}
                       {:label "Amharic"
                        :iso "am"
                        :ratom (reaction (contains? (reagent.session/get type) "am"))}
                       {:label "Arabic"
                        :iso "ar"
                        :ratom (reaction (contains? (reagent.session/get type) "ar"))}
                       {:label "Armenian"
                        :iso "hy"
                        :ratom (reaction (contains? (reagent.session/get type) "hy"))}
                       {:label "Azerbaijani"
                        :iso "az"
                        :ratom (reaction (contains? (reagent.session/get type) "az"))}
                       {:label "Basque"
                        :iso "eu"
                        :ratom (reaction (contains? (reagent.session/get type) "eu"))}
                       {:label "Belarusian"
                        :iso "be"
                        :ratom (reaction (contains? (reagent.session/get type) "be"))}
                       {:label "Bengali"
                        :iso "bn"
                        :ratom (reaction (contains? (reagent.session/get type) "bn"))}
                       {:label "Bosnian"
                        :iso "bs"
                        :ratom (reaction (contains? (reagent.session/get type) "bs"))}
                       {:label "Bulgarian"
                        :iso "bg"
                        :ratom (reaction (contains? (reagent.session/get type) "bg"))}
                       {:label "Catalan"
                        :iso "ca"
                        :ratom (reaction (contains? (reagent.session/get type) "ca"))}
                       {:label "Cebuano"
                        :iso "ceb"
                        :ratom (reaction (contains? (reagent.session/get type) "ceb"))}
                       {:label "Chichewa"
                        :iso "ny"
                        :ratom (reaction (contains? (reagent.session/get type) "ny"))}
                       {:label "Chinese"
                        :iso  "zh-CN"
                        :ratom (reaction (contains? (reagent.session/get type) "zh-CN"))}
                       {:label "Corsican"
                        :iso "co"
                        :ratom (reaction (contains? (reagent.session/get type) "co"))}
                       {:label "Croatian"
                        :iso "hr"
                        :ratom (reaction (contains? (reagent.session/get type) "hr"))}
                       {:label "Czech"
                        :iso "cs"
                        :ratom (reaction (contains? (reagent.session/get type) "cs"))}

                       {:label "Danish"
                        :iso "da"
                        :ratom (reaction (contains? (reagent.session/get type) "da"))}

                       {:label "Dutch"
                        :iso "nl"
                        :ratom (reaction (contains? (reagent.session/get type) "nl"))}

                       {:label "English"
                        :iso "en"
                        :ratom (reaction (contains? (reagent.session/get type) "en"))}

                       {:label "Esperanto"
                        :iso "eo"
                        :ratom (reaction (contains? (reagent.session/get type) "eo"))}

                       {:label "Estonian"
                        :iso "et"
                        :ratom (reaction (contains? (reagent.session/get type) "et"))}

                       {:label "Filipino"
                        :iso "tl"
                        :ratom (reaction (contains? (reagent.session/get type) "tl"))}

                       {:label "Finnish"
                        :iso "fi"
                        :ratom (reaction (contains? (reagent.session/get type) "fi"))}

                       {:label "French"
                        :iso "fr"
                        :ratom (reaction (contains? (reagent.session/get type) "fr"))}

                       {:label "Frisian"
                        :iso "fy"
                        :ratom (reaction (contains? (reagent.session/get type) "fy"))}

                       {:label "Galician"
                        :iso "gl"
                        :ratom (reaction (contains? (reagent.session/get type) "gl"))}

                       {:label "Georgian"
                        :iso "ka"
                        :ratom (reaction (contains? (reagent.session/get type) "ka"))}

                       {:label "German"
                        :iso "de"
                        :ratom (reaction (contains? (reagent.session/get type) "de"))}

                       {:label "Greek"
                        :iso "el"
                        :ratom (reaction (contains? (reagent.session/get type) "el"))}

                       {:label "Gujarati"
                        :iso "gu"
                        :ratom (reaction (contains? (reagent.session/get type) "gu"))}
                       {:label "Haitian Creole"
                        :iso "ht"
                        :ratom (reaction (contains? (reagent.session/get type) "ht"))}

                       {:label "Hausa"
                        :iso "ha"
                        :ratom (reaction (contains? (reagent.session/get type) "ha"))}

                       {:label "Hawaiian"
                        :iso  "haw"
                        :ratom (reaction (contains? (reagent.session/get type) "haw"))}

                       {:label "Hebrew"
                        :iso "iw"
                        :ratom (reaction (contains? (reagent.session/get type) "iw"))}

                       {:label "Hindi"
                        :iso "hi"
                        :ratom (reaction (contains? (reagent.session/get type) "hi"))}

                       {:label "Hmong"
                        :iso  "hmn"
                        :ratom (reaction (contains? (reagent.session/get type) "hmn"))}

                       {:label "Hungarian"
                        :iso "hu"
                        :ratom (reaction (contains? (reagent.session/get type) "hu"))}

                       {:label "Icelandic"
                        :iso "is"
                        :ratom (reaction (contains? (reagent.session/get type) "is"))}

                       {:label "Igbo"
                        :iso "ig"
                        :ratom (reaction (contains? (reagent.session/get type) "ig"))}

                       {:label "Indonesian"
                        :iso "id"
                        :ratom (reaction (contains? (reagent.session/get type) "id"))}

                       {:label "Irish"
                        :iso "ga"
                        :ratom (reaction (contains? (reagent.session/get type) "ga"))}

                       {:label "Italian"
                        :iso "it"
                        :ratom (reaction (contains? (reagent.session/get type) "it"))}

                       {:label "Japanese"
                        :iso "ja"
                        :ratom (reaction (contains? (reagent.session/get type) "ja"))}

                       {:label "Javanese"
                        :iso "jv"
                        :ratom (reaction (contains? (reagent.session/get type) "jv"))}

                       {:label "Kannada"
                        :iso "kn"
                        :ratom (reaction (contains? (reagent.session/get type) "kn"))}

                       {:label "Kazakh"
                        :iso "kk"
                        :ratom (reaction (contains? (reagent.session/get type) "kk"))}

                       {:label "Khmer"
                        :iso "km"
                        :ratom (reaction (contains? (reagent.session/get type) "km"))}

                       {:label "Kinyarwanda"
                        :iso "rw"
                        :ratom (reaction (contains? (reagent.session/get type) "rw"))}

                       {:label "Korean"
                        :iso "ko"
                        :ratom (reaction (contains? (reagent.session/get type) "ko"))}

                       {:label "Kurdish"
                        :iso "ku"
                        :ratom (reaction (contains? (reagent.session/get type) "ku"))}

                       {:label "Kyrgyz"
                        :iso "ky"
                        :ratom (reaction (contains? (reagent.session/get type) "ky"))}

                       {:label "Lao"
                        :iso "lo"
                        :ratom (reaction (contains? (reagent.session/get type) "lo"))}

                       {:label "Latin"
                        :iso "la"
                        :ratom (reaction (contains? (reagent.session/get type) "la"))}

                       {:label "Latvian"
                        :iso "lv"
                        :ratom (reaction (contains? (reagent.session/get type) "lv"))}

                       {:label "Lithuanian"
                        :iso "lt"
                        :ratom (reaction (contains? (reagent.session/get type) "lt"))}

                       {:label "Luxembourgish"
                        :iso "lb"
                        :ratom (reaction (contains? (reagent.session/get type) "lb"))}

                       {:label "Macedonian"
                        :iso "mk"
                        :ratom (reaction (contains? (reagent.session/get type) "mk"))}

                       {:label "Malagasy"
                        :iso "mg"
                        :ratom (reaction (contains? (reagent.session/get type) "mg"))}

                       {:label "Malay"
                        :iso "ms"
                        :ratom (reaction (contains? (reagent.session/get type) "ms"))}

                       {:label "Malayalam"
                        :iso "ml"
                        :ratom (reaction (contains? (reagent.session/get type) "ml"))}

                       {:label "Maltese"
                        :iso "mt"
                        :ratom (reaction (contains? (reagent.session/get type) "mt"))}

                       {:label "Maori"
                        :iso "mi"
                        :ratom (reaction (contains? (reagent.session/get type) "mi"))}

                       {:label "Marathi"
                        :iso "mr"
                        :ratom (reaction (contains? (reagent.session/get type) "mr"))}

                       {:label "Mongolian"
                        :iso "mn"
                        :ratom (reaction (contains? (reagent.session/get type) "mn"))}
                       {:label "Myanmar (Burmese)"
                        :iso "my"
                        :ratom (reaction (contains? (reagent.session/get type) "my"))}

                       {:label "Nepali"
                        :iso "ne"
                        :ratom (reaction (contains? (reagent.session/get type) "ne"))}

                       {:label "Norwegian"
                        :iso "no"
                        :ratom (reaction (contains? (reagent.session/get type) "no"))}

                       {:label "Odia"
                        :iso "or"
                        :ratom (reaction (contains? (reagent.session/get type) "or"))}

                       {:label "Pashto"
                        :iso "ps"
                        :ratom (reaction (contains? (reagent.session/get type) "ps"))}

                       {:label "Persian"
                        :iso "fa"
                        :ratom (reaction (contains? (reagent.session/get type) "fa"))}

                       {:label "Polish"
                        :iso "pl"
                        :ratom (reaction (contains? (reagent.session/get type) "pl"))}

                       {:label "Portuguese"
                        :iso "pt"
                        :ratom (reaction (contains? (reagent.session/get type) "pt"))}

                       {:label "Punjabi"
                        :iso "pa"
                        :ratom (reaction (contains? (reagent.session/get type) "pa"))}

                       {:label "Romanian"
                        :iso "ro"
                        :ratom (reaction (contains? (reagent.session/get type) "ro"))}

                       {:label "Russian"
                        :iso "ru"
                        :ratom (reaction (contains? (reagent.session/get type) "ru"))}

                       {:label "Samoan"
                        :iso "sm"
                        :ratom (reaction (contains? (reagent.session/get type) "sm"))}

                       {:label "Scots Gaelic"
                        :iso "gd"
                        :ratom (reaction (contains? (reagent.session/get type) "gd"))}

                       {:label "Serbian"
                        :iso "sr"
                        :ratom (reaction (contains? (reagent.session/get type) "sr"))}

                       {:label "Sesotho"
                        :iso "st"
                        :ratom (reaction (contains? (reagent.session/get type) "st"))}

                       {:label "Shona"
                        :iso "sn"
                        :ratom (reaction (contains? (reagent.session/get type) "sn"))}

                       {:label "Sindhi"
                        :iso "sd"
                        :ratom (reaction (contains? (reagent.session/get type) "sd"))}
                       {:label "Sinhala (Sinhalese)"
                        :iso "si"
                        :ratom (reaction (contains? (reagent.session/get type) "si"))}

                       {:label "Slovak"
                        :iso "sk"
                        :ratom (reaction (contains? (reagent.session/get type) "sk"))}

                       {:label "Slovenian"
                        :iso "sl"
                        :ratom (reaction (contains? (reagent.session/get type) "sl"))}

                       {:label "Somali"
                        :iso "so"
                        :ratom (reaction (contains? (reagent.session/get type) "so"))}

                       {:label "Spanish"
                        :iso "es"
                        :ratom (reaction (contains? (reagent.session/get type) "es"))}

                       {:label "Sundanese"
                        :iso "su"
                        :ratom (reaction (contains? (reagent.session/get type) "su"))}

                       {:label "Swahili"
                        :iso "sw"
                        :ratom (reaction (contains? (reagent.session/get type) "sw"))}

                       {:label "Swedish"
                        :iso "sv"
                        :ratom (reaction (contains? (reagent.session/get type) "sv"))}

                       {:label "Tajik"
                        :iso "tg"
                        :ratom (reaction (contains? (reagent.session/get type) "tg"))}

                       {:label "Tamil"
                        :iso "ta"
                        :ratom (reaction (contains? (reagent.session/get type) "ta"))}

                       {:label "Tatar"
                        :iso "tt"
                        :ratom (reaction (contains? (reagent.session/get type) "tt"))}

                       {:label "Telugu"
                        :iso "te"
                        :ratom (reaction (contains? (reagent.session/get type) "te"))}

                       {:label "Thai"
                        :iso "th"
                        :ratom (reaction (contains? (reagent.session/get type) "th"))}

                       {:label "Turkish"
                        :iso "tr"
                        :ratom (reaction (contains? (reagent.session/get type) "tr"))}

                       {:label "Turkmen"
                        :iso "tk"
                        :ratom (reaction (contains? (reagent.session/get type) "tk"))}

                       {:label "Ukrainian"
                        :iso "uk"
                        :ratom (reaction (contains? (reagent.session/get type) "uk"))}

                       {:label "Urdu"
                        :iso "ur"
                        :ratom (reaction (contains? (reagent.session/get type) "ur"))}

                       {:label "Uyghur"
                        :iso "ug"
                        :ratom (reaction (contains? (reagent.session/get type) "ug"))}

                       {:label "Uzbek"
                        :iso "uz"
                        :ratom (reaction (contains? (reagent.session/get type) "uz"))}

                       {:label "Vietnamese"
                        :iso "vi"
                        :ratom (reaction (contains? (reagent.session/get type) "vi"))}

                       {:label "Welsh"
                        :iso "cy"
                        :ratom (reaction (contains? (reagent.session/get type) "cy"))}

                       {:label "Xhosa"
                        :iso "xh"
                        :ratom (reaction (contains? (reagent.session/get type) "xh"))}

                       {:label "Yiddish"
                        :iso "yi"
                        :ratom (reaction (contains? (reagent.session/get type) "yi"))}

                       {:label "Yoruba"
                        :iso "yo"
                        :ratom (reaction (contains? (reagent.session/get type) "yo"))}

                       {:label "Zulu"
                        :iso "zu"
                        :ratom (reaction (contains? (reagent.session/get type) "zu"))}]

        partitioned-iso-ratom (partition-all 4 all-iso-ratom)]

    [:table
     (into [:tbody]
           (for [row partitioned-iso-ratom]
             (into [:tr]
                   (for [{:keys [iso label ratom]} row]
                     [:td
                      [recom/checkbox
                       :model @ratom
                       :on-change (cb-handler-fn type iso)
                       :label [recom/label :label label :style {:margin-top "3px"}]]
                      ]
                     ))))]
    )
  )

(defn pane-1 []
  (let [auto-detect-ratom? (reagent/atom nil)
        display-next-ratom? (reaction (not (empty? (reagent.session/get :source))))]
    (fn []
      [recom/h-box
       :children [[recom/v-box
                   :width "700px"
                   :align :start
                   :children [[recom/p "We will bulk translate from one language to one or many languages. First, do you want to let Google translate auto detect your input language?"]
                              [recom/radio-button
                               :label "yes"
                               :model auto-detect-ratom?
                               :value true
                               :on-change (fn [v]
                                            (reset! auto-detect-ratom? v)
                                            (reagent.session/put! :source #{"auto"}))]
                              [recom/radio-button
                               :label "no"
                               :model auto-detect-ratom?
                               :value false
                               :on-change (fn [v]
                                            (reset! auto-detect-ratom? v)
                                            (reagent.session/put! :source #{}))]
                              (when (= @auto-detect-ratom? false)
                                [recom/v-box
                                 :children [[recom/p "Please select your input language"]
                                            [lang-option-pane :source]]])
                              ]]
                  (when @display-next-ratom?
                    [recom/box
                     :align :center
                     :child [recom/button
                             :label "Next"
                             :style {:height "500px"
                                     :background-color "#28a745"
                                     :color "white"}
                             :on-click (fn [_]
                                         (reagent.session/put! :curr-pane :step2)
                                         )]])
                  ]])))

(defn pane-2 []
  [recom/h-box
   :children [[recom/v-box
               :width "700px"
               :align :start
               :children [[recom/p "Please select your target languages"]
                          [lang-option-pane :target]]]

              [recom/box
               :align :center
               :child [recom/button
                       :label "Next"
                       :style {:height "500px"
                               :background-color "#28a745"
                               :color "white"}
                       :on-click (fn [_]
                                   (reagent.session/put! :curr-pane :step3)
                                   )]]
              ]
   ])

(defn pane-3 []
  [recom/v-box
   :width "700px"
   :align :center
   :children [[:div {:style {:display "none"}}
               [:input {:id "bulkCsvFileInput" :type "file"
                        :on-change (fn [e]
                                     (put! upload-chan e)
                                     )}]]
              [recom/h-box
               :align :start
               :style {:padding "10px"}
               :children [[recom/button
                           :label "Submit CSV File"
                           ;; :tooltip [recom/v-box
                           ;;           :children [[recom/label :label "Tooltip goes here"]]
                           ;;           ]
                           :style {:width "200px"
                                   :background-color "#007bff"
                                   :color "white"}
                           :on-click (fn [e] (-> "//input[@id='bulkCsvFileInput']" xpath single-node .click))]]]]
   ])

(defn current-page []
  (let [;;current-step-ratom (reagent/atom :step1)
        _ (reagent.session/put! :curr-pane :step1)
        ]
    (fn []
      (cond (= (reagent.session/get :curr-pane) :step1) [pane-1]
            (= (reagent.session/get :curr-pane) :step2) [pane-2]
            (= (reagent.session/get :curr-pane) :step3) [pane-3]
            ))
    ))


(defn mount-root []
  (reagent/render [current-page] (.getElementById js/document "app")))

; -- main entry point -------------------------------------------------------------------------------------------------------
(defn init! []
  (let [background-port (runtime/connect)]
    (go
      (log "POPUP: init")
      (connect-to-background-page! background-port)
      (prn "init reagent.session/state")

      ;; (reagent.session/reset! (<! (storage/get-ui-state)))
      (reagent.session/reset! {:target #{}
                               :source #{}})

      (storage/clear-words!)

      (add-watch reagent.session/state :target
                 (fn [key atom old-state new-state]
                   (prn "new-state: " new-state)
                   ;; (storage/set-ui-state new-state)
                   ))

      ;; handle onload
      (go-loop []
        (let [reader (js/FileReader.)
              file (<! upload-chan)]
          (set! (.-onload reader) #(put! read-chan %))
          (.readAsText reader file)
          (recur)))

      ;; handle reading of the file
      (go-loop []
        (let [file-content (<! read-chan)
              csv-data (->> (csv/read-csv (clojure.string/trim file-content))
                            ;; trim off random whitespaces
                            (map (fn [[word]]
                                   (clojure.string/trim word)
                                   )))]
          (post-message! background-port (common/marshall {:type :init-translations
                                                           :data csv-data
                                                           :source (first(reagent.session/get :source))
                                                           :target (reagent.session/get :target)
                                                           }))

          (recur)
          ))

      (mount-root)))
  )
