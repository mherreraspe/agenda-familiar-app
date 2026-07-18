FROM maven:3.9.11-eclipse-temurin-25 AS construccion
WORKDIR /codigo
COPY pom.xml ./pom.xml
COPY servicios/autenticacion/pom.xml servicios/autenticacion/pom.xml
COPY servicios/agenda/pom.xml servicios/agenda/pom.xml
COPY servicios/autenticacion/src servicios/autenticacion/src
RUN mvn -pl servicios/autenticacion -am -DskipTests package

FROM eclipse-temurin:25-jre-alpine
RUN addgroup -S aplicacion && adduser -S aplicacion -G aplicacion
USER aplicacion
WORKDIR /aplicacion
COPY --from=construccion /codigo/servicios/autenticacion/target/servicio-autenticacion-*.jar aplicacion.jar
EXPOSE 8101
ENTRYPOINT ["java", "-jar", "/aplicacion/aplicacion.jar"]
