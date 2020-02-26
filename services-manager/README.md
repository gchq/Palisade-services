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
This file is under substitution in the build process - maven's `process-resources` stage will substitute executable(dot)jar for services-manager-0.4.0-SNAPSHOT-exec.jar
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
 * `--run` - run the configured services, spawning a new process for each entry
 * `--logging` - perform a change to the logging level of configured services by REST requests
 * `--config` - print out a human-readable view of the spring-boot configuration given to the services-manager
 
 
## Examples

### Starting Services
If services are already running, using the built-in profiles:  
 1. Start Eureka - `java -jar -Dspring.profiles.active=eureka services-manager-0.4.0-SNAPSHOT-exec.jar --run` 
    * Wait for Eureka to start up on `localhost:8083`
 3. Start all other services - `java -jar -Dspring.profiles.active=services services-manager-0.4.0-SNAPSHOT-exec.jar --run` 
    * Services should now begin registering with Eureka and appear on the dashboard
 4. Begin using Palisade (see [palisade-examples](https://github.com/gchq/Palisade-examples))  

### Enabling Debug Logging

#### At Start-Time
If services are not running, or debug logging is required from startup, using the built-in profiles:  
 1. Start Eureka as above
 2. Add the `debug` profile to the services runner - `java -jar -Dspring.profiles.active=services,debug services-manager-0.4.0-SNAPSHOT-exec.jar --run` 
    * Services should now log at `DEBUG` level from startup  
 
#### During Runtime
If services are already running, using the built-in profiles:  
 1. Make a POST to Spring logging actuators - `java -jar -Dspring.profiles.active=debug services-manager-0.4.0-SNAPSHOT-exec.jar --logging` 
    * Services should now begin logging at `DEBUG` level (note that this will not include past debug logs) 

### Creating a new Configuration
Take a look at the example configuration file:
```yaml
manager:
  # Search up path hierarchy for the root directory by name
  # This allows the services-manager to be less dependant on where it is located and where it was run from
  root: palisade-services

  services:
     # Example configuration for a service "my-service" with a single class under my.service.MainApplication
     # Where appropriate, each entry is formatted as "TAG: [VALUE] :: USAGE - DESCRIPTION"
    
     my-service:                                               # "spring.application.name=${my-service}" - tag for the service being managed, should match with the service's Spring Boot application name (in eureka)
      jar: my-service.jar                                     # "java -jar ${jar}" - executable service jar file with main entry point
       paths:                                                  # Additional (external) libraries to dynamically load at runtime (e.g. example library)
         - "/data/types.jar"                                   # "java -Dloader.path=${paths[0]},${paths[1]}"
       profiles:                                               # Spring Boot profiles to enable, comma-separated list
         - default                                             # "java -Dspring.profiles.active=${profiles[0]},${profiles[1]}"
       log: my-service.log                                     # "java [args] > ${log}" - logging output filepath, singleton filepath
       err: my-service.err                                     # "java [args] 2> ${err}" - error output filepath, singleton filepath
       level:                                                  # "java -Dlogging.level.${level.key}=${level.value}" - same format as spring's standard logging changes, classpath-loglevel map
         my.service.MainApplication: "INFO"                    # ALSO http POST address /actuator/loggers/${key}, body "configuredLevel=${value}" - classpath to change and logging level to change to
```
When testing your new configuration, you may find the config flag useful:
 1. Write a new configuration `application-mynewprofile.yaml`
 2. See what the services-manager has been given by Spring - run `java -jar -Dspring.profiles.active=mynewprofile services-manager-0.4.0-SNAPSHOT-exec.jar --config` and the Java object representing the configuration should be printed  
 3. Need more? Add the debug profile too - run `java -jar -Dspring.profiles.active=debug,mynewprofile services-manager-0.4.0-SNAPSHOT-exec.jar --config`  

## License

Palisade-Services is licensed under the [Apache 2.0 License](https://www.apache.org/licenses/LICENSE-2.0) and is covered by [Crown Copyright](https://www.nationalarchives.gov.uk/information-management/re-using-public-sector-information/copyright-and-re-use/crown-copyright/).


## Contributing
We welcome contributions to the project. Detailed information on our ways of working can be found [here](https://gchq.github.io/Palisade/doc/other/ways_of_working.html).


## FAQ

What versions of Java are supported? We are currently using Java 11.
