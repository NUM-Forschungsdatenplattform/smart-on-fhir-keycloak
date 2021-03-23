package de.vita.num.smart.events;

import org.keycloak.Config;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public class SmartOnFhirEventListenerProviderFactory implements EventListenerProviderFactory {

    @Override
    public SmartOnFhirEventListenerProvider create(KeycloakSession keycloakSession) {
        return new SmartOnFhirEventListenerProvider(keycloakSession);
    }

    @Override
    public void init(Config.Scope scope) {
    }

    @Override
    public void postInit(KeycloakSessionFactory keycloakSessionFactory) {
    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return "SMART_on_FHIR_Event_Listener";
    }

}
