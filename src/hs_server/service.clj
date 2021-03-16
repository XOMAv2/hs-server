(ns hs-server.service
  (:require [hs-server.specs :as ss]
            [hs-server.helpers :as help]
            [hs-server.repository :as repo]))

(defn get-all-users
  [db]
  (try (let [users (repo/get-all-users db)]
         {:status 200 :body users})
       (catch Exception e
         {:status 500 :body {:message (str "Ошибка при обращении к базе данных.\n"
                                           (ex-message e))}})))

(defn add-user
  [[db options] user]
  (help/if-let-conform
   [user [::ss/user-form user]]
   (try (let [user (repo/add-user db user)
              uri (:service-uri options)]
          {:status 201 :body user :headers {"Location" (str uri
                                                            (when (not= (last uri) \/) "/")
                                                            "api/v1/users/"
                                                            (:id user))}})
        (catch Exception e
          {:status 500 :body {:message (str "Ошибка при обращении к базе данных.\n"
                                            (ex-message e))}}))
   {:status 422 :body {:message (help/beautiful-spec-explain ::ss/user-form
                                                             user)}}))

(defn get-user-by-id
  [db id]
  (try (let [user (repo/get-user-by-id db id)]
         (if (nil? user)
           {:status 404 :body {:message (str "Пользователь с id " id " не найден.")}}
           {:status 200 :body user}))
       (catch Exception e
         {:status 500 :body {:message (str "Ошибка при обращении к базе данных.\n"
                                           (ex-message e))}})))

(defn delete-user
  [db id]
  (try (let [user (repo/delete-user db id)]
         (if (nil? user)
           {:status 404 :body {:message (str "Пользователь с id " id " не найден.")}}
           {:status 200 :body user}))
       (catch Exception e
         {:status 500 :body {:message (str "Ошибка при обращении к базе данных.\n"
                                           (ex-message e))}})))

(def ^:private user-form-map (help/spec->map ::ss/user-form))

(defn update-user
  [db id partial-user]
  (help/if-let-conform
   [partial-user [user-form-map partial-user]]
   (try (let [user (repo/update-user db id partial-user)]
          (if (nil? user)
            {:status 404 :body {:message (str "Пользователь с id " id " не найден.")}}
            {:status 200 :body user}))
        (catch Exception e
          {:status 500 :body {:message (str "Ошибка при обращении к базе данных.\n"
                                            (ex-message e))}}))
   {:status 422 :body {:message (help/beautiful-spec-explain ::ss/user-form
                                                             partial-user)}}))