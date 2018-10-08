(ns yakutsk.server
  (:require
    [rum.core :as rum]
    [ring.util.response]
    [clojure.edn :as edn]
    [immutant.web :as web]
    [ring.middleware.params]
    [compojure.core :as compojure]
    [ring.middleware.multipart-params]
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


(re-matches #".*(\.[^.]+)" "sash.a.p-ng")


(defn save-post! [post pictures]
  (let [dir           (io/file (str "posts/" (:id post)))
        picture-names (for [[picture idx] (map vector pictures (range))
                            :let [in-name  (:filename picture)
                                  [_ ext]  (re-matches #".*(\.[^\.]+)" in-name)]]
                        (str (:id post) "_" (inc idx) ext))]
    (.mkdir dir)
    (doseq [[picture name] (map vector pictures picture-names)]
      (io/copy (:tempfile picture) (io/file dir name))
      (.delete (:tempfile picture)))
    (spit (io/file dir "post.edn") (pr-str (assoc post :pictures (vec picture-names))))))


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
               :enc-type "multipart/form-data"
               :method "post" }
        [:.edit_post__picture
          [:input { :type "file" :name "picture" }]]
        [:.edit_post__body
          [:textarea
            { :value (:body post "")
              :name "body"
              :placeholder "Пиши сюда..." }]]
        [:.edit_post__submit
          [:button
            (if create? "Создать" "Сохранить") ]]])))


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

  (ring.middleware.multipart-params/wrap-multipart-params
    (compojure/POST "/post/:post-id/edit" [post-id :as req]
      (let [params  (:multipart-params req)
            body    (get params "body")
            picture (get params "picture")]
          ;; fixme pictures not empty if empty
        (save-post! { :id post-id
                      :body body }
                    [picture])
        { :status 302
          :headers { "Location" (str "/post/" post-id) }})))

  (fn [req]
    { :status 404
      :body "404 Not found" }))


(defn with-headers [handler headers]
  (fn [request]
    (some-> (handler request)
      (update :headers merge headers))))


(defn print-errors [handler]
  (fn [req]
    (try
      (handler req)
      (catch Exception e
        (.printStackTrace e)
        { :status 500
          :headers { "Content-Type" "text/html; charset=utf-8" }
          :body (with-out-str
                  (clojure.stacktrace/print-stack-trace (clojure.stacktrace/root-cause e))) }))))


(def app
  (-> routes
    (ring.middleware.params/wrap-params)
    (with-headers { "Content-Type"  "text/html; charset=utf-8"
                    "Cache-Control" "no-cache"
                    "Expires"       "-1" })
    (print-errors)))


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
