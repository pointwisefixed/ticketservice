(ns ticket-service.env
  (:require [selmer.parser :as parser]
            [clojure.tools.logging :as log]
            [ticket-service.dev-middleware :refer [wrap-dev]]))

(def defaults
  {:init
   (fn []
     (parser/cache-off!)
     (log/info "\n-=[ticket-service started successfully using the development profile]=-"))
   :stop
   (fn []
     (log/info "\n-=[ticket-service has shut down successfully]=-"))
   :middleware wrap-dev})
