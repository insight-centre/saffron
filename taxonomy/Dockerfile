FROM maven AS builder
WORKDIR /usr/local/saffron
COPY . /usr/local/saffron
RUN mvn install -pl taxonomy -am -DskipTests=true
RUN java -version

FROM openjdk:8
WORKDIR /usr/local/app
COPY --from=builder /usr/local/saffron/taxonomy/target/taxonomy-exec.jar ./taxonomy.jar

ENTRYPOINT ["java", "-jar","/usr/local/app/taxonomy.jar", "org.insightcentre.nlp.saffron.taxonomy.supervised.TaxonomyExtractionApplication"]