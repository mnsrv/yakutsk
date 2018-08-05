(ns yakutsk.server
  (:require
    [rum.core :as rum]
    [immutant.web :as web]
    [compojure.core :as cj]
    [compojure.route :as cjr]))


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


(def styles
  "body {
    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Helvetica, Arial, sans-serif, 'Apple Color Emoji', 'Segoe UI Emoji', 'Segoe UI Symbol';
  }")


(rum/defc movie [movie]
  [:.movie
    [:span (:watched movie)]
    [:h2 (:title movie)]
    [:p "Оценка: " (:rate movie)]
    [:p "Дата релиза: " (:released movie)]])


(rum/defc page [title & children]
  [:html
    [:head
      [:meta { :charset "utf-8" }]
      [:title title]
      [:meta { :name "viewport" :content "initial-scale=1.0, width=device-width" }]
      [:style { :dangerouslySetInnerHTML { :__html styles } }]]
    [:body
      [:header
        [:h1 title]
        [:p "Фронтенд разработчик в " [:a { :href "https://rocketbank.ru/loves/sasha" :target "_blank" } "Рокетбанке"]]]
      children]])


(rum/defc index [movies]
  (page "Сашин Дом"
    (for [m movies]
      (movie m))))


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
