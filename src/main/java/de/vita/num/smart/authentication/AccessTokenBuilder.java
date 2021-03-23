package de.vita.num.smart.authentication;

import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.KeyUse;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.jose.jws.JWSBuilder;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.representations.AccessToken;
import org.keycloak.services.Urls;

import java.security.PrivateKey;

public class AccessTokenBuilder {
    private final KeycloakSession _session;

    public AccessTokenBuilder(KeycloakSession pSession){
        _session = pSession;
    }

    /**
     * Creates an OAuth access token without calling the REST APIs of KeyCloak
     * @param pRealmId The Realm Id of the Realm where the authentication session is takign place
     * @return An OAuth token in string form
     */
    public String createAccessToken(String pRealmId) throws SmartOnFhirException {
        try{
            KeycloakContext ctx = this._session.getContext();
            AccessToken t = new AccessToken();
            t.audience("demographic-server");//TODO: not implemented on the token receiver side yet
            t.subject("keycloak");//it's the keycloak service itself acquiring this token
            t.issuedFor("demographic-server");//TODO: which claim will this produce?
            t.type("Bearer");
            //t.setOtherClaims("vitakc", "claim");//TODO: any claims we want to add
            t.issuer(
                Urls.realmIssuer(
                    ctx.getUri().getBaseUri(),
                    ctx.getRealm().getName()));
            t.issuedNow();
            AccessToken.Access access = new AccessToken.Access();
            access.addRole("admin");
            t.setRealmAccess(access);
            //t.getRealmAccess().addRole("admin");
            t.expiration((int)(t.getIat() + 30L));//30 seconds lifespan

            KeyWrapper keyWrapper =
                this._session.keys()
                    .getActiveKey(
                        this._session.realms().getRealm(pRealmId),
                        KeyUse.SIG,
                        Algorithm.RS256);
            return new JWSBuilder()
                .kid(keyWrapper.getKid())
                .type("JWT")
                .jsonContent(t)
                .rsa256((PrivateKey) keyWrapper.getPrivateKey());
        }catch (Exception ex){
            throw new SmartOnFhirException("Exception while trying to create " +
                                               "an access token using internal Keycloak APIs.", ex);
        }
    }
}
