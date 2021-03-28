(ns hs-server.specs
  (:require [clojure.spec.alpha :as s]
            [hs-common.specs :as ss]))

(s/def ::dbtype #{"postgresql"})
(s/def ::dbname ::ss/non-empty-string)
(s/def ::host ::ss/non-empty-string)
(s/def ::port int?)
(s/def ::user ::ss/non-empty-string)
(s/def ::password ::ss/non-empty-string)
(s/def ::uri ::ss/non-empty-string)
(s/def :db/options (s/keys :req-un [::dbtype
                                    ::dbname
                                    ::host
                                    ::port
                                    ::user
                                    ::password]
                           :opt-un [::uri]))
(s/def ::db (s/keys :req-un [:db/options]))

(s/def ::service-uri ::ss/non-empty-string)
(s/def :app/options (s/keys :req-un [::service-uri]))
(s/def ::app (s/keys :req-un [:app/options]))

(s/def ::join? boolean?)
(s/def :server/options (s/keys :req-un [::host
                                        ::port
                                        ::join?]))
(s/def ::server (s/keys :req-un [:server/options]))