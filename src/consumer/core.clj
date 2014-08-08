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
      (chesire/parse-string (:body data) true)))

(defn render-plain-page
  "Builds a plain page with no sidebar"
  [content]
  (let [js ["c3.js" "d3.v3.js" "jquery-2.1.1.js" "bootstrap.js" "jasny-bootstrap.js"]
        css ["bootstrap.css" "jasny-bootstrap.css" "font-awesome.css" "c3.css" "auxilary.css"]]
    (hiccup/html 
      [:html
        [:head
          (for [item js] [:script {:src (str "/assets/js/" item)}])
          (for [item css] [:link {:rel "stylesheet" :href (str "/assets/css/" item)}])]
        [:body 
          content]])))

(defn render-page
  "Builds a page with the sidebar"
  [request content]
  (render-plain-page
    [:div.navmenu.navmenu-inverse.navmenu-fixed-left.offcanvas
      [:a.navmenu-brand {:href "/"}]
      [:ul.nav.navmenu-nav
        (cons [:li 
          [:a.navmenu-header {:href "/"}
            [:i.fa.fa-question]
            "Visualizations"]]
          (for [vis (get-visualization-list request)]
            [:a {:href (str "/view/" (:_id vis))
                 :data-toggle "tooltip"
                 :data-placement "right"
                 :title (:description vis)} 
                 (:title vis)]))]]))


(defn home-route
  "Renders the homepage"
  [request]
  {:status 200
   :body (hiccup/html [:h1 "Hello"])})

(defn dashboard-route
  "Renders the dashboard"
  [request]
  ; TODO
  (render-page request [:h1 "Hello hello!"]))

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
