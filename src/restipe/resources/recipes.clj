(ns restipe.resources.recipes
  (:import
    [javax.servlet.http HttpServletResponse])
  (:require
    [clojure.set :refer [map-invert rename-keys]]
    [datomic.api :refer [db transact tempid q squuid]]
    [io.aviso.rook.schema :as schema]
    [ring.util.response :as res]
    [schema.core :as s]))

(schema/defschema RecipeParameters
  {(s/optional-key :name) s/Str
   (s/optional-key :ingredients) [s/Str]
   (s/optional-key :instructions) [s/Str]})

(def recipe-datom
  {:db/id :id
  :recipe/ingredient :ingredient
  :recipe/instructions :instructions
  :recipe/name :name
  :recipe/uuid :uuid})

(defn apply-recipe-ns
  [obj]
  (rename-keys obj (map-invert recipe-datom)))

(defn retract-entity
  [entity-id db-connection]
  (transact db-connection [[:db.fn/retractEntity entity-id]]))

(defn remove-recipe-ns
  [obj]
  (rename-keys obj recipe-datom))

(defn remove-id
  [obj]
  (dissoc obj :id :db/id))

(defn new-recipe
  []
  {:db/id (tempid :recipes) :recipe/uuid (squuid)})

(defn pull-recipe
  [uuid db]
  (ffirst (q '[
    :find (pull ?c [*])
    :in $ ?recipe-uuid
    :where [?c :recipe/uuid ?recipe-uuid]] db (java.util.UUID/fromString uuid))))

(defn pull-recipes
  [db]
  (flatten (q '[:find (pull ?c [*]) :where [?c :recipe/uuid]] db)))

(defn adapt-recipe
  [obj]
  (-> obj remove-recipe-ns remove-id))

(defn index
  "GET /recipes

  Lists all recipes."
  [^:injection db-connection]
  (res/response {:data (map adapt-recipe (pull-recipes (db db-connection)))}))

(defn show
  "GET /recipes/:id

  Lists all recipes."
  [^:injection db-connection id]
  (let [recipe (pull-recipe id (db db-connection))]
    (if (some? recipe)
      (res/response {:data (adapt-recipe recipe)})
      (res/not-found {:message (format "No recipe '%s'." id)}))))

(defn create
  "POST /recipes

  Create a recipe"
  {:body-schema RecipeParameters}
  [^:injection db-connection
    {:keys (vals recipe-datom) :as params}]
  (let [recipe (merge (new-recipe) (apply-recipe-ns params))]
    (transact db-connection [recipe])
    (-> (res/response {:data (adapt-recipe recipe)})
        (res/status HttpServletResponse/SC_CREATED))))

(defn destroy
  "DELETE /recipes/:id

  Delete a recipe"
  [^:injection db-connection id]
  (let [recipe (pull-recipe id (db db-connection))]
    (if (some? recipe)
      (do (retract-entity (:db/id recipe) db-connection)
        (-> (res/response {:data []})
          (res/status HttpServletResponse/SC_NO_CONTENT)))
      (res/not-found {:message (format "No recipe '%s'." id)}))))

(defn change
  "PUT /recipes

  Update a recipe"
  {:body-schema RecipeParameters}
  [^:injection db-connection id
    {:keys (vals recipe-datom) :as params}]
  (let [recipe (pull-recipe id (db db-connection))]
    (if (some? recipe)
      (do (transact db-connection [(merge recipe (apply-recipe-ns params))])
        (res/response {:data (adapt-recipe (merge (remove-recipe-ns recipe) params))}))
      (res/not-found {:message (format "No recipe '%s'." id)}))))
