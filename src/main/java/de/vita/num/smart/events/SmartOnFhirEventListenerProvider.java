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
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.sessions.AuthenticationSessionModel;

import java.util.UUID;

import static de.vita.num.smart.authentication.SmartPatientAppAuthenticator.CLIENT_SESSION_NOTE_KEY;
import static de.vita.num.smart.authentication.SmartPatientAppAuthenticator.SCOPE_LAUNCH_PATIENT;

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
            AuthenticationSessionModel authSession =
                session
                    .getContext()
                    .getAuthenticationSession();

            // don't proceed if this is not a smart on fhir app user registration
            String scope = authSession.getClientNote(OIDCLoginProtocol.SCOPE_PARAM);
            if (!scope.contains(SCOPE_LAUNCH_PATIENT))
                return;

            RealmModel realm = this.model.getRealm(event.getRealmId());
            String keycloakUserId = event.getUserId();

            /**
             * WORKAROUND: place a guid into session's client notes
             * using CLIENT_SESSION_NOTE_KEY as the key and GUID as the value
             * we'll first use this from the VitaKeyCloakTransaction, then we'll
             * check for it from the claim mapper (ClientSessionNoteMapper.java)
             * this guid value establishes communication between the registration event
             * and the first access token generated after registration.
             * this is not needed with kc 12.
             */
            authSession
                .setClientNote(
                    CLIENT_SESSION_NOTE_KEY,
                    UUID.randomUUID().toString());

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
