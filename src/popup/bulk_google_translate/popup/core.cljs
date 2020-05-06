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

(defn connect-to-background-page! []
  (let [background-port (runtime/connect)]
    (post-message! background-port "hello from POPUP!")
    (run-message-loop! background-port)))

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
  (let [;;cb-handler-fn (cb-handler-fn-fn type)
        ;; Afrikaans	af
        af-ratom (reaction (contains? (reagent.session/get type) "af"))
        ;; Albanian	sq
        sq-ratom (reaction (contains? (reagent.session/get type) "sq"))
        ;; Amharic	am
        am-ratom (reaction (contains? (reagent.session/get type) "am"))
        ;; Arabic	ar
        ar-ratom (reaction (contains? (reagent.session/get type) "ar"))
        ;; Armenian	hy
        hy-ratom (reaction (contains? (reagent.session/get type) "hy"))
        ;; Azerbaijani	az
        az-ratom (reaction (contains? (reagent.session/get type) "az"))

        ;; Basque	eu
        eu-ratom (reaction (contains? (reagent.session/get type) "eu"))
        ;; Belarusian	be
        be-ratom (reaction (contains? (reagent.session/get type) "be"))
        ;; Bengali	bn
        bn-ratom (reaction (contains? (reagent.session/get type) "bn"))
        ;; Bosnian	bs
        bs-ratom (reaction (contains? (reagent.session/get type) "bs"))
        ;; Bulgarian	bg
        bg-ratom (reaction (contains? (reagent.session/get type) "bg"))

        ;; Catalan	ca
        ca-ratom (reaction (contains? (reagent.session/get type) "ca"))
        ;; Cebuano	ceb
        eb-ratom (reaction (contains? (reagent.session/get type) "eb"))
        ;; Chichewa ny
        ny-ratom (reaction (contains? (reagent.session/get type) "ny"))
        ;; Chinese (Simplified)	zh-CN
        zh-CN-ratom (reaction (contains? (reagent.session/get type) "zh-CN"))
        ;; Chinese (Traditional)	zh-TW
        zh-TW-ratom (reaction (contains? (reagent.session/get type) "zh-TW"))
        ;; Corsican	co
        co-ratom (reaction (contains? (reagent.session/get type) "co"))
        ;; Croatian	hr
        hr-ratom (reaction (contains? (reagent.session/get type) "hr"))
        ;; Czech	cs
        cs-ratom (reaction (contains? (reagent.session/get type) "cs"))

        ;; Danish	da
        da-ratom (reaction (contains? (reagent.session/get type) "da"))
        ;; Dutch	nl
        nl-ratom (reaction (contains? (reagent.session/get type) "nl"))
        ;; English	en
        en-ratom (reaction (contains? (reagent.session/get type) "en"))
        ;; Esperanto	eo
        eo-ratom (reaction (contains? (reagent.session/get type) "eo"))
        ;; Estonian	et
        et-ratom (reaction (contains? (reagent.session/get type) "et"))

        ;; Filipino tl
        tl-ratom (reaction (contains? (reagent.session/get type) "tl"))
        ;; Finnish	fi
        fi-ratom (reaction (contains? (reagent.session/get type) "fi"))
        ;; French	fr
        fr-ratom (reaction (contains? (reagent.session/get type) "fr"))
        ;; Frisian	fy
        fy-ratom (reaction (contains? (reagent.session/get type) "fy"))

        ;; Galician	gl
        gl-ratom (reaction (contains? (reagent.session/get type) "gl"))
        ;; Georgian	ka
        ka-ratom (reaction (contains? (reagent.session/get type) "ka"))
        ;; German	de
        de-ratom (reaction (contains? (reagent.session/get type) "de"))
        ;; Greek	el
        el-ratom (reaction (contains? (reagent.session/get type) "el"))
        ;; Gujarati	gu
        gu-ratom (reaction (contains? (reagent.session/get type) "gu"))

        ;; Haitian Creole	ht
        ht-ratom (reaction (contains? (reagent.session/get type) "ht"))
        ;; Hausa	ha
        ha-ratom (reaction (contains? (reagent.session/get type) "ha"))
        ;; Hawaiian	haw
        haw-ratom (reaction (contains? (reagent.session/get type) "haw"))
        ;; Hebrew	he or iw
        iw-ratom (reaction (contains? (reagent.session/get type) "iw"))
        ;; Hindi	hi
        hi-ratom (reaction (contains? (reagent.session/get type) "hi"))
        ;; Hmong	hmn
        hmn-ratom (reaction (contains? (reagent.session/get type) "hmn"))
        ;; Hungarian	hu
        hu-ratom (reaction (contains? (reagent.session/get type) "hu"))
        ;; Icelandic	is
        is-ratom (reaction (contains? (reagent.session/get type) "is"))
        ;; Igbo	ig
        ig-ratom (reaction (contains? (reagent.session/get type) "ig"))
        ;; Indonesian	id
        id-ratom (reaction (contains? (reagent.session/get type) "id"))
        ;; Irish	ga
        ga-ratom (reaction (contains? (reagent.session/get type) "ga"))
        ;; Italian	it
        it-ratom (reaction (contains? (reagent.session/get type) "it"))

        ;; Japanese	ja
        ja-ratom (reaction (contains? (reagent.session/get type) "ja"))
        ;; Javanese	jv
        jv-ratom (reaction (contains? (reagent.session/get type) "jv"))
        ;; Kannada	kn
        kn-ratom (reaction (contains? (reagent.session/get type) "kn"))
        ;; Kazakh	kk
        kk-ratom (reaction (contains? (reagent.session/get type) "kk"))
        ;; Khmer	km
        km-ratom (reaction (contains? (reagent.session/get type) "km"))

        ;; Kinyarwanda rw
        rw-ratom (reaction (contains? (reagent.session/get type) "rw"))
        ;; Korean	ko
        ko-ratom (reaction (contains? (reagent.session/get type) "ko")) ;; Kurdish	ku
        ku-ratom (reaction (contains? (reagent.session/get type) "ku"))
        ;; Kyrgyz	ky
        ky-ratom (reaction (contains? (reagent.session/get type) "ky"))

        ;; Lao	lo
        lo-ratom (reaction (contains? (reagent.session/get type) "lo"))
        ;; Latin	la
        la-ratom (reaction (contains? (reagent.session/get type) "la"))
        ;; Latvian	lv
        lv-ratom (reaction (contains? (reagent.session/get type) "lv"))
        ;; Lithuanian	lt
        lt-ratom (reaction (contains? (reagent.session/get type) "lt"))
        ;; Luxembourgish	lb
        lb-ratom (reaction (contains? (reagent.session/get type) "lb"))

        ;; https://cloud.google.com/translate/docs/languages

        ;; Macedonian	mk
        mk-ratom (reaction (contains? (reagent.session/get type) "mk"))
        ;; Malagasy	mg
        mg-ratom (reaction (contains? (reagent.session/get type) "mg"))
        ;; Malay	ms
        ms-ratom (reaction (contains? (reagent.session/get type) "ms"))
        ;; Malayalam	ml
        ml-ratom (reaction (contains? (reagent.session/get type) "ml"))
        ;; Maltese	mt
        mt-ratom (reaction (contains? (reagent.session/get type) "mt"))
        ;; Maori	mi
        mi-ratom (reaction (contains? (reagent.session/get type) "mi"))
        ;; Marathi	mr
        mr-ratom (reaction (contains? (reagent.session/get type) "mr"))
        ;; Mongolian	mn
        mn-ratom (reaction (contains? (reagent.session/get type) "mn"))
        ;; Myanmar (Burmese)	my
        my-ratom (reaction (contains? (reagent.session/get type) "my"))

        ;; Nepali	ne
        ne-ratom (reaction (contains? (reagent.session/get type) "ne"))
        ;; Norwegian	no
        no-ratom (reaction (contains? (reagent.session/get type) "no"))
        ;; Odia or
        or-ratom (reaction (contains? (reagent.session/get type) "or"))

        ;; Pashto	ps
        ps-ratom (reaction (contains? (reagent.session/get type) "ps"))
        ;; Persian	fa
        fa-ratom (reaction (contains? (reagent.session/get type) "fa"))
        ;; Polish	pl
        pl-ratom (reaction (contains? (reagent.session/get type) "pl"))
        ;; Portuguese (Portugal, Brazil)	pt
        pt-ratom (reaction (contains? (reagent.session/get type) "pt"))
        ;; Punjabi	pa
        pa-ratom (reaction (contains? (reagent.session/get type) "pa"))

        ;; Romanian	ro
        ro-ratom (reaction (contains? (reagent.session/get type) "ro"))
        ;; Russian	ru
        ru-ratom (reaction (contains? (reagent.session/get type) "ru"))
        ;; Samoan	sm
        sm-ratom (reaction (contains? (reagent.session/get type) "sm"))
        ;; Scots Gaelic	gd
        gd-ratom (reaction (contains? (reagent.session/get type) "gd"))
        ;; Serbian	sr
        sr-ratom (reaction (contains? (reagent.session/get type) "sr"))

        ;; Sesotho	st
        st-ratom (reaction (contains? (reagent.session/get type) "st"))
        ;; Shona	sn
        sn-ratom (reaction (contains? (reagent.session/get type) "sn"))
        ;; Sindhi	sd
        sd-ratom (reaction (contains? (reagent.session/get type) "sd"))
        ;; Sinhala (Sinhalese)	si
        si-ratom (reaction (contains? (reagent.session/get type) "si"))
        ;; Slovak	sk
        sk-ratom (reaction (contains? (reagent.session/get type) "sk"))
        ;; Slovenian	sl
        sl-ratom (reaction (contains? (reagent.session/get type) "sl"))
        ;; Somali	so
        so-ratom (reaction (contains? (reagent.session/get type) "so"))
        ;; Spanish	es
        es-ratom (reaction (contains? (reagent.session/get type) "es"))
        ;; Sundanese	su
        su-ratom (reaction (contains? (reagent.session/get type) "su"))
        ;; Swahili	sw
        sw-ratom (reaction (contains? (reagent.session/get type) "sw"))
        ;; Swedish	sv
        sv-ratom (reaction (contains? (reagent.session/get type) "sv"))

        ;; Tajik	tg
        tg-ratom (reaction (contains? (reagent.session/get type) "tg"))
        ;; Tamil	ta
        ta-ratom (reaction (contains? (reagent.session/get type) "ta"))
        ;; Tatar tt
        tt-ratom (reaction (contains? (reagent.session/get type) "tt"))
        ;; Telugu	te
        te-ratom (reaction (contains? (reagent.session/get type) "te"))
        ;; Thai	th
        th-ratom (reaction (contains? (reagent.session/get type) "th"))
        ;; Turkish	tr
        tr-ratom (reaction (contains? (reagent.session/get type) "tr"))
        ;; Turkmen tk
        tk-ratom (reaction (contains? (reagent.session/get type) "tk"))
        ;; Ukrainian	uk
        uk-ratom (reaction (contains? (reagent.session/get type) "uk"))
        ;; Urdu	ur
        ur-ratom (reaction (contains? (reagent.session/get type) "ur"))
        ;; Uyghur ug
        ug-ratom (reaction (contains? (reagent.session/get type) "ug"))
        ;; Uzbek	uz
        uz-ratom (reaction (contains? (reagent.session/get type) "uz"))

        ;; Vietnamese	vi
        vi-ratom (reaction (contains? (reagent.session/get type) "vi"))
        ;; Welsh	cy
        cy-ratom (reaction (contains? (reagent.session/get type) "cy"))


        ;; Xhosa	xh
        xh-ratom (reaction (contains? (reagent.session/get type) "xh"))
        ;; Yiddish	yi
        yi-ratom (reaction (contains? (reagent.session/get type) "yi"))
        ;; Yoruba	yo
        yo-ratom (reaction (contains? (reagent.session/get type) "yo"))
        ;; Zulu	zu
        zu-ratom (reaction (contains? (reagent.session/get type) "zu"))
        ]
    [:table
     [:tbody
      [:tr
       [:td
        [recom/checkbox
         :model @af-ratom
         :on-change (cb-handler-fn type "af")
         :label [recom/label :label "Afrikaans" :style {:margin-top "3px"}]]]
       [:td
        [recom/checkbox
         :model @sq-ratom
         :on-change (cb-handler-fn type "sq")
         :label [recom/label :label "Albanian" :style {:margin-top "3px"}]]]
       [:td
        [recom/checkbox
         :model @am-ratom
         :on-change (cb-handler-fn type "am")
         :label [recom/label :label "Amharic" :style {:margin-top "3px"}]]]
       [:td
        [recom/checkbox
         :model @ar-ratom
         :on-change (cb-handler-fn type "ar")
         :label [recom/label :label "Arabic" :style {:margin-top "3px"}]]]
       ]
      [:tr
       [:td
        [recom/checkbox
         :model @hy-ratom
         :on-change (cb-handler-fn type "hy")
         :label [recom/label :label "Armenian" :style {:margin-top "3px"}]]]
       [:td
        [recom/checkbox
         :model @az-ratom
         :on-change (cb-handler-fn type "az")
         :label [recom/label :label "Azerbaijani" :style {:margin-top "3px"}]]]
       [:td
        [recom/checkbox
         :model @eu-ratom
         :on-change (cb-handler-fn type "eu")
         :label [recom/label :label "Basque" :style {:margin-top "3px"}]]]
       [:td
        [recom/checkbox
         :model @be-ratom
         :on-change (cb-handler-fn type "be")
         :label [recom/label :label "Belarusian" :style {:margin-top "3px"}]]]
       ]
      [:tr
       [:td
        [recom/checkbox
         :model @bn-ratom
         :on-change (cb-handler-fn type "bn")
         :label [recom/label :label "Bengali" :style {:margin-top "3px"}]]]
       [:td
        [recom/checkbox
         :model @bs-ratom
         :on-change (cb-handler-fn type "bs")
         :label [recom/label :label "Bosnian" :style {:margin-top "3px"}]]]
       [:td
        [recom/checkbox
         :model @bg-ratom
         :on-change (cb-handler-fn type "bg")
         :label [recom/label :label "Bulgarian" :style {:margin-top "3px"}]]]
       ]
      [:tr
       [:td
        [recom/checkbox
         :model @ca-ratom
         :on-change (cb-handler-fn type "ca")
         :label [recom/label :label "Catalan" :style {:margin-top "3px"}]]]
       [:td
        [recom/checkbox
         :model @eb-ratom
         :on-change (cb-handler-fn type "eb")
         :label [recom/label :label "Cebuano" :style {:margin-top "3px"}]]]
       [:td
        [recom/checkbox
         :model @ny-ratom
         :on-change (cb-handler-fn type "ny")
         :label [recom/label :label "Chichewa" :style {:margin-top "3px"}]]]
       [:td
        [recom/checkbox
         :model @zh-CN-ratom
         :on-change (cb-handler-fn type "zh-CN")
         :label [recom/label :label "Chinese (Simplified)" :style {:margin-top "3px"}]]]
       ]
      [:tr
       [:td
        [recom/checkbox
         :model @zh-TW-ratom
         :on-change (cb-handler-fn type "zh-TW")
         :label [recom/label :label "Chinese (Traditional)" :style {:margin-top "3px"}]]]
       [:td
        [recom/checkbox
         :model @co-ratom
         :on-change (cb-handler-fn type "co")
         :label [recom/label :label "Corsican" :style {:margin-top "3px"}]]]
       [:td
        [recom/checkbox
         :model @hr-ratom
         :on-change (cb-handler-fn type "hr")
         :label [recom/label :label "Croatian" :style {:margin-top "3px"}]]]
       [:td
        [recom/checkbox
         :model @cs-ratom
         :on-change (cb-handler-fn type "cs")
         :label [recom/label :label "Czech" :style {:margin-top "3px"}]]]
       ]
      [:tr
       [:td
        [recom/checkbox
         :model @da-ratom
         :on-change (cb-handler-fn type "da")
         :label [recom/label :label "Danish" :style {:margin-top "3px"}]]]
       [:td
        [recom/checkbox
         :model @nl-ratom
         :on-change (cb-handler-fn type "nl")
         :label [recom/label :label "Dutch" :style {:margin-top "3px"}]]]
       [:td
        [recom/checkbox
         :model @en-ratom
         :on-change (cb-handler-fn type "en")
         :label [recom/label :label "English" :style {:margin-top "3px"}]]]
       [:td
        [recom/checkbox
         :model @eo-ratom
         :on-change (cb-handler-fn type "eo")
         :label [recom/label :label "Esperanto" :style {:margin-top "3px"}]]]
       ]
      [:tr
       [:td
        [recom/checkbox
         :model @et-ratom
         :on-change (cb-handler-fn type "et")
         :label [recom/label :label "Estonian" :style {:margin-top "3px"}]]]

       [:td
        [recom/checkbox
         :model @tl-ratom
         :on-change (cb-handler-fn type "tl")
         :label [recom/label :label "Filipino" :style {:margin-top "3px"}]]]
       [:td
        [recom/checkbox
         :model @fi-ratom
         :on-change (cb-handler-fn type "fi")
         :label [recom/label :label "Finnish" :style {:margin-top "3px"}]]]
       [:td
        [recom/checkbox
         :model @fr-ratom
         :on-change (cb-handler-fn type "fr")
         :label [recom/label :label "French" :style {:margin-top "3px"}]]]
       [:td
        [recom/checkbox
         :model @fy-ratom
         :on-change (cb-handler-fn type "fy")
         :label [recom/label :label "Frisian" :style {:margin-top "3px"}]]]
       ]
      [:tr
       [:td
        [recom/checkbox
         :model @gl-ratom
         :on-change (cb-handler-fn type "gl")
         :label [recom/label :label "Galician" :style {:margin-top "3px"}]]]
       [:td
        [recom/checkbox
         :model @ka-ratom
         :on-change (cb-handler-fn type "ka")
         :label [recom/label :label "Georgian" :style {:margin-top "3px"}]]]

       [:td
        [recom/checkbox
         :model @de-ratom
         :on-change (cb-handler-fn type "de")
         :label [recom/label :label "German" :style {:margin-top "3px"}]]]
       [:td
        [recom/checkbox
         :model @el-ratom
         :on-change (cb-handler-fn type "el")
         :label [recom/label :label "Greek" :style {:margin-top "3px"}]]]
       ]
      [:tr
       [:td
        [recom/checkbox
         :model @gu-ratom
         :on-change (cb-handler-fn type "gu")
         :label [recom/label :label "Gujarati" :style {:margin-top "3px"}]]]
       [:td
        [recom/checkbox
         :model @ht-ratom
         :on-change (cb-handler-fn type "ht")
         :label [recom/label :label "Haitian Creole" :style {:margin-top "3px"}]]]
       [:td
        [recom/checkbox
         :model @ha-ratom
         :on-change (cb-handler-fn type "ha")
         :label [recom/label :label "Hausa" :style {:margin-top "3px"}]]]

       [:td
        [recom/checkbox
         :model @haw-ratom
         :on-change (cb-handler-fn type "haw")
         :label [recom/label :label "Hawaiian" :style {:margin-top "3px"}]]]
       ]
      [:tr
       [:td
        [recom/checkbox
         :model @iw-ratom
         :on-change (cb-handler-fn type "iw")
         :label [recom/label :label "Hebrew" :style {:margin-top "3px"}]]]
       [:td
        [recom/checkbox
         :model @hi-ratom
         :on-change (cb-handler-fn type "hi")
         :label [recom/label :label "Hindi" :style {:margin-top "3px"}]]]
       [:td
        [recom/checkbox
         :model @hmn-ratom
         :on-change (cb-handler-fn type "hmn")
         :label [recom/label :label "Hmong" :style {:margin-top "3px"}]]]
       [:td
        [recom/checkbox
         :model @hu-ratom
         :on-change (cb-handler-fn type "hu")
         :label [recom/label :label "Hungarian" :style {:margin-top "3px"}]]]

       ]
      [:tr
       [:td
        [recom/checkbox
         :model @is-ratom
         :on-change (cb-handler-fn type "is")
         :label [recom/label :label "Icelandic" :style {:margin-top "3px"}]]]
       [:td
        [recom/checkbox
         :model @ig-ratom
         :on-change (cb-handler-fn type "ig")
         :label [recom/label :label "Igbo" :style {:margin-top "3px"}]]]
       [:td
        [recom/checkbox
         :model @id-ratom
         :on-change (cb-handler-fn type "id")
         :label [recom/label :label "Indonesian" :style {:margin-top "3px"}]]]
       [:td
        [recom/checkbox
         :model @ga-ratom
         :on-change (cb-handler-fn type "ga")
         :label [recom/label :label "Irish" :style {:margin-top "3px"}]]]
       ]
      [:tr
       [:td
        [recom/checkbox
         :model @it-ratom
         :on-change (cb-handler-fn type "it")
         :label [recom/label :label "Italian" :style {:margin-top "3px"}]]]

       [:td
        [recom/checkbox
         :model @ja-ratom
         :on-change (cb-handler-fn type "ja")
         :label [recom/label :label "Japanese" :style {:margin-top "3px"}]]]
       [:td
        [recom/checkbox
         :model @jv-ratom
         :on-change (cb-handler-fn type "jv")
         :label [recom/label :label "Javanese" :style {:margin-top "3px"}]]]
       [:td
        [recom/checkbox
         :model @kn-ratom
         :on-change (cb-handler-fn type "kn")
         :label [recom/label :label "Kannada" :style {:margin-top "3px"}]]]
       ]
      [:tr
       [:td
        [recom/checkbox
         :model @kk-ratom
         :on-change (cb-handler-fn type "kk")
         :label [recom/label :label "Kazakh" :style {:margin-top "3px"}]]]
       [:td
        [recom/checkbox
         :model @km-ratom
         :on-change (cb-handler-fn type "km")
         :label [recom/label :label "Khmer" :style {:margin-top "3px"}]]]

       [:td
        [recom/checkbox
         :model @rw-ratom
         :on-change (cb-handler-fn type "rw")
         :label [recom/label :label "Kinyarwanda" :style {:margin-top "3px"}]]]
       [:td
        [recom/checkbox
         :model @ko-ratom
         :on-change (cb-handler-fn type "ko")
         :label [recom/label :label "Korean" :style {:margin-top "3px"}]]]
       ]
      [:tr
       [:td
        [recom/checkbox
         :model @ku-ratom
         :on-change (cb-handler-fn type "ku")
         :label [recom/label :label "Kurdish (Kurmanji)" :style {:margin-top "3px"}]]]
       [:td
        [recom/checkbox
         :model @ky-ratom
         :on-change (cb-handler-fn type "ky")
         :label [recom/label :label "Kyrgyz" :style {:margin-top "3px"}]]]
       [:td
        [recom/checkbox
         :model @lo-ratom
         :on-change (cb-handler-fn type "lo")
         :label [recom/label :label "Lao" :style {:margin-top "3px"}]]]

       [:td
        [recom/checkbox
         :model @la-ratom
         :on-change (cb-handler-fn type "la")
         :label [recom/label :label "Latin" :style {:margin-top "3px"}]]]
       ]
      [:tr
       [:td
        [recom/checkbox
         :model @lv-ratom
         :on-change (cb-handler-fn type "lv")
         :label [recom/label :label "Latvian" :style {:margin-top "3px"}]]]
       [:td
        [recom/checkbox
         :model @lt-ratom
         :on-change (cb-handler-fn type "lt")
         :label [recom/label :label "Lithuanian" :style {:margin-top "3px"}]]]
       [:td
        [recom/checkbox
         :model @lb-ratom
         :on-change (cb-handler-fn type "lb")
         :label [recom/label :label "Luxembourgish" :style {:margin-top "3px"}]]]
       [:td
        [recom/checkbox
         :model @mk-ratom
         :on-change (cb-handler-fn type "mk")
         :label [recom/label :label "Macedonian" :style {:margin-top "3px"}]]]

       ]
      [:tr
       [:td
        [recom/checkbox
         :model @mg-ratom
         :on-change (cb-handler-fn type "mg")
         :label [recom/label :label "Malagasy" :style {:margin-top "3px"}]]]
       [:td
        [recom/checkbox
         :model @ms-ratom
         :on-change (cb-handler-fn type "ms")
         :label [recom/label :label "Malay" :style {:margin-top "3px"}]]]
       [:td
        [recom/checkbox
         :model @ml-ratom
         :on-change (cb-handler-fn type "ml")
         :label [recom/label :label "Malayalam" :style {:margin-top "3px"}]]]
       [:td
        [recom/checkbox
         :model @mt-ratom
         :on-change (cb-handler-fn type "mt")
         :label [recom/label :label "Maltese" :style {:margin-top "3px"}]]]
       ]
      [:tr
       [:td
        [recom/checkbox
         :model @mi-ratom
         :on-change (cb-handler-fn type "mi")
         :label [recom/label :label "Maori" :style {:margin-top "3px"}]]]

       [:td
        [recom/checkbox
         :model @mr-ratom
         :on-change (cb-handler-fn type "mr")
         :label [recom/label :label "Marathi" :style {:margin-top "3px"}]]]
       [:td
        [recom/checkbox
         :model @mn-ratom
         :on-change (cb-handler-fn type "mn")
         :label [recom/label :label "Mongolian" :style {:margin-top "3px"}]]]
       [:td
        [recom/checkbox
         :model @my-ratom
         :on-change (cb-handler-fn type "my")
         :label [recom/label :label "Myanmar" :style {:margin-top "3px"}]]]

       [:td
        [recom/checkbox
         :model @ne-ratom
         :on-change (cb-handler-fn type "ne")
         :label [recom/label :label "Nepali" :style {:margin-top "3px"}]]]
       ]
      [:tr
       [:td
        [recom/checkbox
         :model @no-ratom
         :on-change (cb-handler-fn type "no")
         :label [recom/label :label "Norwegian (Bokmål)" :style {:margin-top "3px"}]]]
       [:td
        [recom/checkbox
         :model @or-ratom
         :on-change (cb-handler-fn type "or")
         :label [recom/label :label "Odia" :style {:margin-top "3px"}]]]

       [:td
        [recom/checkbox
         :model @ps-ratom
         :on-change (cb-handler-fn type "ps")
         :label [recom/label :label "Pashto" :style {:margin-top "3px"}]]]
       [:td
        [recom/checkbox
         :model @fa-ratom
         :on-change (cb-handler-fn type "fa")
         :label [recom/label :label "Persian" :style {:margin-top "3px"}]]]
       ]
      [:tr
       [:td
        [recom/checkbox
         :model @pl-ratom
         :on-change (cb-handler-fn type "pl")
         :label [recom/label :label "Polish" :style {:margin-top "3px"}]]]
       [:td
        [recom/checkbox
         :model @pt-ratom
         :on-change (cb-handler-fn type "pt")
         :label [recom/label :label "Portuguese" :style {:margin-top "3px"}]]]
       [:td
        [recom/checkbox
         :model @pa-ratom
         :on-change (cb-handler-fn type "pa")
         :label [recom/label :label "Punjabi (Gurmukhi)" :style {:margin-top "3px"}]]]

       [:td
        [recom/checkbox
         :model @ro-ratom
         :on-change (cb-handler-fn type "ro")
         :label [recom/label :label "Romanian" :style {:margin-top "3px"}]]]
       ]
      [:tr
       [:td
        [recom/checkbox
         :model @ru-ratom
         :on-change (cb-handler-fn type "ru")
         :label [recom/label :label "Russian" :style {:margin-top "3px"}]]]
       [:td
        [recom/checkbox
         :model @sm-ratom
         :on-change (cb-handler-fn type "sm")
         :label [recom/label :label "Samoan" :style {:margin-top "3px"}]]]
       [:td
        [recom/checkbox
         :model @gd-ratom
         :on-change (cb-handler-fn type "gd")
         :label [recom/label :label "Scots Gaelic" :style {:margin-top "3px"}]]]
       [:td
        [recom/checkbox
         :model @sr-ratom
         :on-change (cb-handler-fn type "sr")
         :label [recom/label :label "Serbian" :style {:margin-top "3px"}]]]
       ]
      [:tr
       [:td
        [recom/checkbox
         :model @st-ratom
         :on-change (cb-handler-fn type "st")
         :label [recom/label :label "Sesotho" :style {:margin-top "3px"}]]]
       [:td
        [recom/checkbox
         :model @sn-ratom
         :on-change (cb-handler-fn type "sn")
         :label [recom/label :label "Shona" :style {:margin-top "3px"}]]]
       [:td
        [recom/checkbox
         :model @sd-ratom
         :on-change (cb-handler-fn type "sd")
         :label [recom/label :label "Sindhi" :style {:margin-top "3px"}]]]
       [:td
        [recom/checkbox
         :model @si-ratom
         :on-change (cb-handler-fn type "si")
         :label [recom/label :label "Sinhala" :style {:margin-top "3px"}]]]
       ]
      [:tr
       [:td
        [recom/checkbox
         :model @sk-ratom
         :on-change (cb-handler-fn type "sk")
         :label [recom/label :label "Slovak" :style {:margin-top "3px"}]]]

       [:td
        [recom/checkbox
         :model @sl-ratom
         :on-change (cb-handler-fn type "sl")
         :label [recom/label :label "Slovenian" :style {:margin-top "3px"}]]]
       [:td
        [recom/checkbox
         :model @so-ratom
         :on-change (cb-handler-fn type "so")
         :label [recom/label :label "Somali" :style {:margin-top "3px"}]]]
       [:td
        [recom/checkbox
         :model @es-ratom
         :on-change (cb-handler-fn type "es")
         :label [recom/label :label "Spanish" :style {:margin-top "3px"}]]]
       ]
      [:tr
       [:td
        [recom/checkbox
         :model @su-ratom
         :on-change (cb-handler-fn type "su")
         :label [recom/label :label "Sundanese" :style {:margin-top "3px"}]]]
       [:td
        [recom/checkbox
         :model @sw-ratom
         :on-change (cb-handler-fn type "sw")
         :label [recom/label :label "Swahili" :style {:margin-top "3px"}]]]

       [:td
        [recom/checkbox
         :model @sv-ratom
         :on-change (cb-handler-fn type "sv")
         :label [recom/label :label "Swedish" :style {:margin-top "3px"}]]]
       [:td
        [recom/checkbox
         :model @tg-ratom
         :on-change (cb-handler-fn type "tg")
         :label [recom/label :label "Tajik" :style {:margin-top "3px"}]]]

       ]

      [:tr
       [:td
        [recom/checkbox
         :model @ta-ratom
         :on-change (cb-handler-fn type "ta")
         :label [recom/label :label "Tamil" :style {:margin-top "3px"}]]]
       ;; voice output unavailable for tator
       [:td
        [recom/checkbox
         :model @tt-ratom
         :on-change (cb-handler-fn type "tt")
         :label [recom/label :label "Tatar" :style {:margin-top "3px"}]]]
       [:td
        [recom/checkbox
         :model @te-ratom
         :on-change (cb-handler-fn type "te")
         :label [recom/label :label "Telugu" :style {:margin-top "3px"}]]]

       [:td
        [recom/checkbox
         :model @th-ratom
         :on-change (cb-handler-fn type "th")
         :label [recom/label :label "Thai" :style {:margin-top "3px"}]]]

       ]
      [:tr
       [:td
        [recom/checkbox
         :model @tr-ratom
         :on-change (cb-handler-fn type "tr")
         :label [recom/label :label "Turkish" :style {:margin-top "3px"}]]]
       [:td
        [recom/checkbox
         :model @tk-ratom
         :on-change (cb-handler-fn type "tk")
         :label [recom/label :label "Turkmen" :style {:margin-top "3px"}]]]
       [:td
        [recom/checkbox
         :model @uk-ratom
         :on-change (cb-handler-fn type "uk")
         :label [recom/label :label "Ukrainian" :style {:margin-top "3px"}]]]
       [:td
        [recom/checkbox
         :model @ur-ratom
         :on-change (cb-handler-fn type "ur")
         :label [recom/label :label "Urdu" :style {:margin-top "3px"}]]]


       ]
      [:tr
       [:td
        [recom/checkbox
         :model @ug-ratom
         :on-change (cb-handler-fn type "ug")
         :label [recom/label :label "Uyghur" :style {:margin-top "3px"}]]]
       [:td
        [recom/checkbox
         :model @uz-ratom
         :on-change (cb-handler-fn type "uz")
         :label [recom/label :label "Uzbek" :style {:margin-top "3px"}]]]
       [:td
        [recom/checkbox
         :model @vi-ratom
         :on-change (cb-handler-fn type "vi")
         :label [recom/label :label "Vietnamese" :style {:margin-top "3px"}]]]
       [:td
        [recom/checkbox
         :model @cy-ratom
         :on-change (cb-handler-fn type "cy")
         :label [recom/label :label "Welsh" :style {:margin-top "3px"}]]]

       ]
      [:tr
       [:td
        [recom/checkbox
         :model @xh-ratom
         :on-change (cb-handler-fn type "xh")
         :label [recom/label :label "Xhosa" :style {:margin-top "3px"}]]]
       [:td
        [recom/checkbox
         :model @yi-ratom
         :on-change (cb-handler-fn type "yi")
         :label [recom/label :label "Yiddish" :style {:margin-top "3px"}]]]
       [:td
        [recom/checkbox
         :model @yo-ratom
         :on-change (cb-handler-fn type "yo")
         :label [recom/label :label "Yoruba" :style {:margin-top "3px"}]]]
       ]
      [:tr
       [:td
        [recom/checkbox
         :model @zu-ratom
         :on-change (cb-handler-fn type "zu")
         :label [recom/label :label "Zulu" :style {:margin-top "3px"}]]]]
      ]
     ]
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
  (go
    (log "POPUP: init")
    (connect-to-background-page!)
    (prn "init reagent.session/state")

    ;; (reagent.session/reset! (<! (storage/get-ui-state)))
    (reagent.session/reset! {:target #{}
                             :source #{}})

    (add-watch reagent.session/state :target
               (fn [key atom old-state new-state]
                 (prn "new-state: " new-state)
                 (storage/set-ui-state new-state)
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
                                 )))
            _ (prn csv-data)
            ]
        (recur)
        ))

    (mount-root))
  )
