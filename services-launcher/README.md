<!---
Copyright 2019 Crown Copyright

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


# <img src="../logos/logo.svg" width="180">

### Palisade Services Launcher

## Documentation

The documentation for the latest release can be found [here](https://gchq.github.io/Palisade).

## Getting started

The Palisade Services Launcher is a configuration-driven process runner with particular considerations to assist in running multiple Palisade services in a local JVM.

The launcher is designed to be used by defining a collection of SpringBoot configuration files:
 * One for the launcher itself - this defines how it should start up further services
 * One for each service - these are discovered through the above configuration and passed on as an argument to the service jar
 
 The launcher configuration further defines two types of configurations:
 * A list of configurations for each service to be run - required fields are:
    * target (SpringBoot-enabled JAR to run)
    * config (directory to pass to `-Dspring.config.location=<...>`)
    * profiles (profiles to pass to `-Dspring.profiles.active=<...>`, `default` for JAR default profile)
    * log (can be `/dev/null` for no logging)
 * One default configuration - for any required field not defined in the services, its value is deduced from the defaults:
    * The `root` directory is the first directory matching the field value after traversing up the directory tree from the working directory
       * Running the launcher from any subdirectory of `palisade-service` does not change application behaviour
    * A further string substitution is performed on any missing required field - any instance of the keyword `SERVICE` for the appropriate service name defined in the `name` field

## Examples

### Default Launcher Configuration
By default, the launcher configuration looks as follows:
```$xslt
launcher:
  default-service:
    root: palisade-services
    name: default
    target: SERVICE/target/SERVICE-0.4.0-SNAPSHOT-exec.jar
    config: SERVICE/classes
    profiles: default
    log: SERVICE.log
  services:
    -
      name: audit-service
   ...
```
When loaded, instances of the keyword `SERVICE` are substituted per-service for that service's name.
In this case, the configuration is equivalent to `java -jar audit-service/target/audit-service-0.4.0-SNAPSHOT-exec.jar > audit-service.log 2 &> 1`

This default configuration is enough to launch all Palisade services given the existing directory structure and enabling/disabling services can be done using the command-line arguments:  
`java -jar services-launcher.jar --enable=example-service --enable=another-service --disable=unneccessary-service`

Each service enabled from the command line arguments is instantiated the same as the above services are - a overriden name applied to the default config.

### Disabling Logging
The default configuration pipes stdout and stderr to log files for each service.
To disable this, a single change is required to the configuration which can be accomplished using the `nologs` profile provided in `application-nologs.yaml`.

Execute:  
`java -jar -Dspring.profiles.active=nologs,default services-launcher.jar`

Launcher's `application-nologs.yaml` (provided in the jar and available in services-launcher/target/classes):
```$xslt
launcher:
  default-service:
    log: /dev/null
```

### Enabling Eureka - Per-Service
By default across Palisade services, Eureka service discovery is disabled.
A common use-case may then be to start _some_ services (here, just the audit-service) with Eureka enabled, while leaving others unmodified.

Execute:  
`java -jar -Dspring.profiles.active=with-eureka,default services-launcher.jar`

Launcher's `application-with-eureka.yaml`:
```$xslt
launcher:
  services:
    -
      name: audit-service
      profiles: eureka-service,default
    -
      name: discovery-service
   ...
```

Service's `application-eureka-service.yaml` (either in the local directory or for each service in SERVICE/target/classes):
```$xslt
eureka:
  client:
    enabled: true
```

_Note that the discovery service has Eureka enabled by default, so does not need a configuration file defined._  

### Enabling Eureka - Globally
Similarly to the above, a deployed system may require Eureka enabled for _all_ services, as well as per-service configurations for some services.

Execute:  
`java -jar -Dspring.profiles.active=with-eureka,default services-launcher.jar`

Launcher's `application-with-eureka.yaml`:
```$xslt
launcher:
  default-service:
    profiles: eureka-service,default
  services:
    -
      name: audit-service
      profiles: audit,eureka-service,default
    -
      name: discovery-service
   ...
```

Service's `application-eureka-service.yaml`:
```$xslt
eureka:
  client:
    enabled: true
```

Audit Service's `application-audit.yaml`:
```$xslt
# further configuration here
```


## License

Palisade-Common is licensed under the [Apache 2.0 License](https://www.apache.org/licenses/LICENSE-2.0) and is covered by [Crown Copyright](https://www.nationalarchives.gov.uk/information-management/re-using-public-sector-information/copyright-and-re-use/crown-copyright/).


## Contributing
We welcome contributions to the project. Detailed information on our ways of working can be found [here](https://gchq.github.io/Palisade/doc/other/ways_of_working.html).


## FAQ

What versions of Java are supported? We are currently using Java 11.
