(ns visualizer.core 
  (:use compojure.core)
  (:require [compojure.route       :as route]
            [immutant.web          :as web]
            [clj-http.client       :as client]
            [hiccup.core           :as hiccup]
            [cheshire.core         :as chesire]
            [monger.core           :as monger]
            [monger.json]
            [monger.collection     :as collections]
            [monger.operators      :refer :all])
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

(defn get-visualizations
  "Pulls the visualization data for a given query ID"
  [id]
  (collections/find-one-as-map db "visualizations" {:query_id (ObjectId. id)}))

(defn render-plain-page
  "Builds a plain page with no sidebar"
  [& content]
  (let [js ["c3-0.2.5.js" "d3.v3.js" "jquery-2.1.1.js" "bootstrap.js" "jasny-bootstrap.js" 
            "codemirror.js" "jshint.js" "mode-javascript.js" "addon-lint.js"
            "lodash.js" "addon-javascript-lint.js"]
        css ["bootstrap.css" "jasny-bootstrap.css" "font-awesome.css" "c3.css" "auxilary.css"
             "codemirror.css" "neo.css" "lint.css"]]
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
        visualizations (get-visualizations id)]
    (render-page 
      request
      [:div#head.row.text-center
       [:h1 (:title query)]
       [:h4 (:description query)]]
      [:div#vis.row
       [:div.panel.panel-default
        [:div.panel-body
         [:div#chart]]
        [:script (str "var query = " (chesire/generate-string query) ";"
                      "var visualizations = " (chesire/generate-string visualizations) ";")]]]
      [:div.row
       [:ul.nav.nav-tabs {:role "tablist"}
         [:li.active [:a {:href "#info", :role "tab", :data-toggle "tab"} "Info"]]
         [:li [:a {:href "#edit", :role "tab", :data-toggle "tab"} "Edit"]]
         [:li [:a {:href "#debug", :role "tab", :data-toggle "tab"} "Debug"]]]
        [:div.tab-content
         [:div#info.tab-pane.active
          [:p [:strong "Number of Endpoints Participating: "] (count (:data query))]]
         [:div#edit.tab-pane
          [:div.col-xs-12
           [:div.row
            [:div.btn-group
             [:button#eval.btn.btn-sm.btn-primary "Eval"]
             [:button#new.btn.btn-sm.btn-success "New"]
             [:button#save.btn.btn-sm.btn-info "Save"]]
            [:select#chooser (map
                               (fn [choice] [:option {:value (:label choice)} (:label choice)])
                               (:choices visualizations))]]
           [:div.row
            [:input#label]
            [:textarea#visualization-editor.row]
            [:script "var visEditor; 
                      $(document).ready(function() {
                        // First Open
                        try {
                         if (visualizations.choices.length > 0) {
                            $('#visualization-editor').val(visualizations.choices[0].code);
                            $('#label').val(visualizations.choices[0].label);
                            eval($('#visualization-editor').val());
                         }
                        } catch (e) {
                          $('#chart').html('<pre>No Visualizations defined.</pre>');
                        }
                        // Initialize CodeMirror.
                        visEditor = CodeMirror.fromTextArea(document.getElementById('visualization-editor'), {
                          mode: 'javascript',
                          lineNumbers: true,
                          gutters: ['CodeMirror-lint-markers'],
                          lint: true,
                          hightlightSelectionMatches: true,
                          matchTags: true,
                          matchBrackets: true,
                          hint: true
                        });
                        // Workaround for on-click.
                        $('a[href=\"#edit\"]').click(function () {
                          setTimeout(function () {
                            visEditor.refresh();
                          }, 0);
                        });

                        $('#eval').click(function () {
                          $('#chart').html(''); // Clear it.
                          eval(visEditor.getValue());
                        });  

                        $('#chooser').change(function () {
                          var label = $('#chooser').val(),
                              code;
                          for (var i = 0; i < visualizations.choices.length; i++) {
                            if (visualizations.choices[i].label === label) {
                              code = visualizations.choices[i].code;
                            }
                          }
                          $('#label').val(label);
                          visEditor.setValue(code);
                        });

                        $('#new').click(function () {
                          visEditor.setValue('');
                          $('#label').val('Unlabeled');
                        });
                         
                        $('#save').click(function () {
                          if ($('#label').val().length === 0) {
                            alert('Please add a label');
                            return;
                          }
                          var data = {
                            label: $('#label').val(),
                            code: visEditor.getValue()
                          };
                          $.ajax({
                            type: 'POST',
                            url: '/save/' + query._id,
                            data: JSON.stringify(data),
                            dataType: 'json',
                            contentType: 'application/json',
                          }).done(function () { alert('Saved!'); })
                            .fail(function () { alert('Failed to save!'); });
                        });
                       });"]]]]
         [:div#debug.tab-pane 
          [:div#debug-editor [:pre (chesire/generate-string query {:pretty true})]]]]])))

(defn report-route
  "Renders a specified report"
  [request id]
  ; TODO
  (apply str (get-query-list request)))

(defn save-route
  "Saves a labelled visualization"
  [request id]
  (let [parsed (chesire/decode (slurp (:body request)) true)
        label (:label parsed)
        code (:code parsed)
        entry (collections/find-one-as-map db "visualizations" {:query_id (ObjectId. id)})
        new? (not (some #(= (:label %1) label) (:choices entry)))]
    (if entry
      ; Existing Entry
      (if new?
        ; New Label
        (do (collections/update db "visualizations" 
                                {:query_id (ObjectId. id)},
                                {$push {:choices {:label label,
                                                  :code code}}})
            {:status 200 :body "{}"}) ; Must return {} for Jquery not to fail.
        ; Existing Label
        (do (collections/update db "visualizations" 
                                {:query_id (ObjectId. id),
                                 "choices.label" label} 
                                {$set {"choices.$" {:label label,
                                                    :code code}}})
            {:status 200 :body "{}"}))
      ; New Entry
      (do (collections/insert db "visualizations" {:query_id (ObjectId. id),
                                                   :choices [{:label label,
                                                              :code code}]})
          {:status 200 :body "{}"}))))

(defroutes app
  "The router."
  (GET "/" [:as request] (home-route request))
  (GET "/dashboard" [:as request] (dashboard-route request))
  (GET "/view/:id" [id :as request] (view-route request id))
  (GET "/report/:id" [id :as request] (report-route request id))
  (POST "/save/:id" [id :as request] (save-route request id))
  (route/resources "/assets/")
  (route/not-found "<pre>Page not found</pre>"))

(defn -main
  "Start the server"
  [& args]
  ; Start the server.
  (web/run app :port 8080))
