(ns hs-server.middlewares
  (:require [hs-server.helpers :refer [change-keys-style]]))

(defn format-body-keys
  "В теле запроса/ответа обходит структуры языка Clojure любой вложенности и изменяет стиль ключевых
   слов при помощи указанной функции форматирования format-fn.
   Значение ключа :phase ...
       :pre - применит форматирование к запросу,
       :post - применит форматирование к ответу,
       остальные значения - не применяют форматирование."
  [handler & {:keys [format-fn phase]}]
  (fn [request]
    (let [request (if (= phase :pre)
                    (update request :body #(change-keys-style % format-fn))
                    request)
          response (handler request)
          response (if (= phase :post)
                     (update response :body #(change-keys-style % format-fn))
                     response)]
      response)))

(defn try-catch-middleware
  "Последний рубеж обороны."
  [handler & {:keys [default-msg]}]
  (fn [request]
    (try (handler request)
         (catch Exception e
           {:status 500 :body {:message (str "Брошено исключение "
                                             (ex-message e)
                                             \newline
                                             default-msg)}}))))

(defn key->request
  "Добавляет слот к запросу при помощи функции assoc."
  [handler key value]
  (fn [request]
    (let [request (assoc request key value)]
      (handler request))))