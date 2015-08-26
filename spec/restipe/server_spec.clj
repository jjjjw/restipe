(ns restipe.server-spec
  (:import
    [javax.servlet.http HttpServletResponse])
  (:require
    [clj-http.client :as client]
    [datomic.api :refer [db transact q]]
    [restipe.server :as server]
    [speclj.core :refer :all] speclj.run.standard
    [cheshire.core :refer [parse-string generate-string]]))

(defn get-recipe
  [uuid]
  (client/get (str "http://localhost:8080/recipes/" uuid)
    {:content-type :json :throw-exceptions false}))

(defn delete-recipe
  [uuid]
  (client/delete (str "http://localhost:8080/recipes/" uuid)
    {:content-type :json :throw-exceptions false}))

(defn get-recipes
  []
  (client/get "http://localhost:8080/recipes"
    {:content-type :json}))

(defn parse-response
  [response]
  (-> response :body parse-string (get "data")))

(defn post-pizza
  []
  (client/post "http://localhost:8080/recipes"
    {:content-type :json :body (generate-string {:name "pizza"})}))

(defn put-tacos
  [uuid]
  (client/put (str "http://localhost:8080/recipes/" uuid)
    {:content-type :json :body (generate-string {:name "tacos"}) :throw-exceptions false}))

(defn recipe-ids
  []
  (flatten (into [] (q '[:find ?c :where [?c :recipe/uuid]] (db server/db-connection)))))

(defn retract-entities
  [entity-ids]
  (transact server/db-connection (into [] (map (fn [id] [:db.fn/retractEntity id]) (recipe-ids)))))

(describe "integration"
  (with-all server (server/start-server))

  ;; Force the server to start up
  (before-all @server)

  ;; And shut it down at the very end
  (after-all
    ;; start-server returns a function to stop the server, invoke it after all characteristics
    ;; have executed.
    (@server))

  ;; Reset db after each test
  (after (retract-entities (recipe-ids)))

  (it "can get recipes (none)"

    (->> (get-recipes)
      parse-response
      (should= [])))

  (it "can post a recipe"

    (->> (post-pizza)
      :status
      (should= HttpServletResponse/SC_CREATED)))

  (it "validates post requests"

    (->> (client/post "http://localhost:8080/recipes"
            {:content-type :json :body (generate-string {:invalid "pizza"})
              :throw-exceptions false})
      :status
      (should= HttpServletResponse/SC_BAD_REQUEST)))

  (it "can get recipes (list)"

    (let [recipe (parse-response (post-pizza))]
      (->> (get-recipes)
        parse-response
        (should= [recipe]))))

  (it "can get recipe (none)"

    (->> (get-recipe "55d38dda-3b04-4115-90fb-a0a85f0a70ff")
      :status
      (should= HttpServletResponse/SC_NOT_FOUND)))

  (it "can get recipe (one)"

    (let [recipe (parse-response (post-pizza))]
      (->> (get-recipe (get recipe "uuid"))
        parse-response
        (should= recipe))))

  (it "can delete recipe (not found)"

    (->> (delete-recipe "55d38dda-3b04-4115-90fb-a0a85f0a70f")
      :status
      (should= HttpServletResponse/SC_NOT_FOUND)))

  (it "can delete recipe (one)"

    (let [recipe (parse-response (post-pizza))]
      (do
        (->> (delete-recipe (get recipe "uuid"))
          :status
          (should= HttpServletResponse/SC_NO_CONTENT))
        (->> (get-recipe (get recipe "uuid"))
          :status
          (should= HttpServletResponse/SC_NOT_FOUND)))))

  (it "can put recipe (not found)"

    (->> (put-tacos "55d38dda-3b04-4115-90fb-a0a85f0a70f")
      :status
      (should= HttpServletResponse/SC_NOT_FOUND)))

  (it "can put recipe (one)"

    (let [recipe (parse-response (post-pizza)) tacos (merge recipe {"name" "tacos"})]
      (do
        (->> (put-tacos (get recipe "uuid"))
          parse-response
          (should= tacos))
        (->> (get-recipe (get recipe "uuid"))
          parse-response
          (should= tacos))))))

(run-specs)
