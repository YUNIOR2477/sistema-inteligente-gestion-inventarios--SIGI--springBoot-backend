FROM amazoncorretto:21-alpine-jdk

COPY target/sigi-backend-0.0.1-SNAPSHOT.jar /api-sigi-v1.jar

ENTRYPOINT ["java","-jar","/api-sigi-v1.jar"]