(ns ticket-service.test.service.tickets
  (:require [ticket-service.service.tickets :refer :all]
            [clojure.test :refer :all]))

(deftest test-hold-seats
  (testing "holding-seats"
    (let [hold (hold-seats 10 "gary@garyrosales.com")]
      (is (= 90 (count @available-map)))
      (is (= 1 (count @hold-map)))
      (is (= 10 (count (flatten (map #(:seats %) @hold-map))))))))
