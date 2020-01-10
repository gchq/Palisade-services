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
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.shared.Application;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import uk.gov.gchq.palisade.service.manager.config.RunnerConfiguration;

import java.io.IOException;
import java.util.AbstractMap.SimpleEntry;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Component("services-runner")
public class ServicesRunner implements ApplicationRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServicesRunner.class);

    @Autowired
    private EurekaClient eurekaClient;

    // Autowired through constructor
    private Map<String, ProcessBuilder> processBuilders;
    private Map<String, RunnerConfiguration> runnerConfiguration;

    public ServicesRunner(final Map<String, ProcessBuilder> runnerBuilders, final Map<String, RunnerConfiguration> runnerConfiguration) {
        this.processBuilders = runnerBuilders;
        this.runnerConfiguration = runnerConfiguration;
    }

    List<InstanceInfo> getRunningServices() {
        if (Objects.nonNull(eurekaClient)) {
            LOGGER.debug("Getting InstanceInfo from EurekaClient");
            return eurekaClient.getApplications().getRegisteredApplications().stream()
                    .map(Application::getInstances)
                    .flatMap(List::stream)
                    .collect(Collectors.toList());
        } else {
            LOGGER.debug("EurekaClient is null - is the discovery service running?");
            return Collections.emptyList();
        }
    }

    Map<String, ProcessBuilder> filterRunningServices(Map<String, ProcessBuilder> processBuilders) {
        Set<String> instanceNames = getRunningServices().stream().map(InstanceInfo::getAppName).collect(Collectors.toSet());
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
            Map<String, ProcessBuilder> filteredBuilders = filterRunningServices(processBuilders);

            // Start processes for each service configuration
            Map<String, Process> processes = runApplications(filteredBuilders);
        }
    }
}
