package de.vita.num.smart.authentication;

import org.jboss.modules.Module;

public class ModuleConfiguration {

    //should match property name in module.xml
    public static final String PROPERTY_DEMOGRAPHIC_BASE_URL = "demographic.base.url";

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

}
