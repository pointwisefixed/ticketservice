(ns user
  (:require [mount.core :as mount]
            ticket-service.core))

(defn start []
  (mount/start-without #'ticket-service.core/repl-server))

(defn stop []
  (mount/stop-except #'ticket-service.core/repl-server))

(defn restart []
  (stop)
  (start))


