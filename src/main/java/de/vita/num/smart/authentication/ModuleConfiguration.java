package de.vita.num.smart.authentication;

import org.jboss.modules.Module;

public class ModuleConfiguration {

    //should match property name in module.xml
    public static final String PROPERTY_DEMOGRAPHIC_BASE_URL = "demographic.base.url";
    private static final String PROPERTY_FHIR_BRIDGE_EHR_ADMIN_ENDPOINT_URL = "fhirbridge.ehradmin.endpoint.url";

    /**
     * Get the base URL for the fhir demographic service using jboss module metadata as the configuration source.
     * Normally, this metadata is represented as property elements in the module.xml file
     * @return Base Url of the fhir demographic service
     * @throws SmartOnFhirException If the configuration value is not available.
     */
    public static String DemographicBaseUrl() throws SmartOnFhirException {
        //TODO: assuming this is thread safe. Ideally, this should go behind an interface
        //but I don't know how wildfly dependency injection works. so keeping it simple for the moment
        Module module = Module.forClass(ModuleConfiguration.class);
        String fhirDemographicBaseUrl = module.getProperty(PROPERTY_DEMOGRAPHIC_BASE_URL);
        if (fhirDemographicBaseUrl == null || fhirDemographicBaseUrl.equals("")) {
            throw new SmartOnFhirException(
                "Keycloak plugin module should have a " + PROPERTY_DEMOGRAPHIC_BASE_URL +
                    " property with an url value");
        }
        return fhirDemographicBaseUrl;
    }

    /**
     * Get the URL for the Fhir Bridge Ehr Admin endpoint. This endpoint exposes openEHR EHR related operations
     * in a simple way, for the keycloak plugin to create EHRs during registration, or perform other tasks
     * as required in the future. The actual URL is normally stored as module metadata, which is represented by
     * a module.xml file.
     * @return Url of the Ehr Admin endpoint.
     * @throws SmartOnFhirException If the configuratation value is not available.
     */
    public static String FhirBridgeEHRAdminEndpointUrl() throws SmartOnFhirException {
        Module module = Module.forClass(ModuleConfiguration.class);
        String endpointUrl = module.getProperty(PROPERTY_FHIR_BRIDGE_EHR_ADMIN_ENDPOINT_URL);
        if (endpointUrl == null || endpointUrl.equals("")) {
            throw new SmartOnFhirException(
                "Keycloak plugin module should have a " + PROPERTY_FHIR_BRIDGE_EHR_ADMIN_ENDPOINT_URL +
                    " property with an url value");
        }
        return endpointUrl;
    }

}
