FROM maven AS builder

WORKDIR /usr/local/saffron
COPY . /usr/local/saffron
RUN mvn install -pl authors -am
RUN ls -l /usr/local/saffron/authors/target

FROM openjdk:8

WORKDIR /usr/local/app
COPY --from=builder /usr/local/saffron/authors/target/authors-exec.jar ./authors.jar


ENTRYPOINT ["java", "-jar","/usr/local/app/authors.jar", "org.insightcentre.nlp.saffron.authors.AuthorsApplication"]

