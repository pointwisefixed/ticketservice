(ns ticket-service.routes.services
  (:require [ring.util.http-response :refer :all]
            [compojure.api.sweet :refer :all]
            [schema.core :as s]
            [ticket-service.service.tickets :as ticketservice]))

(s/defschema SeatModel {:name s/Str
                        :position {:row s/Int
                                    :col s/Int}})

(s/defschema HoldModel {:id s/Str
                        :email s/Str
                        :seats [SeatModel]
                        :expiration-timestamp s/Num})

(defapi service-routes
  {:swagger {:ui "/swagger-ui"
             :spec "/swagger.json"
             :data {:info {:version "1.0.0"
                           :title "Ticket Service"
                           :description "Ticket Service"}}}}

  (context "/api" []
    :tags ["ticket-service"]

    (GET "/seats" []
      :return       [SeatModel]
      :summary      "Returns the theater"
      (ok ticketservice/theater))

    (GET "/available" []
      :return       Long
      :summary      "Returns the available seats count"
      (ok (count @ticketservice/available-map)))

    (POST "/hold" []
      :return      HoldModel
      :body-params [quantity :- Integer, email :- String]
      :summary     "Holds (quantity) of seats to an email"
      (ok (ticketservice/hold-seats quantity email)))

    (POST "/reserve" []
      :return      String
      :body-params [holdId :- String]
      :summary     "Reserves the hold if it exists"
      (ok (ticketservice/reserve-seats holdId)))))
