FROM adoptopenjdk/openjdk8
COPY target/Registration-0.0.1-SNAPSHOT.jar .
EXPOSE 55551
CMD java -jar Registration-0.0.1-SNAPSHOT.jar