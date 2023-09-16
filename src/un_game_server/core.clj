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

(def players (atom {}))

(defn get-players [] @players)

(defn filter-out-channels [players]
  (apply merge (map #(hash-map (first %) {:gameKey (:gameKey (second %))}) players)))

(defn register-player [player-name channel]
  (if (= nil ((keyword player-name) (get-players)))
    (swap! players assoc (keyword player-name) {:gameKey nil :channel channel})
    (throw (Exception. "Player already exists"))))

(defn get-player [channel]
  (->> (get-players)
       (filter #(= channel (:channel (second %))))
       (map #(first %))
       first))

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

(defn in? 
  "true if coll contains elm"
  [coll elm]  
  (some #(= elm %) coll))

(defn start-game [{player-names :players
                   game-key :key}]
  (let [game-only-players (filter #(in? player-names (name (first %))) (get-players))]
    (doseq [player-info game-only-players]
      (println "Send game key to all players involved in game..." player-info)
      (let [updated-state (swap! players assoc-in [(first player-info) :gameKey] game-key)
            asd (println "Updated state " updated-state)
            msg-to-players (to-json {:msgType "startgame" :content game-key})
            asdasd (println "The message " msg-to-players)
            channel (:channel (second player-info))]
        (async/send! channel msg-to-players)))))

(defn pickgame [game-key player-name]
  (let [player ((keyword player-name) (get-players))]
    (println player)
    (swap! players update-in [(keyword player-name)] merge {:gameKey game-key})))

(defn handle-message [{message-type :msgType
                       content :content}
                      channel]
  (println "Attempting to handle message of type: " message-type " and content: " content)
  (try
    (let [player (get-player channel)
          from (if player
                 (str player ": ")
                 "")]
      (case message-type
        "registerplayer" (to-json (player-list (register-player (:playerName content) channel)))
        "startgame" (start-game content)
        "pickgame" (to-json (player-list (pickgame (:gameKey content) (:playerName content))))
        "text" (to-json (text-response (apply str from content)))
        (to-json (throw (Exception. (str "No message handler for message type " message-type))))))
    (catch Exception e (to-json (error-msg e)))))

(defn send-to-all [message]
  (doseq [player-info (get-players)]
    (println player-info)
    (let [channel (:channel (second player-info))]
           (async/send! channel message))))

(def websocket-callbacks
  "WebSocket callback functions"
  {:on-open   (fn [channel]
    (async/send! channel "Ready to process your messages!"))
  :on-close   (fn [channel {:keys [code reason]}]
    (println "close code:" code "reason:" reason))
  :on-message (fn [ch m]
    (let [res (handle-message (from-json m) ch)]
      (println res)
      (send-to-all res)))})

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