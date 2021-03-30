(ns hs-server.repository-test
  (:require [clojure.test :refer [use-fixtures testing deftest is]]
            [hs-server.repository :as repo]
            [hs-server.helpers :as help]
            [next.jdbc :as jdbc]
            [next.jdbc.sql :as sql]))

(def ^:dynamic *db*)

(def db-config (-> "config.edn"
                   (help/load-config {:profile :test})
                   (:hs-server.core/db)
                   (:options)))

(def cols
  ["id int primary key generated always as identity"
   "fullname text not null"
   "sex char(1) not null"
   "birthday date not null"
   "address text not null"
   "policy_number text not null"])

(defn fix-create-db [t]
  (help/create-table db-config :users cols)
  (t))

(defn fix-with-multi-db [t]
  (binding [*db* db-config]
    (testing "PostgreSQL config."
      (t)))
  (binding [*db* (jdbc/get-datasource db-config)]
    (testing "Datasource from PostgreSQL."
      (t)))
  (binding [*db* (jdbc/get-connection db-config)]
    (testing "Connection from PostgreSQL."
      (t))))

(defn fix-db-data [t]
  (jdbc/execute! db-config ["truncate users;"])
  (doseq [i (range 0 10)]
    (sql/insert! db-config :users {:fullname (str "Nikki " i)
                                   :sex "m"
                                   :birthday #inst "2019-11-09"
                                   :address "Moscow"
                                   :policy_number "544256"}))
  (t))

(use-fixtures :once fix-create-db fix-with-multi-db)
(use-fixtures :each fix-db-data)

(deftest test-get-all-users
  (testing "Wrong arguments."
    (is (thrown? java.lang.AssertionError (repo/get-all-users nil))))
  (testing "Correct arguments."
    (is (= 10 (count (repo/get-all-users *db*))))
    (do (jdbc/execute! db-config ["truncate users;"])
        (is (= [] (repo/get-all-users *db*))))))

;; (deftest test-get-user-by-id
;;   (testing ""
;;     ;(is (= {} (get-user-by-id *db* id)) "Некорректный тип аргументов.")
;;     ;(is (= {} (get-user-by-id *db* id)) "Некорректные ключи в мапах.")
;;     ;(is (= [] (get-user-by-id *db* id)) "Некорректные значения в мапах.")
;;     (is (= [] (get-user-by-id *db* id)))))

;; (deftest test-add-user
;;   (testing ""
;;     (is (= {} (add-user *db* user)))))

;; (deftest test-delete-user
;;   (testing ""
;;     (is (= {} (delete-user *db* id)))))

;; (deftest test-update-user
;;   (testing ""
;;     (is (= {} (update-user *db* id user)))))

#_ (run-tests)