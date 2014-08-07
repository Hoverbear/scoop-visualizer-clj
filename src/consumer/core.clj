(ns consumer.core 
  (:use compojure.core)
  (:require [compojure.route       :as route]
            [immutant.web          :as web]
            [immutant.web.servlet  :as servlet]
            [clj-http.client       :as client])
  (:gen-class))

(defn get-session
  "Gets the session, get the object with `.getToken` or the access token itself with `.getTokenString`"
  [request]
  (let [{servlet-request :servlet-request} request
        session (.getAttribute servlet-request "org.keycloak.KeycloakSecurityContext")]
    session))

(defn back-channel-handler
  "A backchannel request handler"
  [request]
  (try
    (client/get "http://provider:8080/api/"
      {:headers {
        "Authorization" (str "Bearer " (.getTokenString (get-session request)))
      }})
    (catch Exception e
      {:status 404
       :body (str "Exception occured... <pre>" e "</pre>")})))

(defroutes app
  "The router."
  (GET "/backchannel" [:as request] (back-channel-handler request))
  ; Logout
  ; Doesn't currently work: https://issues.jboss.org/browse/KEYCLOAK-478
  (GET "/logout" [:as request]
    (let [{servlet-request :servlet-request} request]
      (.logout servlet-request)
      (str "Logged out")))
  (route/not-found "<h1>Page not found</h1>"))

(defn -main
  "Start the server"
  [& args]
  ; Start the server.
  (web/run (servlet/create-servlet app) :port 8080))
