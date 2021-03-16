(defproject hs-server "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [ring/ring-core "1.8.2"]
                 [ring/ring-jetty-adapter "1.8.2"]
                 [ring/ring-json "0.4.0"]
                 [com.github.seancorfield/next.jdbc "1.1.643"]
                 [org.postgresql/postgresql "42.2.18.jre7"]
                 [expound "0.8.9"] ; Приятные отчёты об ошибках для clojure.spec.alpha.
                 [camel-snake-kebab "0.4.2"] ; Либа для форматирования стиля написания.
                 [integrant "0.8.0"] ; Либа для создания компонентов.
                 [spootnik/signal "0.2.4"] ; Либа для обработки сигналов.
                 [compojure "1.6.2"]]
  :main ^:skip-aot hs-server.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}})
