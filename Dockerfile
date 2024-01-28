FROM eclipse-temurin:17.0.10_7-jdk
COPY ./target/nixie*.jar nixie.jar
ENTRYPOINT ["java", "-jar", "nixie.jar"]