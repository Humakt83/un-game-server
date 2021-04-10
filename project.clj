(defproject un-game-server "0.1.0-SNAPSHOT"
:dependencies [[org.clojure/clojure "1.10.0"]
               [org.immutant/web "2.1.10"]
               [compojure "1.6.2"]
               [ring/ring-core "1.9.1"]
               [environ "1.2.0"]
               [org.clojure/data.json "2.0.2"]]
  :main un-game-server.core
  :uberjar-name "un-game-server.jar"
  :profiles {:uberjar {:aot [un-game-server.core]}}
  :min-lein-version "2.4.0")
