FROM maven:3-openjdk-16-slim AS builder
WORKDIR /usr/local/saffron
COPY . /usr/local/saffron
RUN mvn install -pl taxonomy -am
RUN java -version

FROM openjdk:14
WORKDIR /usr/local/app
COPY --from=builder /usr/local/saffron/taxonomy/target/taxonomy-exec.jar ./taxonomy.jar

ENTRYPOINT ["java", "-jar","/usr/local/app/taxonomy.jar", "org.insightcentre.nlp.saffron.taxonomy.supervised.TaxonomyExtractionApplication"]