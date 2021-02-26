<!---
Copyright 2018-2021 Crown Copyright

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
    * These are located in the service-manager's `resources` directory
 * One set for each service - these are discovered through the above configuration and passed on as an argument to the service jar
    * These are located in each service's `resources` directory
 
The manager works in several modes, operated by the use of several flags:
 * `--manager.mode=run` - run the *schedule*, in turn running a sequence of *tasks*, with each task running a collection of *services*
 * `--manager.mode=shutdown` - shutdown the services in the opposite order to how they were started in the schedule
 * `--manager.mode=loggers` - perform a change to the logging level of configured services by REST requests (POST /actuator/loggers/*)
 * `--manager.mode=config` - print out a human-readable view of the spring-boot configuration given to the services-manager, useful for debugging

When set to the `run` mode, the manager breaks down possible jobs as follows:

#### `--manager.mode=run`
Complete the configured *schedule*.

#### `--manager.schedule=...`
An ordered list of *task names*, where each task is started and waited upon until completion.
The schedule will fail-fast if any task fails, and is only completed successfully once all tasks complete successfully.

#### `--manager.tasks=...`
A map of *task names* to unordered collections of *service names*.
The task will fail if any service halts with failure, and is completed successfully once every service is healthy or halted with success. 

#### `--manager.services=...`
A map of *service names* to a number of configuration settings, forming a convenience wrapper around a ProcessBuilder that will spawn a daemonised `java -jar` process.
To complete successfully, a service either halts with success or responds `200 OK {status=UP}` to a health check (`GET http://<service-name>/actuators/health`).



## Examples

### Starting Services (`--manager.mode=run` / `--manager.mode=shutdown`)
Using the built-in profiles, the services-manager can be used to perform a number of common tasks, starting the services in different environments for different use-cases:  

#### `static` - Simple setup with static 808x port numbers
```bash
java -jar -Dspring.profiles.active=static target/executable.jar
```
 * By default, palisade-service will be at `localhost:8084` and data-service will be at `localhost:8082`
 
 
#### `example-libs` - Pre-populated Palisade example (see [example-library](https://github.com/gchq/Palisade-examples/tree/develop/example-library))
```bash
java -jar -Dspring.profiles.active=example-libs target/executable.jar
```
 * Services will start up with their cache/persistence-store prepopulated with example data


#### `example-runner` - Automated execution of Palisade client on example data (see [example-runner](https://github.com/gchq/Palisade-examples/tree/develop/example-runner))
```bash
java -jar -Dspring.profiles.active=example-runner target/executable.jar
```
 * Services will start up with their cache/persistence-store prepopulated with example data
 * The rest-example will run once all services have started
 * Check `rest-example.log` for output data

The data used in this example comes checked-in to the repo and does not need generating
 
 
#### `example-perf` - Automated execution of Palisade performance tests on example data (see [performance](https://github.com/gchq/Palisade-examples/tree/develop/performance))
```bash
java -jar -Dspring.profiles.active=example-perf target/executable.jar
```
 * Services will start up with their cache/persistence-store prepopulated with example data
 * The performance-test will run once all services have started
 * Check `performance-test.log` for output data
 
The data used in this example contains numerous large files, as a result they do not come checked-in with the repo.
Instead, they must be generated before running the performance tests.

Either enable generation of performance test data as part of the services-manager `example-perf` configuration:
 * Change the above command to include the (previously unused) `performance-create-task`:
    ```bash
    java -jar -Dspring.profiles.active=example-perf target/executable.jar --manager.schedule=performance-create-task,palisade-task,performance-test-task
    ```
Or manually generate the data:  
 * From the [Palisade-examples](https://github.com/gchq/Palisade-examples/) directory, run the following command:
    ```bash
    java -jar performance/target/performance-*-exec.jar --performance.action=create
    ```



### Changing Logging Levels / Enabling Debug Logging (`--manager.mode=loggers`)

#### At Start-Time
If services are not running, or debug logging is required from startup, using the built-in profiles:  
 * Add the `debug` profile during the manager's run command - `java -jar -Dspring.profiles.active=static,debug target/executable.jar --manager.mode=run` 
 * The `logging.level.uk.gov.gchq.palisade=DEBUG` configuration value will be set for all services at start-time 
   * Services should now log at `DEBUG` level from startup  

#### During Runtime
If services are already running, using the built-in profiles:  
 * Add the `debug` profile and use the manager's logging command - `java -jar -Dspring.profiles.active=static,debug target/executable.jar --manager.mode=loggers`
 * A POST request will be made to Spring logging actuators 
   * Running services should now begin logging at `DEBUG` level (note that this will not include past debug log content, only debug messages created from now onwards)  


### Creating a new Configuration (`--manager.mode=config`)
Take a look at the [default configuration file](/services-manager/src/main/resources/application.yaml)  
When testing your new configuration, you may find the config flag useful:
 1. Write a new configuration `application-mynewprofile.yaml`
 1. See what the services-manager has been given by Spring - `java -jar -Dspring.profiles.active=mynewprofile target/executable.jar --manager.mode=config` (the Java object representing the configuration should be printed to screen)  
 1. Need a little more? Also add the `debug` profile - `java -jar -Dspring.profiles.active=mynewprofile,debug target/executable.jar --manager.mode=config`  



## License

Palisade-Services is licensed under the [Apache 2.0 License](https://www.apache.org/licenses/LICENSE-2.0) and is covered by [Crown Copyright](https://www.nationalarchives.gov.uk/information-management/re-using-public-sector-information/copyright-and-re-use/crown-copyright/).
