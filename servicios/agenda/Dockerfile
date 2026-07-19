FROM maven:3.9.11-eclipse-temurin-25 AS construccion
WORKDIR /codigo
COPY pom.xml ./pom.xml
COPY servicios/autenticacion/pom.xml servicios/autenticacion/pom.xml
COPY servicios/agenda/pom.xml servicios/agenda/pom.xml
COPY servicios/agenda/src servicios/agenda/src
RUN mvn -pl servicios/agenda -am -DskipTests package

FROM eclipse-temurin:25-jre-alpine
RUN addgroup -S -g 10001 aplicacion && adduser -S -u 10001 aplicacion -G aplicacion
USER aplicacion
WORKDIR /aplicacion
COPY --from=construccion /codigo/servicios/agenda/target/servicio-agenda-*.jar aplicacion.jar
EXPOSE 8102
ENTRYPOINT ["java", "-jar", "/aplicacion/aplicacion.jar"]
