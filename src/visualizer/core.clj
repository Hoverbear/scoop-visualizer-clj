(ns visualizer.core 
  (:use compojure.core)
  (:require [compojure.route       :as route]
            [immutant.web          :as web]
            [immutant.web.servlet  :as servlet]
            [clj-http.client       :as client]
            [hiccup.core           :as hiccup]
            [cheshire.core         :as chesire]
            [monger.core           :as monger]
            [monger.json]
            [monger.collection     :as collections])
  (:import [com.mongodb MongoOptions ServerAddress])
  (:import org.bson.types.ObjectId)
  (:gen-class))

(def provider-url "http://provider.scoop.local:8080")

; MongoDB Details
(def db (monger/get-db 
          (monger/connect {:host "database"})
          "visualizer"))

(defn get-session
  "Gets the session, get the object with `.getToken` or the access token itself with `.getTokenString`"
  [request]
  (let [{servlet-request :servlet-request} request
        session (.getAttribute servlet-request "org.keycloak.KeycloakSecurityContext")]
    session))


(defn get-query-list 
  "Retrieves the query list from the provider"
  [request]
  (let [data (client/get (str provider-url "/api/") 
                         {:headers {"Authorization" 
                                    (str "Bearer " (.getTokenString (get-session request)))}})]
    (chesire/parse-string (:body data) true)))

(defn get-query
  "Retrieves the query from the provider"
  [request id]
  (let [data (client/get (str provider-url "/api/" id)
                         {:headers {"Authorization"
                                    (str "Bearer " (.getTokenString (get-session request)))}})]
    (chesire/parse-string (:body data) true)))

(defn get-visualization
  "Pulls the visualization data for a given query ID"
  [id]
  (collections/find-one-as-map db "visualizations" {:query_id (ObjectId. id)}))

(defn render-plain-page
  "Builds a plain page with no sidebar"
  [& content]
  (let [js ["c3-0.2.5.js" "d3.v3.js" "jquery-2.1.1.js" "bootstrap.js" "jasny-bootstrap.js" "codemirror.js" "jshint.js" "mode-javascript.js" "addon-lint.js" "addon-javascript-lint.js"]
        css ["bootstrap.css" "jasny-bootstrap.css" "font-awesome.css" "c3.css" "auxilary.css" "codemirror.css" "neo.css" "lint.css"]]
    (hiccup/html 
      [:html
       [:head
        (for [item js] [:script {:src (str "/assets/js/" item)}])
        (for [item css] [:link {:rel "stylesheet" :href (str "/assets/css/" item)}])]
       [:body 
        content]])))

(defn render-page
  "Builds a page with the sidebar"
  [request & content]
  (render-plain-page
    [:div.navmenu.navmenu-inverse.navmenu-fixed-left.offcanvas
     [:a.navmenu-brand {:href "/"}]
     [:ul#queries.nav.navmenu-nav
      (cons 
        [:li [:a.navmenu-header {:href "/"}
              [:i.fa.fa-question]
              " Visualizations"]]
        (for [vis (get-query-list request)]
          [:li [:a {:href (str "/view/" (:_id vis))
                    :data-toggle "tooltip"
                    :data-placement "right"
                    :title (:description vis)} 
                (:title vis)]]))]
     [:script "$(document).ready(function() { $('ul#queries > li > a').tooltip(); });"]]
    [:div.navbar.navbar-default.navbar-fixed-top
     [:button.navbar-toggle {:data-toggle "offcanvas"
                             :data-target ".navmenu"
                             :data-canvas "body"}
      (for [x (range 3)] [:span.icon-bar])]]
    [:div.container
     content]))


(defn home-route
  "Renders the homepage"
  [request]
  (render-plain-page 
    [:div.container
     [:div.row.text-center 
      [:h1 "Visualizer"]
      [:hr]
      [:h3.text-muted "A project supported by the PDC and NSERC. By Lead/Simbioses Lab."]]
     [:div.row.text-center
      [:div "In order to utilize this software, you need to be an authenticated user."]
      [:a.btn.btn-primary.btn-labeled {:href "/dashboard"}
       [:span.btn-label
        [:i.fa.fa-key]]
       "Authenicate"]]]))

(defn dashboard-route
  "Renders the dashboard"
  [request]
  ; TODO
  (render-page
    request
    [:h1 "Hello hello!"]))

(defn view-route
  "Renders the view of a specified visualization"
  [request id]
  ; TODO
  (let [query (get-query request id)
        visualization (get-visualization id)]
    (render-page 
      request
      [:div#head.row.text-center
       [:h1 (:title query)]
       [:h4 (:description query)]]
      [:div#vis.row
       [:div.panel.panel-default
        [:div.panel-body
         [:div#chart]]
        [:script (str "var visualization = " (chesire/generate-string query) ";")]]]
      [:div.row
       [:ul.nav.nav-pills {:role "tablist"}
         [:li.active [:a {:href "#info", :role "tab", :data-toggle "tab"} "Info"]]
         [:li [:a {:href "#edit", :role "tab", :data-toggle "tab"} "Edit"]]
         [:li [:a {:href "#debug", :role "tab", :data-toggle "tab"} "Debug"]]]
        [:div.tab-content
         [:div#info.tab-pane.active
          [:p [:strong "Number of Endpoints Participating: "] (count (:data query))]]
         [:div#edit.tab-pane
          [:div.col-xs-12
           [:textarea#visualization-editor (chesire/generate-string visualization {:pretty true})]
           [:script "var visEditor; 
                     $(document).ready(function() {
                       visEditor = CodeMirror.fromTextArea(
                         document.getElementById('visualization-editor'), {
                         mode: 'javascript',
                         lineNumbers: true,
                         gutters: ['CodeMirror-lint-markers'],
                         lint: true,
                         hightlightSelectionMatches: true,
                         matchTags: true,
                         matchBrackets: true,
                         hint: true
                       });
                       $('a[href=\"#edit\"').click(function () {
                         setTimeout(function () {
                           visEditor.refresh();
                         }, 0);
                       });
                     });"]]]
         [:div#debug.tab-pane 
          [:div#debug-editor (chesire/generate-string query {:pretty true})]]]])))

(defn report-route
  "Renders a specified report"
  [request id]
  ; TODO
  (apply str (get-query-list request)))

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
