<!---
Copyright 2020 Crown Copyright

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
--->

<!---
The contents of this file are under substitution in the build process - maven's `process-resources` stage will substitute executable(dot)jar for this: executable.jar
The source for this file can be found at `services-manager/src/resources/doc/README-TEMPLATE.md`
--->

# <img src="../logos/logo.svg" width="180">

### Palisade Services Manager



## Documentation

The documentation for the latest release can be found [here](https://gchq.github.io/Palisade).



## Getting started

The Palisade Services Manager is a configuration-driven process-spawner and REST-client with particular considerations to assist in managing multiple Palisade services running on local JVMs.

The manager is designed to be used by defining a collection of SpringBoot configuration files:
 * One set for the manager itself - this defines how it should start up further services or reconfigure those already running
 * One set for each service - these are discovered through the above configuration and passed on as an argument to the service jar
    * These wll all already be located in each service's `resources` directory
 
The manager works in several modes, operated by the use of several flags:
 * `--run` - run the *schedule*, in turn running a sequence of *tasks*, with each task running a collection of *services*
 * `--shutdown` - shutdown the services in the opposite order to how they were started in the schedule
 * `--loggers` - perform a change to the logging level of configured services by REST requests (POST /actuator/loggers/*)
 * `--config` - print out a human-readable view of the spring-boot configuration given to the services-manager, useful for debugging



## Examples

### Starting Services
Using the built-in profiles, the services-manager can be used to perform a number of common tasks, starting the services in different environments for different use-cases:  
 * Services using Eureka 
   * First start the discovery-service in a static-like way - `java -jar -Dspring.profiles.active=discovery executable.jar --manager.mode=run` 
   * Once the manager is finished running, check the eureka dashboard at `localhost:8083` 
   * Next start all other palisade services using the discovery-service for service discovery - `java -jar -Dspring.profiles.active=eureka executable.jar --manager.mode=run` 
   * These two commands will only exit once all services are ready, so the two `java -jar ...` commands can be chained together as `java -jar .. && java -jar ...`
 * Services using static ports
   * Just a single command - `java -jar -Dspring.profiles.active=static executable.jar --manager.mode=run` 
   * No eureka dashboard here, but take a look at the /actuator endpoints for some metadata 
   * By default, palisade-service will be at `localhost:8084` and data-service will be at `localhost:8082` 
 * Pre-populated Palisade example (see [palisade-examples](https://github.com/gchq/Palisade-examples)) 
   * For even more automation, the start-services (above) -> configure-services (example) -> run-example (example) steps can be performed in one automated step 
     * First start the discovery-service as above - `java -jar -Dspring.profiles.active=discovery executable.jar --manager.mode=run`   
     Then choose any ***one*** of the following:
     * Start up services with pre-populated example data, run using the *examplelibs* profile - `java -jar -Dspring.profiles.active=examplelibs executable.jar --manager.mode=run`
     * Do all the above, then run the rest-example, run using the *examplemodel* profile - `java -jar -Dspring.profiles.active=examplemodel executable.jar --manager.mode=run`
     * Do all the above, then run the performance tests, run using the *exampleperf* profile - `java -jar -Dspring.profiles.active=exampleperf executable.jar --manager.mode=run`

**The choice here between `eureka` or `static` profiles will be referred to unilaterally as the `environment` profile - make sure to substitute as appropriate**  



### Enabling Debug Logging

#### At Start-Time
If services are not running, or debug logging is required from startup, using the built-in profiles:  
 * *For the appropriate `environment`*, add the `debug` profile during the manager's run command - `java -jar -Dspring.profiles.active=environment,debug executable.jar --manager.mode=run` 
 * The `logging.level.uk.gov.gchq.palisade=DEBUG` configuration value will be set for all services at start-time 
   * Services should now log at `DEBUG` level from startup  

#### During Runtime
If services are already running, using the built-in profiles:  
 * *For the appropriate `environment`*, add the `debug` profile and use the manager's logging command - `java -jar -Dspring.profiles.active=environment,debug executable.jar --manager.mode=loggers`
 * A POST request will be made to Spring logging actuators 
   * Running services should now begin logging at `DEBUG` level (note that this will not include past debug log content, only debug messages created from now onwards)  



### Creating a new Configuration
Take a look at the [default configuration file](/services-manager/src/main/resources/application.yaml)

When testing your new configuration, you may find the config flag useful:
 1. Write a new configuration `application-mynewprofile.yaml`
 1. See what the services-manager has been given by Spring - `java -jar -Dspring.profiles.active=mynewprofile executable.jar --manager.mode=config` (the Java object representing the configuration should be printed to screen)  
 1. Need a little more? Also add the `debug` profile - `java -jar -Dspring.profiles.active=mynewprofile,debug executable.jar --manager.mode=config`  



## License

Palisade-Services is licensed under the [Apache 2.0 License](https://www.apache.org/licenses/LICENSE-2.0) and is covered by [Crown Copyright](https://www.nationalarchives.gov.uk/information-management/re-using-public-sector-information/copyright-and-re-use/crown-copyright/).
