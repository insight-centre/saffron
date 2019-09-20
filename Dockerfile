FROM openjdk:8-jre-alpine

# First run `mvn -f web/pom.xml package`
COPY web/target/saffron-web-3.3-jar-with-dependencies.jar /saffron.jar
COPY web/static /static/
COPY models/ /models/

RUN apk update
RUN apk add mongodb

RUN mkdir -p /data/db/

WORKDIR /
CMD mongod & /usr/bin/java -cp /saffron.jar org.insightcentre.saffron.web.Launcher
