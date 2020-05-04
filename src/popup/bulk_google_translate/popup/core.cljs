(ns bulk-google-translate.popup.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [reagent.ratom :refer [reaction]])
  (:require [cljs.core.async :refer [<!]]
            [reagent.core :as reagent :refer [atom]]
            [chromex.logging :refer-macros [log info warn error group group-end]]
            [chromex.protocols.chrome-port :refer [post-message!]]
            [re-com.core :as recom]
            [reagent.core :as reagent :refer [atom]]
            [reagent.session]
            [bulk-google-translate.background.storage :as storage]
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


(defn cb-handler-fn [iso]
  (fn [checked?]
    (if checked?
      (reagent.session/update! :target conj iso)
      (reagent.session/update! :target disj iso))))

(defn current-page []
  (let [
        ;; Afrikaans	af
        af-ratom (reaction (contains? (reagent.session/get :target) "af"))
        ;; Albanian	sq
        sq-ratom (reaction (contains? (reagent.session/get :target) "sq"))
        ;; Amharic	am
        am-ratom (reaction (contains? (reagent.session/get :target) "am"))
        ;; Arabic	ar
        ar-ratom (reaction (contains? (reagent.session/get :target) "ar"))
        ;; Armenian	hy
        hy-ratom (reaction (contains? (reagent.session/get :target) "hy"))
        ;; Azerbaijani	az
        az-ratom (reaction (contains? (reagent.session/get :target) "az"))

        ;; Basque	eu
        eu-ratom (reaction (contains? (reagent.session/get :target) "eu"))
        ;; Belarusian	be
        be-ratom (reaction (contains? (reagent.session/get :target) "be"))
        ;; Bengali	bn
        bn-ratom (reaction (contains? (reagent.session/get :target) "bn"))
        ;; Bosnian	bs
        bs-ratom (reaction (contains? (reagent.session/get :target) "bs"))
        ;; Bulgarian	bg
        bg-ratom (reaction (contains? (reagent.session/get :target) "bg"))

        ;; Catalan	ca
        ca-ratom (reaction (contains? (reagent.session/get :target) "ca"))
        ;; Cebuano	ceb
        eb-ratom (reaction (contains? (reagent.session/get :target) "eb"))
        ;; Chichewa ny
        ny-ratom (reaction (contains? (reagent.session/get :target) "ny"))
        ;; Chinese (Simplified)	zh-CN
        zh-CN-ratom (reaction (contains? (reagent.session/get :target) "zh-CN"))
        ;; Chinese (Traditional)	zh-TW
        zh-TW-ratom (reaction (contains? (reagent.session/get :target) "zh-TW"))
        ;; Corsican	co
        co-ratom (reaction (contains? (reagent.session/get :target) "co"))
        ;; Croatian	hr
        hr-ratom (reaction (contains? (reagent.session/get :target) "hr"))
        ;; Czech	cs
        cs-ratom (reaction (contains? (reagent.session/get :target) "cs"))

        ;; https://cloud.google.com/translate/docs/languages
        ;; Danish	da
        da-ratom (reaction (contains? (reagent.session/get :target) "da"))
        ;; Dutch	nl
        nl-ratom (reaction (contains? (reagent.session/get :target) "nl"))
        ;; English	en
        en-ratom (reaction (contains? (reagent.session/get :target) "en"))
        ;; Esperanto	eo
        eo-ratom (reaction (contains? (reagent.session/get :target) "eo"))
        ;; Estonian	et
        et-ratom (reaction (contains? (reagent.session/get :target) "et"))

        ;; Filipino tl
        tl-ratom (reaction (contains? (reagent.session/get :target) "tl"))
        ;; Finnish	fi
        fi-ratom (reaction (contains? (reagent.session/get :target) "fi"))
        ;; French	fr
        fr-ratom (reaction (contains? (reagent.session/get :target) "fr"))
        ;; Frisian	fy
        fy-ratom (reaction (contains? (reagent.session/get :target) "fy"))

        ;; Galician	gl
        gl-ratom (reaction (contains? (reagent.session/get :target) "gl"))
        ;; Georgian	ka
        ka-ratom (reaction (contains? (reagent.session/get :target) "ka"))
        ;; German	de
        de-ratom (reaction (contains? (reagent.session/get :target) "de"))
        ;; Greek	el
        el-ratom (reaction (contains? (reagent.session/get :target) "el"))
        ;; Gujarati	gu
        gu-ratom (reaction (contains? (reagent.session/get :target) "gu"))

        ;; Haitian Creole	ht
        ht-ratom (reaction (contains? (reagent.session/get :target) "ht"))
        ;; Hausa	ha
        ha-ratom (reaction (contains? (reagent.session/get :target) "ha"))
        ;; Hawaiian	haw
        haw-ratom (reaction (contains? (reagent.session/get :target) "haw"))
        ;; Hebrew	he or iw
        iw-ratom (reaction (contains? (reagent.session/get :target) "iw"))
        ;; Hindi	hi
        hi-ratom (reaction (contains? (reagent.session/get :target) "hi"))
        ;; Hmong	hmn
        hmn-ratom (reaction (contains? (reagent.session/get :target) "hmn"))
        ;; Hungarian	hu
        hu-ratom (reaction (contains? (reagent.session/get :target) "hu"))
        ;; Icelandic	is
        is-ratom (reaction (contains? (reagent.session/get :target) "is"))
        ;; Igbo	ig
        ig-ratom (reaction (contains? (reagent.session/get :target) "ig"))
        ;; Indonesian	id
        id-ratom (reaction (contains? (reagent.session/get :target) "id"))
        ;; Irish	ga
        ga-ratom (reaction (contains? (reagent.session/get :target) "ga"))
        ;; Italian	it
        it-ratom (reaction (contains? (reagent.session/get :target) "it"))

        ;; Japanese	ja
        ja-ratom (reaction (contains? (reagent.session/get :target) "ja"))
        ;; Javanese	jv
        jv-ratom (reaction (contains? (reagent.session/get :target) "jv"))
        ;; Kannada	kn
        kn-ratom (reaction (contains? (reagent.session/get :target) "kn"))
        ;; Kazakh	kk
        kk-ratom (reaction (contains? (reagent.session/get :target) "kk"))
        ;; Khmer	km
        km-ratom (reaction (contains? (reagent.session/get :target) "km"))

        ;; Kinyarwanda rw
        rw-ratom (reaction (contains? (reagent.session/get :target) "rw"))
        ;; Korean	ko
        ko-ratom (reaction (contains? (reagent.session/get :target) "ko"))
        ;; Kurdish	ku
        ku-ratom (reaction (contains? (reagent.session/get :target) "ku"))
        ;; Kyrgyz	ky
        ky-ratom (reaction (contains? (reagent.session/get :target) "ky"))

        ;; Lao	lo
        lo-ratom (reaction (contains? (reagent.session/get :target) "lo"))
        ;; Latin	la
        la-ratom (reaction (contains? (reagent.session/get :target) "la"))
        ;; Latvian	lv
        lv-ratom (reaction (contains? (reagent.session/get :target) "lv"))
        ;; Lithuanian	lt
        lt-ratom (reaction (contains? (reagent.session/get :target) "lt"))
        ;; Luxembourgish	lb
        lb-ratom (reaction (contains? (reagent.session/get :target) "lb"))
        ]

    [recom/v-box
     :width "700px"
     :align :center
     :children [[recom/h-box
                 :align :start
                 :style {:padding "10px"}
                 :children [
                            [recom/button
                             :label "Submit CSV File"
                             :tooltip [recom/v-box
                                       :children [[recom/label :label "Tooltip goes here"]]
                                       ]
                             :style {:width "200px"
                                     :background-color "#007bff"
                                     :color "white"}
                             :on-click (fn [e]
                                         (go (<! (storage/get-ui-state)))
                                         )]

                            ]
                 ]
                [:table
                 [:tbody
                  [:tr
                   [:td
                    [recom/checkbox
                     :model @af-ratom
                     :on-change (cb-handler-fn "af")
                     :label [recom/label :label "Afrikaans" :style {:margin-top "3px"}]]]
                   [:td
                    [recom/checkbox
                     :model @sq-ratom
                     :on-change (cb-handler-fn "sq")
                     :label [recom/label :label "Albanian" :style {:margin-top "3px"}]]]
                   [:td
                    [recom/checkbox
                     :model @am-ratom
                     :on-change (cb-handler-fn "am")
                     :label [recom/label :label "Amharic" :style {:margin-top "3px"}]]]
                   [:td
                    [recom/checkbox
                     :model @ar-ratom
                     :on-change (cb-handler-fn "ar")
                     :label [recom/label :label "Arabic" :style {:margin-top "3px"}]]]
                   ]
                  [:tr
                   [:td
                    [recom/checkbox
                     :model @hy-ratom
                     :on-change (cb-handler-fn "hy")
                     :label [recom/label :label "Armenian" :style {:margin-top "3px"}]]]
                   [:td
                    [recom/checkbox
                     :model @az-ratom
                     :on-change (cb-handler-fn "az")
                     :label [recom/label :label "Azerbaijani" :style {:margin-top "3px"}]]]
                   [:td
                    [recom/checkbox
                     :model @eu-ratom
                     :on-change (cb-handler-fn "eu")
                     :label [recom/label :label "Basque" :style {:margin-top "3px"}]]]
                   [:td
                    [recom/checkbox
                     :model @be-ratom
                     :on-change (cb-handler-fn "be")
                     :label [recom/label :label "Belarusian" :style {:margin-top "3px"}]]]
                   ]
                  [:tr
                   [:td
                    [recom/checkbox
                     :model @bn-ratom
                     :on-change (cb-handler-fn "bn")
                     :label [recom/label :label "Bengali" :style {:margin-top "3px"}]]]
                   [:td
                    [recom/checkbox
                     :model @bs-ratom
                     :on-change (cb-handler-fn "bs")
                     :label [recom/label :label "Bosnian" :style {:margin-top "3px"}]]]
                   [:td
                    [recom/checkbox
                     :model @bg-ratom
                     :on-change (cb-handler-fn "bg")
                     :label [recom/label :label "Bulgarian" :style {:margin-top "3px"}]]]
                   ]
                  [:tr
                   [:td
                    [recom/checkbox
                     :model @ca-ratom
                     :on-change (cb-handler-fn "ca")
                     :label [recom/label :label "Catalan" :style {:margin-top "3px"}]]]
                   [:td
                    [recom/checkbox
                     :model @eb-ratom
                     :on-change (cb-handler-fn "eb")
                     :label [recom/label :label "Cebuano" :style {:margin-top "3px"}]]]
                   [:td
                    [recom/checkbox
                     :model @ny-ratom
                     :on-change (cb-handler-fn "ny")
                     :label [recom/label :label "Chichewa" :style {:margin-top "3px"}]]]
                   [:td
                    [recom/checkbox
                     :model @zh-CN-ratom
                     :on-change (cb-handler-fn "zh-CN")
                     :label [recom/label :label "Chinese (Simplified)" :style {:margin-top "3px"}]]]
                   ]
                  [:tr
                   [:td
                    [recom/checkbox
                     :model @zh-TW-ratom
                     :on-change (cb-handler-fn "zh-TW")
                     :label [recom/label :label "Chinese (Traditional)" :style {:margin-top "3px"}]]]
                   [:td
                    [recom/checkbox
                     :model @co-ratom
                     :on-change (cb-handler-fn "co")
                     :label [recom/label :label "Corsican" :style {:margin-top "3px"}]]]
                   [:td
                    [recom/checkbox
                     :model @hr-ratom
                     :on-change (cb-handler-fn "hr")
                     :label [recom/label :label "Croatian" :style {:margin-top "3px"}]]]
                   [:td
                    [recom/checkbox
                     :model @cs-ratom
                     :on-change (cb-handler-fn "cs")
                     :label [recom/label :label "Czech" :style {:margin-top "3px"}]]]
                   ]
                  [:tr
                   [:td
                    [recom/checkbox
                     :model @da-ratom
                     :on-change (cb-handler-fn "da")
                     :label [recom/label :label "Danish" :style {:margin-top "3px"}]]]
                   [:td
                    [recom/checkbox
                     :model @nl-ratom
                     :on-change (cb-handler-fn "nl")
                     :label [recom/label :label "Dutch" :style {:margin-top "3px"}]]]
                   [:td
                    [recom/checkbox
                     :model @en-ratom
                     :on-change (cb-handler-fn "en")
                     :label [recom/label :label "English" :style {:margin-top "3px"}]]]
                   [:td
                    [recom/checkbox
                     :model @eo-ratom
                     :on-change (cb-handler-fn "eo")
                     :label [recom/label :label "Esperanto" :style {:margin-top "3px"}]]]
                   ]
                  [:tr
                   [:td
                    [recom/checkbox
                     :model @et-ratom
                     :on-change (cb-handler-fn "et")
                     :label [recom/label :label "Estonian" :style {:margin-top "3px"}]]]

                   [:td
                    [recom/checkbox
                     :model @tl-ratom
                     :on-change (cb-handler-fn "tl")
                     :label [recom/label :label "Filipino" :style {:margin-top "3px"}]]]
                   [:td
                    [recom/checkbox
                     :model @fi-ratom
                     :on-change (cb-handler-fn "fi")
                     :label [recom/label :label "Finnish" :style {:margin-top "3px"}]]]
                   [:td
                    [recom/checkbox
                     :model @fr-ratom
                     :on-change (cb-handler-fn "fr")
                     :label [recom/label :label "French" :style {:margin-top "3px"}]]]
                   [:td
                    [recom/checkbox
                     :model @fy-ratom
                     :on-change (cb-handler-fn "fy")
                     :label [recom/label :label "Frisian" :style {:margin-top "3px"}]]]
                   ]
                  [:tr
                   [:td
                    [recom/checkbox
                     :model @gl-ratom
                     :on-change (cb-handler-fn "gl")
                     :label [recom/label :label "Galician" :style {:margin-top "3px"}]]]
                   [:td
                    [recom/checkbox
                     :model @ka-ratom
                     :on-change (cb-handler-fn "ka")
                     :label [recom/label :label "Georgian" :style {:margin-top "3px"}]]]

                   [:td
                    [recom/checkbox
                     :model @de-ratom
                     :on-change (cb-handler-fn "de")
                     :label [recom/label :label "German" :style {:margin-top "3px"}]]]
                   [:td
                    [recom/checkbox
                     :model @el-ratom
                     :on-change (cb-handler-fn "el")
                     :label [recom/label :label "Greek" :style {:margin-top "3px"}]]]
                   ]
                  [:tr
                   [:td
                    [recom/checkbox
                     :model @gu-ratom
                     :on-change (cb-handler-fn "gu")
                     :label [recom/label :label "Gujarati" :style {:margin-top "3px"}]]]
                   [:td
                    [recom/checkbox
                     :model @ht-ratom
                     :on-change (cb-handler-fn "ht")
                     :label [recom/label :label "Haitian Creole" :style {:margin-top "3px"}]]]
                   [:td
                    [recom/checkbox
                     :model @ha-ratom
                     :on-change (cb-handler-fn "ha")
                     :label [recom/label :label "Hausa" :style {:margin-top "3px"}]]]

                   [:td
                    [recom/checkbox
                     :model @haw-ratom
                     :on-change (cb-handler-fn "haw")
                     :label [recom/label :label "Hawaiian" :style {:margin-top "3px"}]]]
                   ]
                  [:tr
                   [:td
                    [recom/checkbox
                     :model @iw-ratom
                     :on-change (cb-handler-fn "iw")
                     :label [recom/label :label "Hebrew" :style {:margin-top "3px"}]]]
                   [:td
                    [recom/checkbox
                     :model @hi-ratom
                     :on-change (cb-handler-fn "hi")
                     :label [recom/label :label "Hindi" :style {:margin-top "3px"}]]]
                   [:td
                    [recom/checkbox
                     :model @hmn-ratom
                     :on-change (cb-handler-fn "hmn")
                     :label [recom/label :label "Hmong" :style {:margin-top "3px"}]]]
                   [:td
                    [recom/checkbox
                     :model @hu-ratom
                     :on-change (cb-handler-fn "hu")
                     :label [recom/label :label "Hungarian" :style {:margin-top "3px"}]]]

                   ]
                  [:tr
                   [:td
                    [recom/checkbox
                     :model @is-ratom
                     :on-change (cb-handler-fn "is")
                     :label [recom/label :label "Icelandic" :style {:margin-top "3px"}]]]
                   [:td
                    [recom/checkbox
                     :model @ig-ratom
                     :on-change (cb-handler-fn "ig")
                     :label [recom/label :label "Igbo" :style {:margin-top "3px"}]]]
                   [:td
                    [recom/checkbox
                     :model @id-ratom
                     :on-change (cb-handler-fn "id")
                     :label [recom/label :label "Indonesian" :style {:margin-top "3px"}]]]
                   [:td
                    [recom/checkbox
                     :model @ga-ratom
                     :on-change (cb-handler-fn "ga")
                     :label [recom/label :label "Irish" :style {:margin-top "3px"}]]]
                   ]
                  [:tr
                   [:td
                    [recom/checkbox
                     :model @it-ratom
                     :on-change (cb-handler-fn "it")
                     :label [recom/label :label "Italian" :style {:margin-top "3px"}]]]

                   [:td
                    [recom/checkbox
                     :model @ja-ratom
                     :on-change (cb-handler-fn "ja")
                     :label [recom/label :label "Japanese" :style {:margin-top "3px"}]]]
                   [:td
                    [recom/checkbox
                     :model @jv-ratom
                     :on-change (cb-handler-fn "jv")
                     :label [recom/label :label "Javanese" :style {:margin-top "3px"}]]]
                   [:td
                    [recom/checkbox
                     :model @kn-ratom
                     :on-change (cb-handler-fn "kn")
                     :label [recom/label :label "Kannada" :style {:margin-top "3px"}]]]
                   ]
                  [:tr
                   [:td
                    [recom/checkbox
                     :model @kk-ratom
                     :on-change (cb-handler-fn "kk")
                     :label [recom/label :label "Kazakh" :style {:margin-top "3px"}]]]
                   [:td
                    [recom/checkbox
                     :model @km-ratom
                     :on-change (cb-handler-fn "km")
                     :label [recom/label :label "Khmer" :style {:margin-top "3px"}]]]

                   [:td
                    [recom/checkbox
                     :model @rw-ratom
                     :on-change (cb-handler-fn "rw")
                     :label [recom/label :label "Kinyarwanda" :style {:margin-top "3px"}]]]
                   [:td
                    [recom/checkbox
                     :model @ko-ratom
                     :on-change (cb-handler-fn "ko")
                     :label [recom/label :label "Korean" :style {:margin-top "3px"}]]]
                   ]
                  [:tr
                   [:td
                    [recom/checkbox
                     :model @ku-ratom
                     :on-change (cb-handler-fn "ku")
                     :label [recom/label :label "Kurdish (Kurmanji)" :style {:margin-top "3px"}]]]
                   [:td
                    [recom/checkbox
                     :model @ky-ratom
                     :on-change (cb-handler-fn "ky")
                     :label [recom/label :label "Kyrgyz" :style {:margin-top "3px"}]]]
                   [:td
                    [recom/checkbox
                     :model lo-ratom
                     :on-change (cb-handler-fn "lo")
                     :label [recom/label :label "Lao" :style {:margin-top "3px"}]]]

                   [:td
                    [recom/checkbox
                     :model la-ratom
                     :on-change (cb-handler-fn "la")
                     :label [recom/label :label "Latin" :style {:margin-top "3px"}]]]
                   ]
                  [:tr
                   [:td
                    [recom/checkbox
                     :model lv-ratom
                     :on-change (cb-handler-fn "lv")
                     :label [recom/label :label "Latvian" :style {:margin-top "3px"}]]]
                   [:td
                    [recom/checkbox
                     :model lt-ratom
                     :on-change (cb-handler-fn "lt")
                     :label [recom/label :label "Lithuanian" :style {:margin-top "3px"}]]]
                   [:td
                    [recom/checkbox
                     :model lb-ratom
                     :on-change (cb-handler-fn "lb")
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
     ]))


(defn mount-root []
  (reagent/render [current-page] (.getElementById js/document "app")))

; -- main entry point -------------------------------------------------------------------------------------------------------

(defn init! []
  (go
    (log "POPUP: init")
    (connect-to-background-page!)
    (prn "init reagent.session/state")

    (reagent.session/reset! (<! (storage/get-ui-state)))
    (prn ">> initial reagent.sesssion/state: " @reagent.session/state) ;;xxx

    (add-watch reagent.session/state :target
               (fn [key atom old-state new-state]
                 (prn "new-state: " new-state)
                 (storage/set-ui-state new-state)
                 ))

    (mount-root))
  )
