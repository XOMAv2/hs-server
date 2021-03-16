(ns hs-server.router
  (:require [compojure.core :refer [GET POST PATCH DELETE
                                    context routes]]
            [compojure.coercions :refer [as-int]]
            [clojure.string]
            [hs-server.service :as service]))

(defn not-found
  [{:keys [request-method uri]}]
  {:status 404
   :body {:message (format "Не удалось выполнить действие по запросу %s %s"
                           (clojure.string/upper-case (name request-method))
                           uri)}})

(def router
  (routes (context "/api/v1/users" {{:keys [db options]} :components}
            (GET "/" [] (service/get-all-users db))
            (POST "/" {:keys [body]} (service/add-user [db options] body))
            (context "/:id" [id :<< as-int]
              (GET "/" [] (service/get-user-by-id db id))
              (DELETE "/" [] (service/delete-user db id))
              (PATCH "/" {:keys [body]} (service/update-user db id body))))
          not-found))