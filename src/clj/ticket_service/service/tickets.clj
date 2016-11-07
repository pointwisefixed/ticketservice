(ns ticket-service.service.tickets
  (:import (java.time Instant)))

(defrecord Position [row col])

(defrecord Seat [name position])

(defrecord Hold [id email seats expiration-timestamp])

(def get-timestamp (.toEpochMilli (.plusSeconds (Instant/now) 60)))

(defn seat-name [row col] (str (char (+ row 96)) col))

(def theater (flatten (for [i (range 1 11)] (for [k (range 1 11)] (Seat. (seat-name i k) (Position. i k))))))

(def available-map (ref theater))

(def hold-map (ref {}))

(def reserved-map (ref {}))

(defn best-by-random [quantity available-map]
  (take quantity available-map))

(defn hold [email seats available-map hold-map]
  (alter available-map dissoc seats)
  (alter hold-map conj (Hold. (.toString (java.util.UUID/randomUUID)) email seats (get-timestamp))))

(defn find-expired [hold-map]
  (filter #(.isAfter (Instant/now) (Instant/ofEpochMilli (:expiration-timestamp %))) hold-map))

(defn get-seats-from-holds [expired-holds]
  (map #(:seats expired-holds) expired-holds))

(defn release-seats [available hold]
  (let [expired-holds (find-expired @hold) expired-seats (get-seats-from-holds expired-holds)]
    (alter hold dissoc expired-holds)
    (alter available conj expired-seats)))

(defn hold-seats [quantity email]
  (dosync
    (release-seats available-map hold-map)
    (let [best-seats (best-by-random quantity @available-map)]
      (hold email best-seats available-map hold-map))))

(defn reserve-seats [hold-id]
  (dosync
    (release-seats available-map hold-map)
    (let [hold (some #(= (:id %) hold-id) @hold-map)]
      (alter hold-map dissoc hold)
      (alter reserved-map conj hold))))