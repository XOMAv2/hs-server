(ns hs-server.helpers
  (:require [integrant.core :as ig]
            [next.jdbc :as jdbc]
            [clojure.spec.alpha :as s]
            [clojure.string]
            [clojure.set]
            [clojure.walk :refer [postwalk]]
            [aero.core :as aero]
            [expound.alpha :as ex]))

(defmethod aero/reader 'ig/ref
  [_ _ value]
  (ig/ref value))

(defn load-config
  "Загрузка файла конфигурации из edn-файла с поддержкой тега #ig/ref и тегами, определёнными в
   библиотеке aero."
  [filename opts]
  (aero/read-config filename opts))

(defn create-table-ddl
  "table-name - имя таблицы (строка или ключевое слово).
   cols-def - массив строк, где каждая строка - описание создаваемого столбца.
   :can-exist - ключ, если true, то перед именем таблицы будет добавлена строка \"IF NOT EXISTS\".
   Пример вызова:
   (create-table (jdbc/get-conntection db-spec)
                 :orders
                 [\"id int primary key generated always as identity\"
                  \"order_date timestamp not null\"
                  \"order_uid uuid not null unique\"
                  \"status VARCHAR (255) not null\"])"
  [table-name cols-def & {:keys [can-exist]}]
  (format "CREATE TABLE%s %s (%s)"
          (if can-exist " IF NOT EXISTS" "")
          (name table-name)
          (clojure.string/replace (clojure.string/join ", " cols-def) #"\s\s+" " ")))

(defn create-table
  "db - DataSource, Connection, спецификация для подключения к БД.
   table-name - имя таблицы (строка или ключевое слово).
   cols-def - массив строк, где каждая строка - описание создаваемого столбца.
   Пример вызова:
   (create-table db-spec :orders
                 [\"id int primary key generated always as identity\"
                  \"order_date timestamp not null\"
                  \"order_uid uuid not null unique\"
                  \"status VARCHAR (255) not null\"])"
  [db table-name cols-def]
  (jdbc/execute! db [(create-table-ddl table-name cols-def :can-exist true)]))

(defn beautiful-spec-explain
  "Просто обёртка над функцией expound-str библиотеки expound с несколькими ключиками."
  [spec val]
  (ex/expound-str spec val
                  {:print-specs? false
                   :show-valid-values? true}))

(defn change-keys-style
  "clj-struct - структура языка Clojure, стиль ключей в которой необходимо изменить.
   format-fn - функция форматирования ключа (для либы camel-snake-kebab это ->snake_case и т.д.).
   Изменяет стиль ключевых слов в структурах любой вложенности."
  [clj-struct format-fn]
  (postwalk #(if (keyword? %)
               (format-fn %)
               %)
            clj-struct))

(defn ->unqualify
  "Снимает кваливикатор с ключевого слова."
  [kw]
  (keyword (name kw)))

(defn spec->keys
  "spec-k - спека, кторая была определена формой (s/def spec-k (s/keys ...)).
   Получение всех ключей верхнего уровня из spec-k (и обязательных, и опциональных, и
   квалифицированных, и нет)."
  [spec-k]
  (let [form (s/form spec-k)
        params (apply hash-map (rest form))
        {:keys [req opt req-un opt-un]} params]
    (concat req opt (map ->unqualify opt-un) (map ->unqualify req-un))))

(defn spec->map
  "spec-k - спека, кторая была определена формой (s/def spec-k (s/keys ...)).
   Формирование мапы, ключами которой являются все ключи верхнего уровня из spec-k (и обязательные,
   и опциональные, и квалифицированные, и нет), а значениями - спеки, эти ключи валидирующие."
  [spec-k]
  (let [form (s/form spec-k)
        params (apply hash-map (rest form))
        {:keys [req opt req-un opt-un]} params
        keys (concat req opt (map ->unqualify opt-un) (map ->unqualify req-un))
        vals (concat req opt opt-un req-un)]
    (apply hash-map (mapcat #(vector % %2) keys vals))))

(defn partial-conform
  "spec-map - мапа, ключями которой служат ключи val-map'а, ожидающие conform'а, а значения - спеки.
   val-map - мапа, key-value пары из которой будут conform'иться по отдельности.
   Если хотябы один confrom вернул значение invalid, то функция вернёт invalid, иначе будет
   возвращена val-mal с conform'ленными значениями."
  [spec-map val-map]
  (let [res (mapcat (fn [[k v]]
                      [k (s/conform (k spec-map) v)])
                    (vec val-map))]
    (if (some s/invalid? res)
      ::s/invalid
      (apply hash-map res))))

(defmacro if-let-conform
  "sym - символ для привязки conform'ированного значения.
   spec - спека или мапа спек (см. функцию partial-conform).
   val - confrom'ируемое значение.   
   Если результат вызова clojure.spec.alpha/conform равен invalid, то вызывается форма else (если
   есть), иначе будет осуществлено связывание результата с символом sym и выполнена форма then."
  ([[sym [spec val]] then]
   `(if-let-conform ~[sym [spec val]] ~then nil))
  ([[sym [spec val]] then else]
   (let [spec (eval spec)
         conform-fn (if (map? spec) partial-conform s/conform)]
     `(let [res# (try (~conform-fn ~spec ~val)
                      (catch Exception e# :clojure.spec.alpha/invalid))]
        (if (s/invalid? res#)
          ~else
          (let [~sym res#]
            ~then))))))