package de.vita.num.smart.events;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.BearerTokenAuthInterceptor;
import de.vita.num.smart.authentication.*;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.StringType;
import org.jboss.logging.Logger;
import org.keycloak.models.AbstractKeycloakTransaction;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.sessions.AuthenticationSessionModel;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static de.vita.num.smart.authentication.ModuleConfiguration.DemographicBaseUrl;
import static de.vita.num.smart.authentication.SmartPatientAppAuthenticator.*;

public class VitaKeycloakTransaction extends AbstractKeycloakTransaction {
    private static Logger log = Logger.getLogger(VitaKeycloakTransaction.class);

    private final KeycloakSession _session;
    private final RealmModel realm;
    private final String _userId;
    private final AccessTokenBuilder _accessTokenBuilder;
    private final AuthenticationSessionModel _authSessionModel;


    public VitaKeycloakTransaction(AuthenticationSessionModel pAuthSessionModel,
                                   KeycloakSession pSession,
                                   RealmModel pRealm,
                                   String pKeycloakUserId) {
        _authSessionModel = pAuthSessionModel;
        realm =pRealm;
        _userId = pKeycloakUserId;
        _session = pSession;
        _accessTokenBuilder = new AccessTokenBuilder(_session);
    }

    @Override
    protected void commitImpl() {
        try{
            UserModel user =
                _session
                    .users()
                    .getUserById(_userId, realm);
//        log.info(user.getFirstName());

            String accessToken = _accessTokenBuilder.createAccessToken(realm.getId());

            String fhirResId =
                createFhirPatientResource(
                    accessToken,
                    _userId,
                    user.getFirstName(),
                    user.getLastName());

            String ehrId = createEhrBaseEhr(_userId, accessToken);

            /**
             * for some reason, writing into session from this point in code is not making it
             * to note mapper. so we're using the value already written in the session (from event listener)
             * to find the registration session id and using the same value as the key to write
             * the fhir resource id into an in memory cache
             */
            String registrationSessionId = _authSessionModel.getClientNote(CLIENT_SESSION_NOTE_KEY);
            TokenRegistry.put(registrationSessionId, fhirResId);

            if (ehrId == null) {
                //TODO: KILL TRANSACTION HERE SOMEHOW
            }
        }catch (Exception ex){
            log.error("Exception while creating FHIR and openEHR configuration " +
                          "for a new user registration.", ex);
            rollbackImpl();
        }
    }

    public String createEhrBaseEhr(String pFhirPatientId, String pToken){
        try (CloseableHttpClient httpC = HttpClients.createDefault()) {
            HttpPost post = new HttpPost("http://localhost:8888/fhir-bridge/ehradmin");

            URI uri = new URIBuilder(post.getURI())
                .addParameter("pPatientId", pFhirPatientId)
                .build();
            post.setURI(uri);

            post.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + pToken);

            try (CloseableHttpResponse response = httpC.execute(post)) {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                response.getEntity().writeTo(bos);
                String ehrId = bos.toString(StandardCharsets.UTF_8);
                EntityUtils.consume(response.getEntity());//recommended cleanup
                return ehrId;
            } catch (Exception ex) {
                return null;
            }

        } catch (Exception ex) {
            //TODO: return failure status, cancel user registration transaction
            //if we don't end up with a patient res. AND an EHR both
            return null;
        }
    }

    private String createFhirPatientResource(String token,
                                            String pPatientId,
                                            String pFirstName,
                                            String pFamilyName) throws SmartOnFhirException {
        try {
            FhirContext ctx = FhirContext.forR4();
            IGenericClient client = ctx.newRestfulGenericClient(DemographicBaseUrl());

            BearerTokenAuthInterceptor bearerTokenAuthInterceptor = new BearerTokenAuthInterceptor(token);
            client.registerInterceptor(bearerTokenAuthInterceptor);

            Patient patient = new Patient();
            patient
                .addIdentifier()
                .setSystem(FhirConstants.PATIENT_IDENIFIER_SYSTEM)
                .setValue(pPatientId);
            patient
                .addName()
                .setGiven(Arrays.asList(new StringType(pFirstName)))
                .setFamily(pFamilyName)
                .setText(pFirstName + " " + pFamilyName);
            MethodOutcome createPatientOutcome =
                client
                    .create()
                    .resource(patient)
                    .execute();

            return createPatientOutcome.getId().getIdPart();
        } catch (Exception ex) {
            throw new SmartOnFhirException("Could not create FHIR Patient Resource", ex);
        }
    }

    @Override
    protected void rollbackImpl() {
        //TODO: implement rollback logic
    }


}
