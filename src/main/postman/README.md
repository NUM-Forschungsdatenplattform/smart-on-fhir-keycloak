##Postman collections for tests and automating keycloak

### Smart on fhir postman collection
This postman collection contains various calls a SMART ON FHIR client could make to a SMART ON FHIR (SOF) endpoint. 

The current scope of this collection is limited to limited scope of proof of concept SOF application implementation in NUM transactional repository, i.e. a standalone patient app making calls to list and create Conditions

You should set the variables in the collection to their values so that calls can reach the fhir bridge and demographic services.

These calls should have an access token attached, so that SMART on FHIR calls can succeed. So the relevant Keycloak deployment URL should also be set at the collection/Authorisation level.

Here are the notes related to individual calls in this collection:

- Smart on fhir metadata

Retrieve the smart on fhir capabilities document from the FHIR endpoint. As per SOF spec, the authorisation server URL returned in this doc should be used to get tokens. Our collection does not do this, but this call shows how to call metadata endpoint for clients that do so.

- Create condition

Create a new condition. For this call to succeed under SOF, `subject.identifier.value` in the FHIR resource should have the identifier of the openEHR EHR of the patient, and `subject.identifier.reference` should have an URL referencing the patient resource with the correct patient resource id, matching that of patient. 
The ehr identifier value would be the keycloak user id (`sub` in access token) and the patient resource id is provided in the access token with `patient` claim. So when you get the access token, you'll have these values in place. These values are determined when a patient is registered as a user for a SOF client app.

- Update condition

Currently not supported by fhir bridge, so cannot be used.

- List Conditions

This request should be denied with a 403 response, since it does not include any criteria regarding the patient compartment (to which patient the Conditions belong to). This demonstrates the correct SOF behavior for the patient app profile.

- Create EHR

This call tests the endpoint used by the keycloak plugin during registration. An EHR will be created will be associated to the id provided as the query parameter.

- Get DEMOGRAPHIC patient resource

This call retrieves the patient resource from the demographic repository. Since it is making a call using the SOF token, only calls to retrieve the patient resource of the patient who acquired the token should succeed. This call represents calls from SOF apps made to demographic endpoint.

- Search conditions

Demonstrates how a FHIR resource can be retrieved by using the patient compartment of the resource as the query criteria. The query parameter should match the fhir reference to the patient who acquired the token as the user.

##Keycloak REST API collection

This collection contains calls that perform administrative tasks using REST API of Keycloak. 

These tasks are currently performed via the UI of Keycloak, but as requirements are clarified, NUM Transactional Platform should introduce its own user interface and facilities to perform these tasks, specific to applications to be used on the transtactional platform, such as SMART ON FHIR APPs.

The calls in this collection are not complete. More APIs need to be used to fully replicate the process performed from the UI. This effort will take place once requirements are clarified.