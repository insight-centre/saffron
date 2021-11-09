FROM maven

WORKDIR /usr/src/app
COPY pom.xml .
RUN mvn install
COPY . .
RUN ls -l /usr/src/app/target

FROM openjdk
WORKDIR /usr/src/app
COPY --from=0 /usr/src/app/target/*.jar ./core/target/

RUN ls -l /usr/src/app/core/target/
#RUN mvn -Dexec.mainClass='org.insightcentre.nlp.saffron.Main'
CMD [ "java", "-jar", "/usr/src/app/core/target/saffron-core-4.0.0-development-jar-with-dependencies.jar" ]