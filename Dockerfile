FROM clojure
COPY . /usr/src/app
WORKDIR /usr/src/app
COPY config.heroku.edn ./target/uberjar
CMD java -jar ./target/uberjar/hs-server-0.1.0-SNAPSHOT-standalone.jar -h