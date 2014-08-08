(ns consumer.core 
  (:use compojure.core)
  (:require [compojure.route       :as route]
            [immutant.web          :as web]
            [immutant.web.servlet  :as servlet]
            [clj-http.client       :as client]
            [hiccup.core           :as hiccup]
            [cheshire.core         :as chesire])
  (:gen-class))

(defn get-session
  "Gets the session, get the object with `.getToken` or the access token itself with `.getTokenString`"
  [request]
  (let [{servlet-request :servlet-request} request
        session (.getAttribute servlet-request "org.keycloak.KeycloakSecurityContext")]
    session))

(defn get-visualization-list 
  "Retrieves the visualization list from the provider"
  [request]
  (let [data (client/get "http://provider:8080/api/" {:headers {
        "Authorization" (str "Bearer " (.getTokenString (get-session request)))}})]
      (chesire/decode (:body data))))

(defn home-route
  "Renders the homepage"
  [request]
  ; TODO
  "Hello, there!")

(defn dashboard-route
  "Renders the dashboard"
  [request]
  ; TODO
  (apply str (get-visualization-list request)))

(defn view-route
  "Renders the view of a specified visualization"
  [request id]
  ; TODO
  (apply str (get-visualization-list request)))

(defn report-route
  "Renders a specified report"
  [request id]
  ; TODO
  (apply str (get-visualization-list request)))

(defroutes app
  "The router."
  (GET "/" [:as request] (home-route request))
  (GET "/dashboard" [:as request] (dashboard-route request))
  (GET "/view/:id" [id :as request] (view-route request id))
  (GET "/report/:id" [id :as request] (report-route request id))
  (route/resources "/assets/")
  (route/not-found "<pre>Page not found</pre>"))

(defn -main
  "Start the server"
  [& args]
  ; Start the server.
  (web/run (servlet/create-servlet app) :port 8080))
