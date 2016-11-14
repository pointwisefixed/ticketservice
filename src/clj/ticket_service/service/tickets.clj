(ns ticket-service.service.tickets
  (:require [clojure.set :refer :all]
            [clojure.set :as set]
            [clojure.tools.logging :as log]
            [clojurewerkz.quartzite.scheduler :as qs]
            [clojurewerkz.quartzite.triggers :as t]
            [clojurewerkz.quartzite.jobs :as j]
            [clojurewerkz.quartzite.jobs :refer [defjob]]
            [clojurewerkz.quartzite.schedule.simple :refer
             [schedule with-repeat-count with-interval-in-milliseconds repeat-forever]])
  (:import (java.time Instant ZoneId)
           (java.time.format DateTimeFormatter)))

(def hold-timeout 30)
(def job-interval 1000)

(defrecord Position [row col])

(defrecord Seat [name position])

(defrecord Hold [id email seats expiration-timestamp])

(defn get-expiration-milli [] (.toEpochMilli (.plusSeconds (Instant/now) hold-timeout)))

(defn seat-name [row col] (str (char (+ row 96)) col))

(def theater (flatten (for [i (range 1 11)] (for [k (range 1 11)] (Seat. (seat-name i k) (Position. i k))))))

(def available-map (ref (set theater)))

(def hold-map (ref #{}))

(def reserved-map (ref #{}))

(defn get-time [instant]
  (.format (.withZone (DateTimeFormatter/ofPattern "yyyy-MM-dd HH:mm:ss,zzz") (ZoneId/systemDefault)) instant))

(defn get-time-from-milis [time-in-millis]
  (get-time (Instant/ofEpochMilli time-in-millis)))

(defn best-by-random [quantity available-map]
  (take quantity available-map))

(defn hold [email seats available-map hold-map]
  (let [holding (Hold. (.toString (java.util.UUID/randomUUID)) email seats (get-expiration-milli))]
    (alter available-map difference (set seats))
    (alter hold-map conj holding)
    (log/debug "Holding seats for" email "at" (get-time (Instant/now)) "with timeout" hold-timeout
               "and time to expire at" (get-time-from-milis (:expiration-timestamp holding)))
    holding))

(defn find-expired [hold-map]
  (filter #(.isAfter (Instant/now) (Instant/ofEpochMilli (:expiration-timestamp %))) hold-map))

(defn release-seats [available hold]
  (dosync
    (let [expired-holds (find-expired @hold) expired-seats (flatten (map #(:seats %) expired-holds))]
      (when (not-empty expired-seats)
        (log/debug "Expiring" (count expired-seats) "at" (get-time (Instant/now))
                   "with timeout" (map #(get-time-from-milis (:expiration-timestamp %)) expired-holds))
        (alter hold difference (set expired-holds))
        (alter available set/union expired-seats)))))

(defn hold-seats [quantity email]
  (dosync
    (release-seats available-map hold-map)
    (log/debug "trying to hold" quantity "seats for" email)
    (let [best-seats (best-by-random quantity @available-map)]
      (hold email best-seats available-map hold-map))))

(defn reserve-seats [hold-id]
  (dosync
    (log/debug "Reserving seat with id" hold-id)
    (release-seats available-map hold-map)
    (let [hold (first (filter #(= (:id %) hold-id) @hold-map))]
      (alter hold-map difference #{hold})
      (when (not-empty hold) (alter reserved-map conj hold))
      hold-id)))

(defn reset-theater []
  (dosync
    (ref-set available-map (set theater))
    (ref-set hold-map #{})
    (ref-set reserved-map #{})))

(def ^:dynamic scheduler (ref nil))

(defjob ReleaseSeatJob
        [ctx]
        (log/debug "Executing Releasing Seat Job")
        (release-seats available-map hold-map))

(defn start-jobs []
  (let [s (-> (qs/initialize) qs/start)
        job (j/build
              (j/of-type ReleaseSeatJob)
              (j/with-identity (j/key "releasejobs.op.1")))
        trigger-key (t/key "trigger.1")
        trigger (t/build
                  (t/with-identity trigger-key)
                  (t/start-now)
                  (t/with-schedule (schedule
                                     (repeat-forever)
                                     (with-interval-in-milliseconds job-interval))))]
    (dosync
      (ref-set scheduler s)
      (qs/schedule @scheduler job trigger)
      (log/info "Started and scheduled job"))))

(defn stop-jobs []
  (log/info "Stopping job")
  (qs/shutdown @scheduler false))