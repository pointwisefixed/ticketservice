(ns ticket-service.test.service.tickets
  (:require [ticket-service.service.tickets :refer :all]
            [clojure.test :refer :all])
  (:import (java.util.concurrent TimeUnit)))

(defn ticket-service-fixture [f]
  (reset-theater)
  (start-jobs)
  (f)
  (stop-jobs))

(use-fixtures :each ticket-service-fixture)

(deftest test-hold-seats
  (testing "holding-seats"
    (let [holdding (hold-seats 10 "gary@garyrosales.com")]
      (is (= 90 (count @available-map)))
      (is (= 1 (count @hold-map)))
      (is (= 10 (count (flatten (map #(:seats %) @hold-map))))))))

(deftest test-hold-multiple
  (testing "test hold multiple and expire"
    (let [holding (hold-seats 10 "gary@garyrosales.com")]
      (is (= 90 (count @available-map)))
      (is (= 1 (count @hold-map)))
      (is (= 0 (count (flatten (map #(:seats %) @reserved-map)))))
      (hold-seats 10 "gary1@garyrosales.com")
      (is (= 80 (count @available-map)))
      (is (= 2 (count @hold-map)))
      (.sleep TimeUnit/MILLISECONDS 31000)
      (is (= 100 (count @available-map)))
      (is (= 0 (count @hold-map))))))

(deftest test-reserve-tickets
  (testing "reserve seats"
    (let [holding (hold-seats 10 "gary@garyrosales.com")
          reserve-id (reserve-seats (:id holding))]
      (is (= 90 (count @available-map)))
      (is (= 10 (count (flatten (map #(:seats %) @reserved-map)))))
      (is (= 0 (count @hold-map))))))