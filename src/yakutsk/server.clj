(ns yakutsk.server
  (:require
    [immutant.web :as web]
    [compojure.core :as cj]
    [compojure.route :as cjr]))


(cj/defroutes routes
  (cj/GET "/" [:as req]
    { :body "INDEX" })

  (cj/GET "/write" [:as req]
    { :body "WRITE" })

  (cj/POST "/write" [:as req]
    { :body "POST" }))


(def app routes)


(defn -main [& args]
  (let [args-map (apply array-map args)
        port-str (or (get args-map "-p")
                     (get args-map "--port")
                     "8080")]
    (web/run #'app { :port (Integer/parseInt port-str) })))


(comment
  (def server (-main "--port" "8080"))
  (web/stop server))
