(ns ticket-service.env
  (:require [clojure.tools.logging :as log]))

(def defaults
  {:init
   (fn []
     (log/info "\n-=[ticket-service started successfully]=-"))
   :stop
   (fn []
     (log/info "\n-=[ticket-service has shut down successfully]=-"))
   :middleware identity})
