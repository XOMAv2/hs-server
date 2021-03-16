(ns hs-server.repository
  (:require [next.jdbc :as jdbc]
            [next.jdbc.date-time] ; Для приведения java.util.Date в тип данных SQL.
            [next.jdbc.sql :as sql]
            [next.jdbc.result-set :as rs]
            [hs-server.specs :as ss]
            [camel-snake-kebab.core :as csk]
            [hs-server.helpers :as help]))

(def ^:private table :users)

(def ^:private opts {:builder-fn rs/as-unqualified-kebab-maps
                     :return-keys true})

(def ^:private user-form-keys (help/spec->keys ::ss/user-form))

(def ^:private user-form-map (help/spec->map ::ss/user-form))

(defn get-all-users
  "Если пользователей нет, то будет возвращён пустой вектор, иначе вектор мап."
  [db]
  (jdbc/execute! db [(format "SELECT * FROM %s" (name table))] opts))

(defn get-user-by-id
  "Если пользователь с указанным id не найден, то будет возвращён nil, иначе мапа с пользователем."
  [db id]
  (sql/get-by-id db table id :id opts))

(defn add-user
  "Если удалось добавить пользователя, то будет возвращена мапа с этим пользователем, иначе
   исключение."
  [db user]
  (help/if-let-conform
   [user [::ss/user-form user]]
   (let [user (select-keys user user-form-keys)
         user (help/change-keys-style user csk/->snake_case)]
     (sql/insert! db table user opts))
   (throw (IllegalArgumentException.
           (str "Аргумент " (name 'user) " не удовлетворяет спецификации user-form.")))))

(defn delete-user
  "Если пользователь с указанным id не найден, то будет возвращён nil, иначе мапа с удалённым
   пользователем."
  [db id]
  (sql/delete! db table {:id id} opts))

(defn update-user
  "Если пользователь с указанным id не найден, то будет возвращён nil, иначе мапа с обновлённым
   пользователем."
  [db id user]
  (help/if-let-conform
   [user [user-form-map user]]
   (let [user (select-keys user user-form-keys)
         user (help/change-keys-style user csk/->snake_case)]
     (sql/update! db table user {:id id} opts))
   (throw (IllegalArgumentException.
           (str "Аргумент " (name 'user) " не удовлетворяет спецификации user-form.")))))