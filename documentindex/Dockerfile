FROM maven AS builder
WORKDIR /usr/local/saffron
COPY . /usr/local/saffron
RUN mvn install -pl documentindex -am
RUN ls -l /usr/local/saffron/documentindex/target

FROM openjdk:8
WORKDIR /usr/local/app
ENV CORPUS=corpus.json
ENV CORPUS_OUTPUT=corpus.json
ENV DOCS_OUTPUT=/docs

COPY --from=builder /usr/local/saffron/documentindex/target/documentindex-exec.jar ./documentindex.jar
ENTRYPOINT ["java", "-jar","/usr/local/app/documentindex.jar", "org.insightcentre.nlp.saffron.documentindex.DocumentIndexApplication"]
