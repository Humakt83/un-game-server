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

(def players (atom {:player1 {:game nil :channel nil}}))

(defn get-players [] @players)

(defn filter-out-channels [players]
  (apply merge (map #(hash-map (first %) {:game (:game (second %))}) players)))

(defn register-player [player-name channel]
  (if (= nil ((keyword player-name) (get-players)))
    (swap! players assoc (keyword player-name) {:game nil :channel channel})
    (throw (Exception. "Player already exists"))))

;; Converts JSON objects clojure hashmaps with keys as keywords
(defn from-json [data] (json/read-str data :key-fn keyword))

;; Converts clojure hashmaps to correct JSON
(defn to-json [data] (json/write-str data))

(defn player-list [players-data]
  {:msgType "playerList" :content (filter-out-channels players-data)})

(defn text-response [data]
  {:msgType "text" :content data})

(defn error-msg [e]
  {:msgType "error" :content (.getMessage e)})

(defn handle-message [{message-type :msgType
                       content :content}
                      channel]
  (println "Attempting to handle message of type: " message-type " and content: " content)
  (try
    (case message-type
      "getplayers" (to-json (player-list (get-players)))
      "registerplayer" (to-json (player-list (register-player (:playerName content) channel)))
      "text" (to-json (text-response (apply str (reverse content))))
      (to-json (throw (Exception. (str "No message handler for message type " message-type)))))
    (catch Exception e (to-json (error-msg e)))))

(def websocket-callbacks
  "WebSocket callback functions"
  {:on-open   (fn [channel]
    (async/send! channel "Ready to reverse your messages!"))
  :on-close   (fn [channel {:keys [code reason]}]
    (println "close code:" code "reason:" reason))
  :on-message (fn [ch m]
    (println "Received message " m)
    (async/send! ch (handle-message (from-json m) ch)))})

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