FROM maven AS builder
WORKDIR /usr/local/saffron
COPY . /usr/local/saffron
RUN mvn install -pl topic -am
RUN ls -l /usr/local/saffron/topic/target/

FROM openjdk:8
WORKDIR /usr/local/app
COPY --from=builder /usr/local/saffron/topic/target/term-exec.jar ./topic.jar

ENTRYPOINT ["java", "-jar","/usr/local/app/topic.jar", "org.insightcentre.nlp.saffron.topic.topicsim.TermSimilarityApplication"]

