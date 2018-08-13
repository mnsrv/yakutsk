(ns yakutsk.server
  (:require
    [rum.core :as rum]
    [java-time :as time]
    [immutant.web :as web]
    [clojure.java.io :as io]
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
(def lastDay (time/adjust now :last-day-of-month))
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
            (- 7 (time/value (time/day-of-week firstDay))))
         weeks []]
    (if (<= start daysInMonth)
      (recur
        (inc end)
        (if (> (+ 7 end) daysInMonth)
          daysInMonth
          (+ 7 end))
        (for [x (range 7)
              :let [y (* x 3)]
              :when (even? y)]
          y)
      )
      weeks)))
month

(time/value (time/day-of-week firstDay))
(time/year-month now)
(time/value (time/day-of-week (time/adjust now :first-day-of-month)))
(time/period 1 :months)
(time/year-month)
(time/max-value (time/month))
(time/day-of-week)
(Integer/parseInt (time/format "F" (time/adjust now :first-day-of-month)))
[[#inst "2018-08-12" 2] [3 4]]
(apply mapv vector [[1 2] [3 4]])
(vector [1 2] [3 4])


(def calendar
  [:section
    [:h2 (nth localeMonths (dec (Integer/parseInt (time/format "M" now))))]
    [:.calendar
      (for [week [[0 0 1 2 3 4 5]
                  [6 7 8 9 10 11 12]
                  [13 14 15 16 17 18 19]
                  [20 21 22 23 24 25 26]
                  [27 28 29 30 31 0 0]]]
        [:.calendar__row
          (for [day week]
            [:.calendar__cell (when (> day 0) day)])])]])


(rum/defc index [movies]
  (page "Саша Мансуров" calendar))


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
