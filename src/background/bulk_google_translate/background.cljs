(ns bulk-google-translate.background
  (:require-macros [chromex.support :refer [runonce]])
  (:require [bulk-google-translate.background.core :as core]))

(runonce
  (core/init!))
