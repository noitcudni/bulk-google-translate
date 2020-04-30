(ns bulk-google-translate.popup
  (:require-macros [chromex.support :refer [runonce]])
  (:require [bulk-google-translate.popup.core :as core]))

(runonce
  (core/init!))
