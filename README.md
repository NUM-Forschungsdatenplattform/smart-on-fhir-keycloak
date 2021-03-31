### Introduction

This repository contains a Keycloak plugin and its runtime dependencies to enable SMART on FHIR behaviour for Keycloak.

Both the plugin and its dependencies are deployed as file based Wildfly modules. The Keycloak plugin adds the following  items to a  Keycloak distribution:
1. A custom authenticator
2. An event listener
3. A scope mapper

These items allow creating custom authentication flows in Keycloak, which in turn allows Keycloak to support functionality defined by [SMART on FHIR](https://smarthealthit.org/) functionality. 
Currently, the functionality of the Keycloak plugin has a limited scope: it supports a simple standalone Patient App launch scenario. However, the items provided in the plugin are capable of supporting more advanced scenarios from the SMART on FHIR specification by extending their functionality or adding more comprehensive implementations of the core functionality provided by these items. 
Note that the behaviour of this plugin assumes a particular architecture and it is meant to work with Demographic server and Fhir Bridge components of the [NUM platform](https://github.com/NUM-Forschungsdatenplattform)
### Installation
Installing this plugin consists of deploying it as a file based JBoss module, along with another module which represents the runtime dependencies this plugin requires to run. The following  steps describe how these two modules can be deployed. 
#### Building and deploying the plugin as a JBoss module
The plugin is developed with Java 11 under Ubuntu Linux, using Keycloak 11.0.2 and 11.0.3 as the deployment target. 
To build the code in this repo, run:

```mvn  package```

This will produce keycloak-plugin-<VERSION>-SNAPSHOT.jar under the target directory. This jar must be copied to a particular location in your standalone Keycloak distribution along with a ```module.xml``` file. This file is included in src/main/jboss-deployment/keycloak folder in this repository. Make sure that the path value in ```<resource-root path="keycloak-plugin-<VERSION>-SNAPSHOT.jar"/>``` matches the name of the jar created from the build. 

Create a series of directories under <keycloak directory>/modules matching the name in the module.xml, as in ```.../modules/de/vita/num/smart/keycloak-plugin/main``` note that you must add a main directory after the components of the name are created as directories.

Copy the build output jar and module.xml files to this directory. 

#### Setting JBoss module properties
The module.xml files are used to set properties used by the Keycloak plugin. Currently, only the module.xml for the plugin contains ```properties``` which define the URLS for Demographic and Fhir Bridge components.
Modify the following properties in the module.xml for the keycloak plugin (not the one for dependencies) according to actual URLs in the deployment environment:
```
<properties>
  <property name="demographic.base.url" value="http://localhost:8082/fhir"/>
  <property name="fhirbridge.ehradmin.endpoint.url" value="http://localhost:8888/fhir-bridge/ehradmin"/>
</properties>
```


#### Deploying the plugin dependencies as a Jboss Module
The plugin uses the [HAPI FHIR](https://hapifhir.io/) libraries to access Demographic service of NUM platform so these libraries must be available as a JBoss module under the Keycloak distribution.

Just like you did for the Keycloak plugin above, create the following directory structure under your modules directory:

```.../de/vita/num/smart/hapi/fhir/4/main/```
Then copy the ```module.xml``` file under  ```src/main/jboss-deployment/hapi-fhir``` folder in this repository to the main directory you just created. Then copy jars under ```src/main/jboss-deployment/hapi-fhir/jars``` to the main directory as well.
#### Registering the JBoss modules
Keycloak will load the two modules we created above only if they're registered in the ```standalone.xml``` file under ```standalone/configuration``` path in your Keycloak distribution. Modify the ```subsystem``` section in this file having ```<web-context>auth</web-context>``` as follows:
```
<providers>
    <provider>classpath:${jboss.home.dir}/providers/*</provider>
    <provider>
        module:de.vita.num.smart.keycloak-plugin
    </provider>
    <provider>
        module:de.vita.num.smart.hapi.fhir.4
    </provider>
</providers>
```
We now registered the plugins we deployed, so that they'll be loaded when Keycloak starts. Keycloak must be restarted when any changes to these plugins are deployed.

### Configuring Keycloak for a Smart on FHIR Patient App
With the plugins in place, we can now create a custom authentication flow for our Patient App.

- Configure email

    We want users to register with Keycloak in order to start using their Smart on Fhir Patient app(s) when they launch their app. Enabling registration requires configuring email, so configure an email account for Keycloak to use. 
- Enable Login
    
    Turn ```User Registration``` on from Realm settings.
- Create Client Scopes

    Create ```launch/patient``` client scope from Client Scopes section (protocol: openid-connect, Include in Token scope ON). After it is created, go to Mappers tab and Create a new mapper. This mapper should have type ```Client Session Note``` Set value of Client Session Note parameter to ```launch/patient``` and Token Claim name to ```patient``` Claim Json type should be String. Add to Id Token OFF, Add to Access Token ON
    
    Now create ```patient/*.*``` and ```openid``` scopes as well, but these do not have any mappers.

- Create a custom Authentication flow

    From Authentication section, create a copy of Browser flow (for example SMART Browser). Then modify this new flow: add an execution (from Actions) under Browser Forms. Provider type: ```Smart on Fhir Patient Selector For Patient App``` This execution should be the last step under Browser Forms, at the same level with Username Password Form. Mark this step as ```REQUIRED```

- Add a custom Event listener

    From Events section, go to Config tab and add ```SMART_on_FHIR_Event_Listener``` to the list of active event listeners  

### Creating a SMART ON FHIR APP on Keycloak
After the steps above, we now have a custom Authentication flow. Create a client suitable for an SPA (public) and go to Authentication Flow Overrides. For Browser Flow, select SMART Browser (or whatever you named the custom flow you created above). 

Go to client scopes of this app and make openid a default scope. Add launch/patient and patient/*.* scopes as optional.

Following these steps, your app should now use the Keycloak plugin. Whenever a user attempts to login, they'll initially create a user. During the user creation, Keycloak plugin will make calls to demographic server and fhir bridge services to create a patient fhir resource and an openEHR EHR for the newly registered user, and patient resource id will be included in all the access tokens created after successful login.
### Creating a Test Client for SMART ON FHIR on Keycloak
You may need a non-browser client such as Postman or Insomnia to test this plugin. In that case, this client should be registered as an OAuth client on Keycloak. The following settings are required to register such a client:

- Client ID: Sof-test
- Enabled: ON
- Client protocol: openid-connect
- Access type: confidential
- Standard flow enabled: OFF
- Direct Access Grants Enabled: ON
- Service Accounts Enabled: ON

Settings not mentioned above are either OFF or empty or left with their default values.

Since this client won't be getting access tokens via a login form, it won't need to override browser flow, but it will need to override Direct Grant Flow. For this to happen, we need a customised Direct Grant Flow. Just like the above browser app approach, make a copy of the built in Direct Grant (from under Authentication). Add a custom execution in the same way described above, which should be at the root level but at the bottom of executions. This execution should have REQUIRED set to requirement. 

Once there are in place, you can acquire tokens from Postman, using Password Credentials Grant type.

### Postman collection for calls to SOF endpoint

The *postman* folder under *src/main/* contains a Postman collection with sample calls from Postman, mimicking a SOF app. Please see the dedicated README in the *postman* folder re explanation of these calls. 

### Acknowledgements
[Igia project](https://igia.github.io/) provides a great amount of valuable know-how and this plugin started based on some of the work done in their Keycloak plugin. Where relevant, the source code level licence statements are kept in place if some code in this repository is based on code written by the Igia team, which is MPL 2.0 with a Healthcare disclaimer. This license is included in this repository, named MPL-LICENSE-HCD.txt

[HAPI FHIR project](https://hapifhir.io/) is a substantial  implementation of FHIR which is used by other components of the NUM platform as well. This plugin uses the libraries provided by this project while making calls to (FHIR) demographic services. These libraries are included in this repository and they are also subject to the Apache 2.0 license this plugin is distributed under.




      