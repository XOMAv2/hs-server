FROM clojure
WORKDIR /usr/src/app
COPY . .
COPY config.edn ./target/uberjar
CMD java -jar ./target/uberjar/hs-server-0.1.0-SNAPSHOT-standalone.jar -h