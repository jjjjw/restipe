(ns restipe.server
  (:gen-class)
  (:import
    [org.eclipse.jetty.server Server])
  (:require
    [clojure.tools.logging :as log]
    [datomic.api :refer [db create-database connect transact delete-database]]
    [io.aviso.rook :as rook]
    [io.aviso.rook.response-validation :as response-validation]
    [io.aviso.rook.schema-validation :as schema-validation]
    [io.aviso.rook.server :as server]
    [ring.adapter.jetty :as jetty]))

(def port 8080)

(def db-uri "datomic:mem://restipe")

(def created-db (create-database db-uri))

(def db-connection (connect db-uri))

(def recipe-schema (read-string (slurp "src/restipe/schemas/recipe.edn")))

(def schema-transaction (transact db-connection recipe-schema))

(defn handler
  "Configures the rook app."
  []
  (-> (rook/namespace-handler
      ["recipes" 'restipe.resources.recipes schema-validation/wrap-with-schema-validation])
    (rook/wrap-with-injections {:db-connection db-connection})
    rook/wrap-with-standard-middleware))

(defn start-server
  "Starts a server and returns a function that shuts it back down."
  []
  (let [^Server server (jetty/run-jetty (handler) {:port port :join? false})]
    (log/infof "Listening on port %d." port)
    #(.stop server)))

(defn -main
  "Main."
  []
  (start-server))
