FROM openjdk:11
VOLUME /tmp

ARG JAR_FILE

ADD $JAR_FILE /usr/share/audit-service/audit-service.jar

LABEL service="audit", project="palisade"

ENTRYPOINT ["java", "-jar", "/usr/share/audit-service/audit-service.jar"]