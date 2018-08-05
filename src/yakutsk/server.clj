(ns yakutsk.server
  (:require
    [rum.core :as rum]
    [immutant.web :as web]
    [compojure.core :as cj]
    [compojure.route :as cjr])
  (:import
    [org.joda.time DateTime]
    [org.joda.time.format DateTimeFormat]))


(def movies
  [{ :id        "1"
     :watched   #inst "2018-07-28"
     :released  #inst "2018-07-26"
     :title      "Миссия Невыполнима: Последствия"
     :rate      4 }
   { :id        "2"
     :watched   #inst "2018-07-26"
     :released  #inst "2017-10-20"
     :title      "Назови меня своим именем"
     :rate      3 }])


(def styles "
  * {
    box-sizing: border-box;
  }
  body {
    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Helvetica, Arial, sans-serif, 'Apple Color Emoji', 'Segoe UI Emoji', 'Segoe UI Symbol';
    margin: 0;
  }
  header {
    padding: 1.5rem;
  }
  header h1 {
    margin: 0;
  }
  header p {
    margin-top: 0.5rem;
    margin-bottom: 0;
  }
  section {
    padding: 1.5rem;
  }
")


(def date-formatter (DateTimeFormat/forPattern "dd.MM.YYYY"))
(def date-year-formatter (DateTimeFormat/forPattern "YYYY"))


(defn render-date [inst]
  (.print date-formatter (DateTime. inst)))

(defn render-date-year [inst]
  (.print date-year-formatter (DateTime. inst)))


(rum/defc movie [movie]
  [:.movie
    [:span (render-date (:watched movie))]
    [:h2 (:title movie)]
    [:p "Оценка: " (:rate movie)]
    [:p "Год релиза: " (render-date-year (:released movie))]])


(rum/defc page [title & children]
  [:html
    [:head
      [:meta { :charset "utf-8" }]
      [:title "Сайт Саши Мансурова"]
      [:meta { :name "viewport" :content "initial-scale=1.0, width=device-width" }]
      [:style { :dangerouslySetInnerHTML { :__html styles } }]]
    [:body
      [:header
        [:h1 title]
        [:p "Фронтенд разработчик в Рокетбанке"]]
      [:main children]]])


(rum/defc index [movies]
  (page "Саша Мансуров"
    [:section
      (for [m movies]
        (movie m))]))


(defn render-html [component]
  (str "<!DOCTYPE html>" (rum/render-static-markup component)))


(cj/defroutes routes
  (cj/GET "/" [:as req]
    { :body (render-html (index movies)) })

  (cj/GET "/write" [:as req]
    { :body "WRITE" })

  (cj/POST "/write" [:as req]
    { :body "POST" }))


(defn with-headers [handler headers]
  (fn [request]
    (some-> (handler request)
      (update :headers merge headers))))


(def app
  (-> routes
    (with-headers { "Content-Type"  "text/html; charset=utf-8"
                    "Cache-Control" "no-cache"
                    "Expires"       "-1" })))


(defn -main [& args]
  (let [args-map (apply array-map args)
        port-str (or (get args-map "-p")
                     (get args-map "--port")
                     "8080")]
    (web/run #'app { :port (Integer/parseInt port-str) })))


(comment
  (def server (-main "--port" "8080"))
  (web/stop server))
