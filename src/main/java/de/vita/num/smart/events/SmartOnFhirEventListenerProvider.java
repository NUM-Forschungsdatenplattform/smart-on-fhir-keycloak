package de.vita.num.smart.events;

import de.vita.num.smart.authentication.AccessTokenBuilder;
import org.jboss.logging.Logger;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmProvider;

public class SmartOnFhirEventListenerProvider implements EventListenerProvider {

    private static final Logger log = Logger.getLogger(SmartOnFhirEventListenerProvider.class);

    private final KeycloakSession session;
    private final RealmProvider model;
    private final AccessTokenBuilder _accessTokenBuilder;

    public SmartOnFhirEventListenerProvider(KeycloakSession session) {
        this.session = session;
        this.model = session.realms();
        _accessTokenBuilder = new AccessTokenBuilder(session);
    }

    @Override
    public void onEvent(Event event) {

        if (EventType.REGISTER.equals(event.getType())) {
            RealmModel realm = this.model.getRealm(event.getRealmId());
            String keycloakUserId = event.getUserId();

            session
                .getTransactionManager()
                .enlistPrepare(
                    new VitaKeycloakTransaction(
                        session.getContext().getAuthenticationSession(),
                        session,
                        realm,
                        keycloakUserId));
        }
    }

    @Override
    public void onEvent(AdminEvent adminEvent, boolean b) {
        //TODO: implement fhir/openehr config logic for admin registered users
    }

    @Override
    public void close() {
    }

}
