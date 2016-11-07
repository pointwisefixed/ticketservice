(ns ticket-service.db.core
  (:require
    [conman.core :as conman]
    [mount.core :refer [defstate]]
    [ticket-service.config :refer [env]]))

(defstate ^:dynamic *db*
           :start (conman/connect! {:jdbc-url (env :database-url)})
           :stop (conman/disconnect! *db*))

(conman/bind-connection *db* "sql/queries.sql")

