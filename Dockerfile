FROM eclipse-temurin:21.0.1_12-jdk
COPY ./target/nixie*.jar nixie.jar
ENTRYPOINT ["java", "-jar", "nixie.jar"]