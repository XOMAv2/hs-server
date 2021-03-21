(ns hs-server.core
  (:require [ring.adapter.jetty :refer [run-jetty]]
            [hs-server.helpers :as help]
            [integrant.core :as ig]
            [hs-server.middlewares :as md]
            [ring.middleware.json :as ring]
            [camel-snake-kebab.core :as csk]
            [hs-server.router :refer [router]]
            [signal.handler :refer [with-handler]]
            [clojure.tools.logging :as log])
  (:gen-class))

; TODO: подумать об идемпотентности компонентов.
; TODO: pool соединений.
; TODO: опциональный компонент для создания таблицы и загрузки данных по умполчанию.
; TODO: логирование.
; TODO: спеки для словарей options для компонентов.

; Система и функции для работы с ней.
(defonce ^:private system nil)

(defn system-start [config]
  (alter-var-root #'system (constantly (ig/init config))))

(defn system-stop []
  (alter-var-root #'system ig/halt!))

; Компонент БД.
(defmethod ig/init-key ::db
  [_ {:keys [options]}]
  options) ; Здесь неплохо было бы создавать пул соединений.

; Компонент приложения (обёрнутый раутер).
(defmethod ig/init-key ::app
  [_ {:keys [options db]}]
  (-> router
      (md/try-catch-middleware)
      (md/format-body-keys :phase :post :format-fn csk/->camelCase)
      (ring/wrap-json-response)
      (md/key->request :components {:db db :options options})
      (md/format-body-keys :phase :pre :format-fn csk/->kebab-case)
      (ring/wrap-json-body {:keywords? true})))

; Компонент сервера.
(defmethod ig/init-key ::server
  [_ {:keys [options app]}]
  (run-jetty app options))

(defmethod ig/halt-key! ::server
  [_ server]
  (.stop server))

#_(
(def cols
  ["id int primary key generated always as identity"
   "fullname text not null"
   "sex char(1) not null"
   "birthday date not null"
   "address text not null"
   "policy_number text not null"])

(defn make-table
  "Создание таблицы users с колонками cols."
  []
  (let [db (-> "config.edn"
               (help/file->clj)
               (:db-spec))]
    (help/create-table db :users cols)))
  
     (make-table)
   )

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (let [flag-map {"-h" {:profile :heroku}
                  "-d" {:profile :dev}}]
    (if-let [profile (flag-map (first args))]
      (try (if-let [config (help/load-config "config.edn" profile)]
             (try (system-start config)
                  (let [exit (fn [signame]
                               (log/info (str "A " signame " was received. Halting components..."))
                               (system-stop)
                               (log/info "All components are halted.")
                               (System/exit 0))]
                    (with-handler :int (exit "SIGINT"))
                    #_(with-handler :kill (exit "SIGKILL"))
                    (with-handler :term (exit "SIGTERM")))
                  (catch Exception _
                    (log/fatal "Error at system startup.")
                    (System/exit -3)))
             (throw (Exception.)))
           (catch Exception _
             (log/fatal "Failed to load the configuration file.")
             (System/exit -2)))
      (do (log/fatal "Missing startup configuration key")
          (System/exit -1)))))

#_(-main "-d")