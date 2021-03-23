package de.vita.num.smart.authentication;

public class FhirConstants {
    //TODO: These should come from a config file. How does it work in wildfly/jboss modules?
    public static final String DEMOGRAPHIC_BASE_URL = "http://localhost:8082/fhir";

    //using the subsytem value used by keycloak config as the system id
    public static final String PATIENT_IDENIFIER_SYSTEM = "urn:jboss:domain:keycloak:1.1";
}
