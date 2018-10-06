(ns yakutsk.server
  (:require
    [rum.core :as rum]
    [ring.util.response]
    [clojure.edn :as edn]
    [immutant.web :as web]
    [ring.middleware.params]
    [compojure.core :as compojure]
    [clojure.java.io :as io])
  (:import
    [java.util UUID]
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


(defn render-date [inst]
  (.print date-formatter (DateTime. inst)))


(rum/defc post [post]
  [:.post
    (for [name (:pictures post)]
      [:img.post__image { :src (str "/post/" (:id post) "/" name) }])
    [:p (:body post)]
    [:p [:a {:href (str "/post/" (:id post))} (render-date (:created post))]]])


  (rum/defc page [opts & children]
    (let [{:keys [index?]
           :or {index? false}} opts]
      [:html
        [:head
          [:meta { :charset "utf-8" }]
          [:title "mansurov.me"]
          [:meta { :name "viewport" :content "initial-scale=1.0, width=device-width" }]
          [:style { :dangerouslySetInnerHTML { :__html styles } }]]
        [:body
          [:header
            (if index?
              [:h1 "mansurov.me"]
              [:h1 [:a {:href "/"} "mansurov.me"]])]
          [:main children]]]))


(defn safe-slurp [source]
  (try
    (slurp source)
    (catch Exception e
      nil)))


(defn get-post [post-id]
  (let [path (str "posts/" post-id "/post.edn")]
    (some-> (io/file path)
            (safe-slurp)
            (edn/read-string))))


(defn next-post-id []
  (let [uuid     (UUID/randomUUID)
        time     (int (/ (System/currentTimeMillis) 1000))
        high     (.getMostSignificantBits uuid)
        low      (.getLeastSignificantBits uuid)
        new-high (bit-or (bit-and high 0x00000000FFFFFFFF)
                         (bit-shift-left time 32))]
    (str (UUID. new-high low))))


(defn save-post! [post]
  (let [dir (io/file (str "posts/" (:id post)))]
    (.mkdir dir)
    (spit (io/file dir "post.edn") (pr-str post))))


(rum/defc index-page [post-ids]
  (page {:index? true}
    (for [post-id post-ids]
      (post (get-post post-id)))))


(rum/defc post-page [post-id]
  (page {}
    (post (get-post post-id))))


(rum/defc edit-post-page [post-id]
  (let [post    (get-post post-id)
        create? (nil? post)]
    (page {:title (if create? "Создание" "Редактирование")}
      [:form { :action (str "/post/" post-id "/edit")
               :method "post" }
        [:textarea.edit_post__body
          { :value (:body post "")
            :name "body"
            :placeholder "Пиши сюда..." }]
        [:input.edit_post__submit
          { :type "submit"
            :value (if create? "Создать" "Сохранить") }]])))


(defn render-html [component]
  (str "<!DOCTYPE html>" (rum/render-static-markup component)))


(defn post-ids []
  (for [name (seq (.list (io/file "posts")))
        :let [child (io/file "posts" name)]
        :when (.isDirectory child)]
    name))


(compojure/defroutes routes
  (compojure/GET "/" [:as req]
    { :body (render-html (index-page (post-ids))) })

  (compojure/GET "/post/new" []
    { :status 303
      :headers { "Location" (str "/post/" (next-post-id) "/edit") }})

  (compojure/GET "/post/:post-id" [post-id]
    { :body (render-html (post-page post-id)) })

  (compojure/GET "/post/:post-id/:img" [post-id img]
    (ring.util.response/file-response (str "posts/" post-id "/" img)))

  (compojure/GET "/post/:post-id/edit" [post-id]
    { :body (render-html (edit-post-page post-id)) })

  (compojure/POST "/post/:post-id/edit" [post-id :as req]
    (let [params (:form-params req)
          body   (get params "body")]
      (save-post! { :id post-id
                    :body body })
      { :status 302
        :headers { "Location" (str "/post/" post-id) }}))

  (fn [req]
    { :status 404
      :body "404 Not found" }))


(defn with-headers [handler headers]
  (fn [request]
    (some-> (handler request)
      (update :headers merge headers))))


(def app
  (-> routes
    (ring.middleware.params/wrap-params)
    (with-headers { "Content-Type"  "text/html; charset=utf-8"
                    "Cache-Control" "no-cache"
                    "Expires"       "-1" })))


(defn -main [& args]
  (let [args-map (apply array-map args)
        port-str (or (get args-map "-p")
                     (get args-map "--port")
                     "8000")]
    (println "Starting web server on port" port-str)
    (web/run #'app { :port (Integer/parseInt port-str) })))


(comment
  (def server (-main "--port" "8080"))
  (web/stop server))
