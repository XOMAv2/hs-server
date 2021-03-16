(ns hs-server.specs
  (:require [clojure.spec.alpha :as s]
            [clojure.instant :refer [read-instant-date]]))

(s/def ::non-empty-string #(and (string? %)
                                (not (empty? %))))

(s/def ::->java-date (s/conformer
                      #(cond (instance? java.util.Date %) %
                             (and (string? %) (not (empty %))) (try (read-instant-date %)
                                                                    (catch Exception _
                                                                      ::s/invalid))
                             :else ::s/invalid)))

(s/def ::->char (s/conformer #(cond
                                (char? %) %
                                (and (string? %)
                                     (= (count %) 1)) (first %)
                                :else ::s/invalid)))

(s/def ::fullname ::non-empty-string)
(s/def ::sex ::->char)
(s/def ::birthday ::->java-date)
(s/def ::address ::non-empty-string)
(s/def ::policy-number ::non-empty-string)

(s/def ::user-form (s/keys :req-un [::fullname
                                    ::sex
                                    ::birthday
                                    ::address
                                    ::policy-number]))

(comment ; Пример мапы, удовлетворяющей вышеописанной спеке:
         (s/conform ::user-form {:fullname "Nikki"
                                 :sex "m"
                                 :birthday "2019-11-09"
                                 :address "Moscow"
                                 :policy-number "544256"}))