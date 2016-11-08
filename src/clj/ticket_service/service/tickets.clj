(ns ticket-service.service.tickets
  (:require [clojure.set :refer :all])
  (:import (java.time Instant)
           (java.util.concurrent Executors ScheduledThreadPoolExecutor TimeUnit)))

(defrecord Position [row col])

(defrecord Seat [name position])

(defrecord Hold [id email seats expiration-timestamp])

(def get-timestamp (.toEpochMilli (.plusSeconds (Instant/now) 60)))

(defn seat-name [row col] (str (char (+ row 96)) col))

(def theater (flatten (for [i (range 1 11)] (for [k (range 1 11)] (Seat. (seat-name i k) (Position. i k))))))

(def available-map (ref (set theater)))

(def hold-map (ref #{}))

(def reserved-map (ref #{}))

(defn best-by-random [quantity available-map]
  (take quantity available-map))

(defn hold [email seats available-map hold-map]
  (let [holding (Hold. (.toString (java.util.UUID/randomUUID)) email seats get-timestamp)]
    (alter available-map difference (set seats))
    (alter hold-map conj holding)
    holding))

(defn find-expired [hold-map]
  (filter #(.isAfter (Instant/now) (Instant/ofEpochMilli (:expiration-timestamp %))) hold-map))

(defn get-seats-from-holds [expired-holds]
  (map #(:seats expired-holds) expired-holds))

(defn release-seats [available hold]
  (dosync
    (let [expired-holds (find-expired @hold) expired-seats (get-seats-from-holds expired-holds)]
      (when (not-empty expired-holds)
        (alter hold difference (set expired-holds))
        (alter available conj expired-seats)))))

(defn hold-seats [quantity email]
  (dosync
    (release-seats available-map hold-map)
    (let [best-seats (best-by-random quantity @available-map)]
      (:id (hold email best-seats available-map hold-map)))))

(defn reserve-seats [hold-id]
  (dosync
    (release-seats available-map hold-map)
    (let [hold (first (filter #(= (:id %) hold-id) @hold-map))]
      (alter hold-map difference #{hold})
      (alter reserved-map conj hold)
      hold-id)))

(defn reset-theater []
  (dosync
    (ref-set available-map (set theater))
    (ref-set hold-map #{})
    (ref-set reserved-map #{})))

(defn pool [n] (Executors/newScheduledThreadPool n))

(defn start-scheduler [] (let [a-pool (pool 1)]
                           (.scheduleAtFixedRate a-pool #(release-seats available-map hold-map) 0 1 TimeUnit/SECONDS)
                           a-pool))

(defn shutdown-scheduler [a-pool]
  (.shutdownNow a-pool))