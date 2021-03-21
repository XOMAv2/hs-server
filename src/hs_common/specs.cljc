(ns hs-common.specs
  (:require [clojure.string]
            #?@(:clj [[clojure.spec.alpha :as s]
                      [clojure.instant :refer [read-instant-date]]]
                :cljs [[cljs.spec.alpha :as s]
                       [cljs.reader :refer [parse-timestamp]]])))

(s/def ::non-empty-string #(and (string? %) (seq %)))

(s/def ::->date
  (s/conformer
   #(cond (instance? #?(:clj java.util.Date :cljs js/Date) %) %
          (and (string? %) (seq %)) (try #?(:clj (read-instant-date %)
                                            :cljs (parse-timestamp %))
                                         (catch #?(:clj Exception :cljs js/Object) _
                                           ::s/invalid))
          :else ::s/invalid)))

(s/def ::->char (s/conformer #(cond (char? %) %
                                    (and (string? %) (seq %)) (first %)
                                    :else ::s/invalid)))

(def valid-sex? #{\m \f \x})

(s/def ::->sex (s/and ::->char
                      (s/conformer #(let [c (if (char? %) % (first %))]
                                      (if (valid-sex? c) c ::s/invalid)))))

(s/def ::->int (s/conformer #(cond (int? %) %
                                   (string? %) (let [s (clojure.string/replace % #" " "")]
                                                 #?(:clj (try (Integer/parseInt s)
                                                              (catch Exception _ ::s/invalid))
                                                    :cljs (let [res (js/parseInt s)]
                                                            (if (.isNaN js/Number res)
                                                              ::s/invalid
                                                              res))))
                                   :else ::s/invalid)))

(s/def ::id ::->int)
(s/def ::fullname ::non-empty-string)
(s/def ::sex ::->sex)
(s/def ::birthday ::->date)
(s/def ::address ::non-empty-string)
(s/def ::policy-number ::non-empty-string)

(s/def ::user-form (s/keys :req-un [::fullname
                                    ::sex
                                    ::birthday
                                    ::address
                                    ::policy-number]))

(comment "Пример мапы, удовлетворяющей вышеописанной спеке:"
         (s/conform ::user-form {:fullname "Nikki"
                                 :sex "m"
                                 :birthday "2019-11-09"
                                 :address "Moscow"
                                 :policy-number "544256"}))

(s/def ::user (s/and ::user-form
                     (s/keys :req-un [::id])))

(s/def ::users (s/coll-of ::user :into []))