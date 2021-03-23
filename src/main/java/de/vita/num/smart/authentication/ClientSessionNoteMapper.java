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

import org.keycloak.models.*;
import org.keycloak.protocol.oidc.mappers.AbstractOIDCProtocolMapper;
import org.keycloak.protocol.oidc.mappers.OIDCAccessTokenMapper;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.protocol.oidc.mappers.OIDCIDTokenMapper;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ServerInfoAwareProviderFactory;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.IDToken;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static de.vita.num.smart.authentication.SmartPatientAppAuthenticator.CLIENT_SESSION_NOTE_KEY;
import static de.vita.num.smart.authentication.SmartPatientAppAuthenticator.TokenRegistry;

public class ClientSessionNoteMapper extends AbstractOIDCProtocolMapper implements OIDCAccessTokenMapper, OIDCIDTokenMapper, ServerInfoAwareProviderFactory {
	private static final List<ProviderConfigProperty> configProperties = new ArrayList<ProviderConfigProperty>();
	private static final String CLIENT_SESSION_NOTE = "client.session.note";
	private static final String CLIENT_SESSION_MODEL_NOTE_LABEL = "Client Session Note";
	private static final String CLIENT_SESSION_MODEL_NOTE_HELP_TEXT = "Name of stored client session note within the ClientSession.note map.";

	static {
		ProviderConfigProperty property;
		property = new ProviderConfigProperty();
		property.setName(CLIENT_SESSION_NOTE);
		property.setLabel(CLIENT_SESSION_MODEL_NOTE_LABEL);
		property.setHelpText(CLIENT_SESSION_MODEL_NOTE_HELP_TEXT);
		property.setType(ProviderConfigProperty.STRING_TYPE);
		configProperties.add(property);
		OIDCAttributeMapperHelper.addAttributeConfig(configProperties, ClientSessionNoteMapper.class);
	}

	public static final String PROVIDER_ID = "smart-oidc-client-session-note-mapper";

	@Override
	public List<ProviderConfigProperty> getConfigProperties() {
		return configProperties;
	}

	@Override
	public String getId() {
		return PROVIDER_ID;
	}

	@Override
	public String getDisplayType() {
		return CLIENT_SESSION_MODEL_NOTE_LABEL;
	}

	@Override
	public String getDisplayCategory() {
		return TOKEN_MAPPER_CATEGORY;
	}

	@Override
	public String getHelpText() {
		return "Map a custom client session note to a token claim.";
	}


	public AccessToken transformUserInfoToken(AccessToken token, ProtocolMapperModel mappingModel, KeycloakSession session,
			UserSessionModel userSession, ClientSessionContext clientSessionCtx) {

		if (!OIDCAttributeMapperHelper.includeInUserInfo(mappingModel)) {
			return token;
		}

		setClaim(token, mappingModel, userSession, session, clientSessionCtx);
		return token;
	}


	@Override
	public AccessToken transformAccessToken(AccessToken token, ProtocolMapperModel mappingModel, KeycloakSession session,
			UserSessionModel userSession, ClientSessionContext clientSessionCtx) {

		if (!OIDCAttributeMapperHelper.includeInAccessToken(mappingModel)){
			return token;
		}

		setClaim(token, mappingModel, userSession, session, clientSessionCtx);
		return token;
	}


	@Override
	public IDToken transformIDToken(IDToken token, ProtocolMapperModel mappingModel, KeycloakSession session,
			UserSessionModel userSession, ClientSessionContext clientSessionCtx) {

		if (!OIDCAttributeMapperHelper.includeInIDToken(mappingModel)){
			return token;
		}

		setClaim(token, mappingModel, userSession, session, clientSessionCtx);
		return token;
	}

    @Override
    protected void setClaim(IDToken token,
							ProtocolMapperModel mappingModel,
							UserSessionModel userSession,
							KeycloakSession session,
							ClientSessionContext clientSessionCtx) {

        String noteName = mappingModel.getConfig().get(CLIENT_SESSION_NOTE);
		String noteValue = clientSessionCtx.getClientSession().getNote(noteName);
		if (noteValue == null){
			// WORKAROUND: noteValue may be null due to keycloak 11 bug.
			//  We check  here if the session has a key that means we're trying to
			// register a new user. In that case, TokenRegistry will have the patient
			// fhir resource id. This is a workaround: normally, we'd just use the session
			// but values written to session notes from the transaction are not showing up here
			// and we learn fhir patient resource id during the transaction, so we're using TokenRegistry
			// as an in-memory storage area where the patient id is assigned as the value.
			// so registration generates a GUID and assigns this to session notes.
			// in the transcation, we read this value from session note and use it as the key to place
			// a key/value into TokenRegistry, where the value is patient id.
			// then we read the guid from session notes here (because it makes it here if written from even listener)
			// and go and fetch the value from TokenRegistry
            String fhirPatientIdKey =
                clientSessionCtx
                    .getClientSession()
                    .getNote(CLIENT_SESSION_NOTE_KEY);
            if (fhirPatientIdKey != null) {
                noteValue = TokenRegistry.get(fhirPatientIdKey);
                if (noteValue != null) {
                    TokenRegistry.remove(fhirPatientIdKey);
                    OIDCAttributeMapperHelper.mapClaim(token, mappingModel, noteValue);
                    return;
                }
            }else{
                return;
            }
        }
		OIDCAttributeMapperHelper.mapClaim(token, mappingModel, noteValue);
	}

	@Override
	public Map<String, String> getOperationalInfo() {
		Map<String, String> ret = new LinkedHashMap<>();
        ret.put("version", "1.0");
        return ret;
	}
}
