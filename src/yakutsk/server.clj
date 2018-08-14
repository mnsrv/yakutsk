(ns yakutsk.server
  (:require
    [rum.core :as rum]
    [java-time :as time]
    [immutant.web :as web]
    [clojure.java.io :as io]
    [clj-http.client :as http]
    [compojure.core :as compojure])
  (:import
    [org.joda.time DateTime]
    [org.joda.time.format DateTimeFormat])
  (:gen-class))


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


(def styles (slurp (io/resource "style.css")))

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

(def now (time/local-date))
(def firstDay (time/adjust now :first-day-of-month))
(def daysInMonth (time/max-value (time/property now :day-of-month)))
(def localeMonths
  ["Январь", "Февраль", "Март",
   "Апрель", "Май", "Июнь",
   "Июль", "Август", "Сентябрь",
   "Октябрь", "Ноябрь", "Декабрь"])
(def month
  (loop [start 1
         end (if (time/sunday? firstDay)
            1
            (- 8 (time/as firstDay :day-of-week)))
         weeks []]
    (if (<= start daysInMonth)
      (recur
        (inc end)
        (if (> (+ 7 end) daysInMonth)
          daysInMonth
          (+ 7 end))
        (conj weeks (vec (for [i (range 7)
              :let [x (cond
                (= 6 (- end start)) (+ start i)
                (= 1 start) (if (> (+ end i -6) 0) (+ end i -6) 0)
                :else (if (<= (+ start i) end)
                  (+ start i)
                  0))]]
          x))))
      weeks)))


(def calendar
  [:section
    [:h2 (nth localeMonths (dec (time/as now :month-of-year)))]
    [:.calendar
      (for [week month]
        [:.calendar__row
          (for [day week]
            [:div { :class ["calendar__cell" (when (= day (time/as now :day-of-month)) "calendar__cell_today")] }
              (when (> day 0) day)])])]])


(def weather
  (try
    [:section
      [:h2 "Погода в Москве"]
      [:.weather
        (Math/round
          (:temperature
            (:body
              (http/get "https://api.mansurov.me/weather" { :as :json }))))
        "°"]]
    (catch Exception e
      (println "Weather request failed:"))))


(rum/defc index [movies]
  (page "Саша Мансуров" calendar weather))


(defn render-html [component]
  (str "<!DOCTYPE html>" (rum/render-static-markup component)))


(compojure/defroutes routes
  (compojure/GET "/" [:as req]
    { :body (render-html (index movies)) })

  (compojure/GET "/write" [:as req]
    { :body "WRITE" })

  (compojure/POST "/write" [:as req]
    { :body "POST" })
  (fn [req]
    { :status 404
      :body "404 Not found" }))


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
    (println "Starting web server on port" port-str)
    (web/run #'app { :port (Integer/parseInt port-str) })))


(comment
  (def server (-main "--port" "8080"))
  (web/stop server))
