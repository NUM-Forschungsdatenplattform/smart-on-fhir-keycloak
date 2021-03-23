/**
 * This Source Code Form is subject to the terms of the Mozilla Public License, v.
 * 2.0 with a Healthcare Disclaimer.
 * A copy of the Mozilla Public License, v. 2.0 with the Healthcare Disclaimer can
 * be found under the top level directory, named LICENSE.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * http://mozilla.org/MPL/2.0/.
 * If a copy of the Healthcare Disclaimer was not distributed with this file, You
 * can obtain one at the project website https://github.com/igia.
 *
 * Copyright (C) 2018-2019 Persistent Systems, Inc.
 */
package de.vita.num.smart.authentication;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import ca.uhn.fhir.rest.client.interceptor.BearerTokenAuthInterceptor;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Resource;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.services.ServicesLogger;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import static de.vita.num.smart.authentication.ModuleConfiguration.DemographicBaseUrl;

public class SmartPatientAppAuthenticator implements Authenticator {
	private static final ServicesLogger logger = ServicesLogger.LOGGER;

	public static final String CLIENT_SESSION_NOTE_KEY = "client_session_note_key";

	public static final String SCOPE_LAUNCH_PATIENT = "launch/patient";


	private static <K, V> Map<K, V> buildEvictingMap(final int limit) {
        return Collections.synchronizedMap(new LinkedHashMap<K, V>(limit) {
            @Override
            protected boolean removeEldestEntry(Entry<K, V> pEldest) {
                return size() > limit;
            }
        });
    }

	/**
	 * This is a workaround for Keycloak's failure to write to session notes from a transaction.
	 * This Map is used to provide access to a FHIR patient resource Id from different phases of
	 * registration of a new user.
	 */
	public static final Map<String,String> TokenRegistry = buildEvictingMap(10000);

	@Override
	public void authenticate(AuthenticationFlowContext context) {
		try{
			String scope =
				context
					.getAuthenticationSession()
					.getClientNote(OIDCLoginProtocol.SCOPE_PARAM);

			if(!scope.contains(SCOPE_LAUNCH_PATIENT)){
				context.failure(AuthenticationFlowError.IDENTITY_PROVIDER_ERROR);
			}else{
				UserModel authenticatedUser = context.getAuthenticationSession().getAuthenticatedUser();
				String accessToken =
					new AccessTokenBuilder(context.getSession())
						.createAccessToken(context.getRealm().getId());

				String patientResourceId = findFhirPatientId(authenticatedUser.getId(), accessToken);
				context
					.getAuthenticationSession()
					.setClientNote(SCOPE_LAUNCH_PATIENT, patientResourceId);
				context.success();
			}
		}catch(Exception ex){
			logger.error("Exception while authentication a SMART ON FHIR authentication request", ex);
			context.failure(AuthenticationFlowError.INTERNAL_ERROR);
		}
	}

    private String findFhirPatientId(String pKeycloakUserId, String pBearerToken) throws SmartOnFhirException {
        try{
			String fhirDemographicBaseUrl = DemographicBaseUrl();
			FhirContext fhirContext = FhirContext.forR4();
			IGenericClient fhirClient = fhirContext.newRestfulGenericClient(fhirDemographicBaseUrl);
			fhirContext.getRestfulClientFactory().setServerValidationMode(ServerValidationModeEnum.NEVER);

			BearerTokenAuthInterceptor tokenInterceptor = new BearerTokenAuthInterceptor(pBearerToken);
			fhirClient.registerInterceptor(tokenInterceptor);

			Bundle searchResult = null;
			try {
				searchResult =
					fhirClient
						.search()
						.forResource(Patient.class)
						.where(Patient.IDENTIFIER.exactly().identifier(pKeycloakUserId))
						.returnBundle(Bundle.class)
						.execute();
			} catch (Exception ex) {
				throw ex;
			}
			Resource patient = searchResult.getEntry().get(0).getResource();
			return patient.getIdElement().getIdPart();
		}catch(Exception ex){
        	throw new SmartOnFhirException("Exception while trying to retrieve FHIR Patient Resource Id", ex);
		}
    }

	@Override
	public void action(AuthenticationFlowContext context) {
		try{
			/**
			 * Normally, this action method is the point the user would be directed after
			 * they are sent to a challenge (login form) by a failed authenticate method call (see @authenticate method)
			 * However, this implementation is meant to run as the last step in an authentication flow and it simply
			 * fails if an authentication call fails. So there is no redirect to a challenge and no way for
			 * an authentication session to end up here. Therefore we fail with Invalid_client_session.
			 */
			context.failure(AuthenticationFlowError.INVALID_CLIENT_SESSION);
		}catch(Exception ex){
			logger.error("Exception while authentication a SMART ON FHIR authentication request", ex);
			context.failure(AuthenticationFlowError.INTERNAL_ERROR);
		}
	}

	@Override
	public boolean requiresUser() {
		return false;
	}

	@Override
	public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
		return true;
	}

	@Override
	public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
	}

	@Override
	public void close() {
	}
}
