(defproject yakutsk "0.1.0-SNAPSHOT"
  :dependencies [
    [org.clojure/clojure        "1.9.0"]
    [clojure.java-time          "0.3.2"]
    [org.immutant/web           "2.1.10"]
    [ring/ring-core             "1.7.0-RC1"]
    [compojure                  "1.6.1"]
    [cheshire                   "5.8.0"]
    [clj-http                   "3.9.1"]
    [rum                        "0.11.2"]
  ]
  :main yakutsk.server
  :profiles {
    :uberjar {
      :aot [yakutsk.server]
      :uberjar-name "yakutsk.jar"
    }
  })
