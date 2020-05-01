(ns bulk-google-translate.popup.core
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require [cljs.core.async :refer [<!]]
            [chromex.logging :refer-macros [log info warn error group group-end]]
            [chromex.protocols.chrome-port :refer [post-message!]]
            [re-com.core :as recom]
            [reagent.core :as reagent :refer [atom]]
            [chromex.ext.runtime :as runtime :refer-macros [connect]]))

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

(defn connect-to-background-page! []
  (let [background-port (runtime/connect)]
    (post-message! background-port "hello from POPUP!")
    (run-message-loop! background-port)))


(defn current-page []
  [recom/v-box
   :width "700px"
   :align :center
   :children [[recom/v-box
               :align :start
               :style {:padding "10px"}
               :children [[recom/title :label "Instructions:" :level :level1]
                          [recom/label :label "- more instructions go here"]]
               ]
              [:table
               [:tbody
                [:tr
                 [:td
                  [recom/checkbox
                   :model false
                   :on-change #()
                   :label [recom/label :label "Afrikaans" :style {:margin-top "3px"}]]]
                 [:td
                  [recom/checkbox
                   :model false
                   :on-change #()
                   :label [recom/label :label "Albanian" :style {:margin-top "3px"}]]]
                 [:td
                  [recom/checkbox
                   :model false
                   :on-change #()
                   :label [recom/label :label "Amharic" :style {:margin-top "3px"}]]]
                 [:td
                  [recom/checkbox
                   :model false
                   :on-change #()
                   :label [recom/label :label "Arabic" :style {:margin-top "3px"}]]]
                 ]
                [:tr
                 [:td
                  [recom/checkbox
                   :model false
                   :on-change #()
                   :label [recom/label :label "Armenian" :style {:margin-top "3px"}]]]
                 [:td
                  [recom/checkbox
                   :model false
                   :on-change #()
                   :label [recom/label :label "Azerbaijani" :style {:margin-top "3px"}]]]
                 [:td
                  [recom/checkbox
                   :model false
                   :on-change #()
                   :label [recom/label :label "Basque" :style {:margin-top "3px"}]]]
                 [:td
                  [recom/checkbox
                   :model false
                   :on-change #()
                   :label [recom/label :label "Belarusian" :style {:margin-top "3px"}]]]
                 ]
                [:tr
                 [:td
                  [recom/checkbox
                   :model false
                   :on-change #()
                   :label [recom/label :label "Bengali" :style {:margin-top "3px"}]]]
                 [:td
                  [recom/checkbox
                   :model false
                   :on-change #()
                   :label [recom/label :label "Bosnian" :style {:margin-top "3px"}]]]
                 [:td
                  [recom/checkbox
                   :model false
                   :on-change #()
                   :label [recom/label :label "Bulgarian" :style {:margin-top "3px"}]]]
                 [:td
                  [recom/checkbox
                   :model false
                   :on-change #()
                   :label [recom/label :label "Burmese" :style {:margin-top "3px"}]]]
                 ]
                [:tr
                 [:td
                  [recom/checkbox
                   :model false
                   :on-change #()
                   :label [recom/label :label "Catalan" :style {:margin-top "3px"}]]]
                 [:td
                  [recom/checkbox
                   :model false
                   :on-change #()
                   :label [recom/label :label "Cebuano" :style {:margin-top "3px"}]]]
                 [:td
                  [recom/checkbox
                   :model false
                   :on-change #()
                   :label [recom/label :label "Chewa" :style {:margin-top "3px"}]]]
                 [:td
                  [recom/checkbox
                   :model false
                   :on-change #()
                   :label [recom/label :label "Chinese (Simplified)" :style {:margin-top "3px"}]]]
                 ]
                [:tr
                 [:td
                  [recom/checkbox
                   :model false
                   :on-change #()
                   :label [recom/label :label "Chinese (Traditional)" :style {:margin-top "3px"}]]]
                 [:td
                  [recom/checkbox
                   :model false
                   :on-change #()
                   :label [recom/label :label "Corsican" :style {:margin-top "3px"}]]]
                 [:td
                  [recom/checkbox
                   :model false
                   :on-change #()
                   :label [recom/label :label "Croatian" :style {:margin-top "3px"}]]]
                 [:td
                  [recom/checkbox
                   :model false
                   :on-change #()
                   :label [recom/label :label "Czech" :style {:margin-top "3px"}]]]
                 ]
                [:tr
                 [:td
                  [recom/checkbox
                   :model false
                   :on-change #()
                   :label [recom/label :label "Danish" :style {:margin-top "3px"}]]]
                 [:td
                  [recom/checkbox
                   :model false
                   :on-change #()
                   :label [recom/label :label "Dutch" :style {:margin-top "3px"}]]]
                 [:td
                  [recom/checkbox
                   :model false
                   :on-change #()
                   :label [recom/label :label "English" :style {:margin-top "3px"}]]]
                 [:td
                  [recom/checkbox
                   :model false
                   :on-change #()
                   :label [recom/label :label "Esperanto" :style {:margin-top "3px"}]]]
                 ]
                [:tr
                 [:td
                  [recom/checkbox
                   :model false
                   :on-change #()
                   :label [recom/label :label "Estonian" :style {:margin-top "3px"}]]]

                 [:td
                  [recom/checkbox
                   :model false
                   :on-change #()
                   :label [recom/label :label "Filipino" :style {:margin-top "3px"}]]]
                 [:td
                  [recom/checkbox
                   :model false
                   :on-change #()
                   :label [recom/label :label "Finnish" :style {:margin-top "3px"}]]]
                 [:td
                  [recom/checkbox
                   :model false
                   :on-change #()
                   :label [recom/label :label "French" :style {:margin-top "3px"}]]]
                 ]
                [:tr
                 [:td
                  [recom/checkbox
                   :model false
                   :on-change #()
                   :label [recom/label :label "Galician" :style {:margin-top "3px"}]]]
                 [:td
                  [recom/checkbox
                   :model false
                   :on-change #()
                   :label [recom/label :label "Georgian" :style {:margin-top "3px"}]]]

                 [:td
                  [recom/checkbox
                   :model false
                   :on-change #()
                   :label [recom/label :label "German" :style {:margin-top "3px"}]]]
                 [:td
                  [recom/checkbox
                   :model false
                   :on-change #()
                   :label [recom/label :label "Greek" :style {:margin-top "3px"}]]]
                 ]
                [:tr
                 [:td
                  [recom/checkbox
                   :model false
                   :on-change #()
                   :label [recom/label :label "Gujarati" :style {:margin-top "3px"}]]]
                 [:td
                  [recom/checkbox
                   :model false
                   :on-change #()
                   :label [recom/label :label "Haitian Creole" :style {:margin-top "3px"}]]]
                 [:td
                  [recom/checkbox
                   :model false
                   :on-change #()
                   :label [recom/label :label "Hausa" :style {:margin-top "3px"}]]]

                 [:td
                  [recom/checkbox
                   :model false
                   :on-change #()
                   :label [recom/label :label "Hawaiian" :style {:margin-top "3px"}]]]
                 ]
                [:tr
                 [:td
                  [recom/checkbox
                   :model false
                   :on-change #()
                   :label [recom/label :label "Hebrew" :style {:margin-top "3px"}]]]
                 [:td
                  [recom/checkbox
                   :model false
                   :on-change #()
                   :label [recom/label :label "Hindi" :style {:margin-top "3px"}]]]
                 [:td
                  [recom/checkbox
                   :model false
                   :on-change #()
                   :label [recom/label :label "Hmong" :style {:margin-top "3px"}]]]
                 [:td
                  [recom/checkbox
                   :model false
                   :on-change #()
                   :label [recom/label :label "Hungarian" :style {:margin-top "3px"}]]]

                 ]
                [:tr
                 [:td
                  [recom/checkbox
                   :model false
                   :on-change #()
                   :label [recom/label :label "Icelandic" :style {:margin-top "3px"}]]]
                 [:td
                  [recom/checkbox
                   :model false
                   :on-change #()
                   :label [recom/label :label "Igbo" :style {:margin-top "3px"}]]]
                 [:td
                  [recom/checkbox
                   :model false
                   :on-change #()
                   :label [recom/label :label "Indonesian" :style {:margin-top "3px"}]]]
                 [:td
                  [recom/checkbox
                   :model false
                   :on-change #()
                   :label [recom/label :label "Irish" :style {:margin-top "3px"}]]]
                 ]
                [:tr
                 [:td
                  [recom/checkbox
                   :model false
                   :on-change #()
                   :label [recom/label :label "Italian" :style {:margin-top "3px"}]]]

                 [:td
                  [recom/checkbox
                   :model false
                   :on-change #()
                   :label [recom/label :label "Japanese" :style {:margin-top "3px"}]]]
                 [:td
                  [recom/checkbox
                   :model false
                   :on-change #()
                   :label [recom/label :label "Javanese" :style {:margin-top "3px"}]]]
                 [:td
                  [recom/checkbox
                   :model false
                   :on-change #()
                   :label [recom/label :label "Kannada" :style {:margin-top "3px"}]]]
                 ]
                [:tr
                 [:td
                  [recom/checkbox
                   :model false
                   :on-change #()
                   :label [recom/label :label "Kazakh" :style {:margin-top "3px"}]]]
                 [:td
                  [recom/checkbox
                   :model false
                   :on-change #()
                   :label [recom/label :label "Khmer" :style {:margin-top "3px"}]]]

                 [:td
                  [recom/checkbox
                   :model false
                   :on-change #()
                   :label [recom/label :label "Kinyarwanda" :style {:margin-top "3px"}]]]
                 [:td
                  [recom/checkbox
                   :model false
                   :on-change #()
                   :label [recom/label :label "Korean" :style {:margin-top "3px"}]]]
                 ]
                [:tr
                 [:td
                  [recom/checkbox
                   :model false
                   :on-change #()
                   :label [recom/label :label "Kurdish (Kurmanji)" :style {:margin-top "3px"}]]]
                 [:td
                  [recom/checkbox
                   :model false
                   :on-change #()
                   :label [recom/label :label "Kyrgyz" :style {:margin-top "3px"}]]]
                 [:td
                  [recom/checkbox
                   :model false
                   :on-change #()
                   :label [recom/label :label "Lao" :style {:margin-top "3px"}]]]

                 [:td
                  [recom/checkbox
                   :model false
                   :on-change #()
                   :label [recom/label :label "Latin" :style {:margin-top "3px"}]]]
                 ]
                [:tr
                 [:td
                  [recom/checkbox
                   :model false
                   :on-change #()
                   :label [recom/label :label "Latvian" :style {:margin-top "3px"}]]]
                 [:td
                  [recom/checkbox
                   :model false
                   :on-change #()
                   :label [recom/label :label "Lithuanian" :style {:margin-top "3px"}]]]
                 [:td
                  [recom/checkbox
                   :model false
                   :on-change #()
                   :label [recom/label :label "Luxembourgish" :style {:margin-top "3px"}]]]
                 [:td
                  [recom/checkbox
                   :model false
                   :on-change #()
                   :label [recom/label :label "Macedonian" :style {:margin-top "3px"}]]]

                 ]
                [:tr
                 [:td
                  [recom/checkbox
                   :model false
                   :on-change #()
                   :label [recom/label :label "Malagasy" :style {:margin-top "3px"}]]]
                 [:td
                  [recom/checkbox
                   :model false
                   :on-change #()
                   :label [recom/label :label "Malay" :style {:margin-top "3px"}]]]
                 [:td
                  [recom/checkbox
                   :model false
                   :on-change #()
                   :label [recom/label :label "Malayalam" :style {:margin-top "3px"}]]]
                 [:td
                  [recom/checkbox
                   :model false
                   :on-change #()
                   :label [recom/label :label "Maltese" :style {:margin-top "3px"}]]]
                 ]
                [:tr
                 [:td
                  [recom/checkbox
                   :model false
                   :on-change #()
                   :label [recom/label :label "Maori" :style {:margin-top "3px"}]]]

                 [:td
                  [recom/checkbox
                   :model false
                   :on-change #()
                   :label [recom/label :label "Marathi" :style {:margin-top "3px"}]]]
                 [:td
                  [recom/checkbox
                   :model false
                   :on-change #()
                   :label [recom/label :label "Mongolian" :style {:margin-top "3px"}]]]
                 [:td
                  [recom/checkbox
                   :model false
                   :on-change #()
                   :label [recom/label :label "Nepali" :style {:margin-top "3px"}]]]
                 ]
                [:tr
                 [:td
                  [recom/checkbox
                   :model false
                   :on-change #()
                   :label [recom/label :label "Norwegian (Bokmål)" :style {:margin-top "3px"}]]]
                 [:td
                  [recom/checkbox
                   :model false
                   :on-change #()
                   :label [recom/label :label "Odia" :style {:margin-top "3px"}]]]

                 [:td
                  [recom/checkbox
                   :model false
                   :on-change #()
                   :label [recom/label :label "Pashto" :style {:margin-top "3px"}]]]
                 [:td
                  [recom/checkbox
                   :model false
                   :on-change #()
                   :label [recom/label :label "Persian" :style {:margin-top "3px"}]]]
                 ]
                [:tr
                 [:td
                  [recom/checkbox
                   :model false
                   :on-change #()
                   :label [recom/label :label "Polish" :style {:margin-top "3px"}]]]
                 [:td
                  [recom/checkbox
                   :model false
                   :on-change #()
                   :label [recom/label :label "Portuguese" :style {:margin-top "3px"}]]]
                 [:td
                  [recom/checkbox
                   :model false
                   :on-change #()
                   :label [recom/label :label "Punjabi (Gurmukhi)" :style {:margin-top "3px"}]]]

                 [:td
                  [recom/checkbox
                   :model false
                   :on-change #()
                   :label [recom/label :label "Romanian" :style {:margin-top "3px"}]]]
                 ]
                [:tr
                 [:td
                  [recom/checkbox
                   :model false
                   :on-change #()
                   :label [recom/label :label "Russian" :style {:margin-top "3px"}]]]
                 [:td
                  [recom/checkbox
                   :model false
                   :on-change #()
                   :label [recom/label :label "Samoan" :style {:margin-top "3px"}]]]
                 [:td
                  [recom/checkbox
                   :model false
                   :on-change #()
                   :label [recom/label :label "Scots Gaelic" :style {:margin-top "3px"}]]]
                 [:td
                  [recom/checkbox
                   :model false
                   :on-change #()
                   :label [recom/label :label "Serbian" :style {:margin-top "3px"}]]]


                 ]
                [:tr
                 [:td
                  [recom/checkbox
                   :model false
                   :on-change #()
                   :label [recom/label :label "Sesotho" :style {:margin-top "3px"}]]]
                 [:td
                  [recom/checkbox
                   :model false
                   :on-change #()
                   :label [recom/label :label "Shona" :style {:margin-top "3px"}]]]
                 [:td
                  [recom/checkbox
                   :model false
                   :on-change #()
                   :label [recom/label :label "Sindhi" :style {:margin-top "3px"}]]]
                 [:td
                  [recom/checkbox
                   :model false
                   :on-change #()
                   :label [recom/label :label "Sinhala" :style {:margin-top "3px"}]]]
                 ]
                [:tr
                 [:td
                  [recom/checkbox
                   :model false
                   :on-change #()
                   :label [recom/label :label "Slovak" :style {:margin-top "3px"}]]]

                 [:td
                  [recom/checkbox
                   :model false
                   :on-change #()
                   :label [recom/label :label "Slovenian" :style {:margin-top "3px"}]]]
                 [:td
                  [recom/checkbox
                   :model false
                   :on-change #()
                   :label [recom/label :label "Somali" :style {:margin-top "3px"}]]]
                 [:td
                  [recom/checkbox
                   :model false
                   :on-change #()
                   :label [recom/label :label "Spanish" :style {:margin-top "3px"}]]]
                 ]
                [:tr
                 [:td
                  [recom/checkbox
                   :model false
                   :on-change #()
                   :label [recom/label :label "Sundanese" :style {:margin-top "3px"}]]]
                 [:td
                  [recom/checkbox
                   :model false
                   :on-change #()
                   :label [recom/label :label "Swahili" :style {:margin-top "3px"}]]]

                 [:td
                  [recom/checkbox
                   :model false
                   :on-change #()
                   :label [recom/label :label "Swedish" :style {:margin-top "3px"}]]]
                 [:td
                  [recom/checkbox
                   :model false
                   :on-change #()
                   :label [recom/label :label "Tajik" :style {:margin-top "3px"}]]]

                 ]

                [:tr
                 [:td
                  [recom/checkbox
                   :model false
                   :on-change #()
                   :label [recom/label :label "Tamil" :style {:margin-top "3px"}]]]
                 [:td
                  [recom/checkbox
                   :model false
                   :on-change #()
                   :label [recom/label :label "Tatar" :style {:margin-top "3px"}]]]
                 [:td
                  [recom/checkbox
                   :model false
                   :on-change #()
                   :label [recom/label :label "Telugu" :style {:margin-top "3px"}]]]

                 [:td
                  [recom/checkbox
                   :model false
                   :on-change #()
                   :label [recom/label :label "Thai" :style {:margin-top "3px"}]]]

                 ]
                [:tr
                 [:td
                  [recom/checkbox
                   :model false
                   :on-change #()
                   :label [recom/label :label "Turkish" :style {:margin-top "3px"}]]]
                 [:td
                  [recom/checkbox
                   :model false
                   :on-change #()
                   :label [recom/label :label "Turkmen" :style {:margin-top "3px"}]]]
                 [:td
                  [recom/checkbox
                   :model false
                   :on-change #()
                   :label [recom/label :label "Ukrainian" :style {:margin-top "3px"}]]]
                 [:td
                  [recom/checkbox
                   :model false
                   :on-change #()
                   :label [recom/label :label "Urdu" :style {:margin-top "3px"}]]]


                 ]
                [:tr
                 [:td
                  [recom/checkbox
                   :model false
                   :on-change #()
                   :label [recom/label :label "Uyghur" :style {:margin-top "3px"}]]]
                 [:td
                  [recom/checkbox
                   :model false
                   :on-change #()
                   :label [recom/label :label "Uzbek" :style {:margin-top "3px"}]]]
                 [:td
                  [recom/checkbox
                   :model false
                   :on-change #()
                   :label [recom/label :label "Vietnamese" :style {:margin-top "3px"}]]]
                 [:td
                  [recom/checkbox
                   :model false
                   :on-change #()
                   :label [recom/label :label "Welsh" :style {:margin-top "3px"}]]]

                 ]
                [:tr
                 [:td
                  [recom/checkbox
                   :model false
                   :on-change #()
                   :label [recom/label :label "West Frisian" :style {:margin-top "3px"}]]]

                 [:td
                  [recom/checkbox
                   :model false
                   :on-change #()
                   :label [recom/label :label "Xhosa" :style {:margin-top "3px"}]]]
                 [:td
                  [recom/checkbox
                   :model false
                   :on-change #()
                   :label [recom/label :label "Yiddish" :style {:margin-top "3px"}]]]
                 [:td
                  [recom/checkbox
                   :model false
                   :on-change #()
                   :label [recom/label :label "Yoruba" :style {:margin-top "3px"}]]]
                 ]
                [:tr
                 [:td
                  [recom/checkbox
                   :model false
                   :on-change #()
                   :label [recom/label :label "Zulu" :style {:margin-top "3px"}]]]]
                ]
               ]
              ]
   ])


(defn mount-root []
  (reagent/render [current-page] (.getElementById js/document "app")))

; -- main entry point -------------------------------------------------------------------------------------------------------

(defn init! []
  (log "POPUP: init")
  (connect-to-background-page!)
  (mount-root)
  )
