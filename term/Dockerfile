FROM maven AS builder
WORKDIR /usr/local/saffron
COPY . /usr/local/saffron
RUN mvn install -pl term -am
RUN ls -l

FROM openjdk:8
WORKDIR /usr/local/app
ENV SAFFRON_VERSION=4.0.0
ENV CORPUS=corpus.json
ENV CONFIG=config.json
ENV TERM_OUTPUT=terms-extracted.json
ENV DOC_TERM_OUTPUT=doc-terms.json

COPY --from=builder /usr/local/saffron/term/target/term-exec.jar ./term.jar

ENTRYPOINT ["java", "-jar","/usr/local/app/term.jar", "org.insightcentre.nlp.saffron.term.TermExtractionApplication"]
