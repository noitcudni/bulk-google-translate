(ns bulk-google-translate.content-script
  (:require-macros [chromex.support :refer [runonce]])
  (:require [bulk-google-translate.content-script.core :as core]))

(runonce
  (core/init!))
