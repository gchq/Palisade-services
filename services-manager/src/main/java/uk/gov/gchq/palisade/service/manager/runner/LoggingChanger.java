/*
 * Copyright 2020 Crown Copyright
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.gchq.palisade.service.manager.runner;

import com.netflix.appinfo.InstanceInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import uk.gov.gchq.palisade.service.manager.config.ServiceConfiguration;

import java.util.AbstractMap.SimpleEntry;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component("logging-changer")
public class LoggingChanger extends EurekaUtils implements ApplicationRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingChanger.class);

    // Autowired through constructor
    private Map<String, ServiceConfiguration> loggingConfiguration;

    public LoggingChanger(final Map<String, ServiceConfiguration> loggingConfiguration) {
        this.loggingConfiguration = loggingConfiguration;
    }

    Map<InstanceInfo, ServiceConfiguration> mapInstancesToConfigs(final List<InstanceInfo> instances, final Map<String, ServiceConfiguration> configuration) {
        return instances.stream()
                .filter(instance -> configuration.containsKey(instance.getAppName().toLowerCase()))
                .map(instance -> new SimpleEntry<>(instance, configuration.get(instance.getAppName().toLowerCase())))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    Map<String, ResponseEntity<String>> postEachLevel(final InstanceInfo instance, final Map<String, HttpEntity<String>> entities) {
        return entities.entrySet().stream()
                .map(entry -> {
                    String endpoint = entry.getKey();
                    HttpEntity<String> entity = entry.getValue();
                    // Default spring actuator - this could be retrievable dynamically through the /actuators endpoint
                    String actuator = String.format("http://%s:%s/actuator/loggers/%s", instance.getIPAddr(), instance.getPort(), endpoint);
                    ResponseEntity<String> response = new RestTemplate().postForEntity(actuator, entity, String.class);
                    return new SimpleEntry<>(endpoint, response);
                })
                .peek(entry -> LOGGER.debug("{}: {}", entry.getKey(), entry.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    Map<InstanceInfo, Map<String, ResponseEntity<String>>> postEachInstance(final Map<InstanceInfo, ServiceConfiguration> instanceConfigs) {
        return instanceConfigs.entrySet().stream()
                .map(entry -> {
                    InstanceInfo instance = entry.getKey();
                    Map<String, HttpEntity<String>> entities = entry.getValue().getLoggingChangeEntities();
                    // There may exist multiple level changes per instance as well as multiple instances per service
                    return new SimpleEntry<>(instance, postEachLevel(instance, entities));
                })
                .peek(entry -> LOGGER.debug("{}: {}", entry.getKey(), entry.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    Boolean joinAllResponses(final Map<InstanceInfo, Map<String, ResponseEntity<String>>> responses) {
        return responses.entrySet().stream()
                .map(instanceEntry -> { // All requests across all services
                    InstanceInfo instance = instanceEntry.getKey();
                    Boolean instanceSuccess = instanceEntry.getValue().entrySet().stream()
                            .map(responseEntry -> { // All reqquests for a single service
                                String logger = responseEntry.getKey();
                                ResponseEntity<String> response = responseEntry.getValue();
                                int statusClass = response.getStatusCodeValue() / 100;
                                Boolean success = statusClass == 2;
                                // Log any errors that may have occurred
                                LOGGER.debug("Response from instance {} and logger {}:\n{}", instance, logger, response);
                                if (Boolean.FALSE.equals(success)) {
                                    LOGGER.warn("Instance {} and logger {} encountered errors while posting to actuator: status code was {}", instance, logger, response.getStatusCode());
                                }
                                return success;
                            })
                            .reduce(true, (x, y) -> x && y);
                    if (Boolean.FALSE.equals(instanceSuccess)) {
                        LOGGER.warn("Instance {} encountered errors while posting to actuators", instance);
                    }
                    return instanceSuccess;
                })
                .reduce(true, (x, y) -> x && y);
    }

    @Override
    public void run(final ApplicationArguments args) throws Exception {
        if (args.containsOption("logging")) {
            LOGGER.debug("Loaded LoggerConfiguration: {}", loggingConfiguration);

            // Get running services from eureka and map each instance to a logging configuration
            List<InstanceInfo> runningServices = getRunningServices();
            LOGGER.info("Discovered {} running services", runningServices.size());

            Map<InstanceInfo, ServiceConfiguration> instanceConfigurations = mapInstancesToConfigs(runningServices, loggingConfiguration);
            LOGGER.info("Mapped {} service instances to logging configurations", instanceConfigurations.size());

            // POST a JSON object to each service instance loggers actuator
            Map<InstanceInfo, Map<String, ResponseEntity<String>>> responses = postEachInstance(instanceConfigurations);
            Integer numLoggers = responses.values().stream().map(Map::size).reduce(0, Integer::sum);
            LOGGER.info("Posted {} logging changes to {} service instances", numLoggers, responses.size());

            // Wait until all POSTs are complete
            Boolean success = joinAllResponses(responses);

            if (Boolean.FALSE.equals(success)) {
                LOGGER.warn("Errors encountered while posting to actuators - check logs");
                System.exit(1);
            } else {
                LOGGER.info("Success");
                System.exit(0);
            }
        }
    }

}
