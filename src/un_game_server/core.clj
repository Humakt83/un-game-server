(ns un-game-server.core
  (:require
   [immutant.web             :as web]
   [immutant.web.async       :as async]
   [immutant.web.middleware  :as web-middleware]
   [compojure.route          :as route]
   [environ.core             :refer (env)]
   [compojure.core           :refer (ANY GET defroutes)]
   [ring.util.response       :refer (response redirect content-type)]
   [clojure.data.json        :as json])
  (:gen-class))

(def players (atom {:player1 {:name "Player 1" :game-id "1234"}}))

(defn get-players [] @players)

;; Converts JSON objects clojure hashmaps with keys as keywords
(defn from-json [data] (json/read-str data :key-fn keyword))

;; Converts clojure hashmaps to correct JSON
(defn to-json [data] (json/write-str data :key-fn name))

(defn handle-message [{message-type :msgType
                       content :content}]
  (println "Attempting to handle message of type: " message-type " and content: " content)
  (case message-type
      "getplayers" (to-json (get-players))
      "text" (apply str (reverse content))))

(def websocket-callbacks
  "WebSocket callback functions"
  {:on-open   (fn [channel]
    (async/send! channel "Ready to reverse your messages!"))
  :on-close   (fn [channel {:keys [code reason]}]
    (println "close code:" code "reason:" reason))
  :on-message (fn [ch m]
    (println "Received message " m)
    (async/send! ch (handle-message (from-json m))))})

(defroutes routes
  (GET "/" {c :context} (redirect (str c "/index.html")))
  (route/resources "/"))

(defn -main [& {:as args}]
  (web/run
    (-> routes
      (web-middleware/wrap-session {:timeout 20})
      ;; wrap the handler with websocket support
      ;; websocket requests will go to the callbacks, ring requests to the handler
      (web-middleware/wrap-websocket websocket-callbacks))
      (merge {"host" (env :demo-web-host), "port" (env :demo-web-port)}
      args)))