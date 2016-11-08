(ns ticket-service.test.service.tickets
  (:require [ticket-service.service.tickets :refer :all]
            [clojure.test :refer :all]))

(defn ticket-service-fixture [f]
  (reset-theater)
  (let [a-pool (start-scheduler)]
    (f)
    (shutdown-scheduler a-pool)))

(use-fixtures :each ticket-service-fixture)

(deftest test-hold-seats
  (testing "holding-seats"
    (let [hold-id (hold-seats 10 "gary@garyrosales.com")]
      (is (= 90 (count @available-map)))
      (is (= 1 (count @hold-map)))
      (is (= 10 (count (flatten (map #(:seats %) @hold-map))))))))

(deftest test-reserve-seat
  (testing "reserve seats"
    (let [hold-id (hold-seats 10 "gary@garyrosales.com")
          reserve-id (reserve-seats hold-id)]
      (is (= 90 (count @available-map)))
      (is (= 10 (count (flatten (map #(:seats %) @reserved-map)))))
      (is (= 0 (count @hold-map))))))