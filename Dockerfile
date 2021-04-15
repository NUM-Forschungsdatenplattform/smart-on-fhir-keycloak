FROM busybox
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} smart-on-fhir-plugin.jar
COPY src/main/jboss-deployment/hapi-fhir/jars jars
