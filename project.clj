(defproject visualizer "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "MPL"
            :url "http://choosealicense.com/licenses/mpl-2.0/"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.immutant/immutant "2.0.0-alpha1"]
                 [compojure "1.1.8"]
                 [cider/cider-nrepl "0.7.0-SNAPSHOT"]
                 [clj-http "0.9.2"]
                 [hiccup "1.0.5"]
                 [com.novemberain/monger "2.0.0" :exclusions [com.google.guava/guava]]
                 [com.google.guava/guava "16.0.1"]
                 [cheshire "5.3.1"]]
  :plugins [[lein-immutant "2.0.0-alpha1"]]
  :main ^:skip-aot visualizer.core 
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}}
  :repl-options {:nrepl-middleware
                 [cider.nrepl.middleware.classpath/wrap-classpath
                  cider.nrepl.middleware.complete/wrap-complete
                  cider.nrepl.middleware.info/wrap-info
                  cider.nrepl.middleware.inspect/wrap-inspect
                  cider.nrepl.middleware.macroexpand/wrap-macroexpand
                  cider.nrepl.middleware.stacktrace/wrap-stacktrace
                  cider.nrepl.middleware.trace/wrap-trace]}
  ; Plugin configuration.
  :immutant {
     :war {
        :dev? false
        :resource-paths ["resources"]
        :nrepl {
          :interface "0.0.0.0" ; Don't do this in production, rather obviously.
          :port 8888
          :start? true}}})
