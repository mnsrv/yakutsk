(ns yakutsk.server
  (:require
    [immutant.web :as web]))


(def app
  (fn [req]
    { :status 200
      :body (:uri req) }))


(defn -main [& args]
  (let [args-map (apply array-map args)
        port-str (or (get args-map "-p")
                     (get args-map "--port")
                     "8080")]
    (web/run #'app { :port (Integer/parseInt port-str) })))


(comment
  (def server (-main "--port" "8080"))
  (web/stop server))
