/*
 * Copyright 2019 Crown Copyright
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
import org.springframework.stereotype.Component;

import uk.gov.gchq.palisade.service.manager.config.RunnerConfiguration;

import java.io.IOException;
import java.util.AbstractMap.SimpleEntry;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Component("services-runner")
public class ServicesRunner extends EurekaUtils implements ApplicationRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServicesRunner.class);

    // Autowired through constructor
    private Map<String, ProcessBuilder> processBuilders;
    private Map<String, RunnerConfiguration> runnerConfiguration;

    public ServicesRunner(final Map<String, ProcessBuilder> runnerBuilders, final Map<String, RunnerConfiguration> runnerConfiguration) {
        this.processBuilders = runnerBuilders;
        this.runnerConfiguration = runnerConfiguration;
    }

    Map<String, ProcessBuilder> filterRunningServices(Map<String, ProcessBuilder> processBuilders, List<InstanceInfo> runningServices) {
        Set<String> instanceNames = runningServices.stream().map(InstanceInfo::getAppName).collect(Collectors.toSet());
        return processBuilders.entrySet().stream()
                .filter(entry -> {
                    Boolean exists = instanceNames.contains(entry.getKey());
                    if (exists) {
                        LOGGER.warn("Eureka already has registered instance named {} - excluding from run", entry.getKey());
                    }
                    return exists;
                })
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    Map<String, Process> runApplications(final Map<String, ProcessBuilder> configurations) {
        return configurations.entrySet().stream()
                .map(e -> {
                    try {
                        LOGGER.info("Started service: {}", e.getKey());
                        LOGGER.debug("Service {} started with command: {}", e.getKey(), e.getValue().command());
                        return new SimpleEntry<>(e.getKey(), e.getValue().start());
                    } catch (IOException ex) {
                        LOGGER.error("Error while starting service {}: {}", e.getKey(), ex.getMessage());
                        ex.printStackTrace();
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(SimpleEntry::getKey, SimpleEntry::getValue));
    }

    Map<String, Integer> joinProcesses(final Map<String, Process> processes) {
        return processes.entrySet().stream()
                .map(e -> {
                    try {
                        return new SimpleEntry<>(e.getKey(), e.getValue().waitFor());
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(SimpleEntry::getKey, SimpleEntry::getValue));
    }

    @Override
    public void run(final ApplicationArguments args) throws Exception {
        if (args.containsOption("run")) {
            LOGGER.info("Loaded RunnerConfiguration: {}", runnerConfiguration);

            // Get running services from eureka and warn-don't-start any that are already running
            List<InstanceInfo> runningServices = getRunningServices();
            Map<String, ProcessBuilder> filteredBuilders = filterRunningServices(processBuilders, runningServices);

            // Start processes for each service configuration
            Map<String, Process> processes = runApplications(filteredBuilders);
        }
    }
}
