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

package uk.gov.gchq.palisade.service.launcher.runner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class ServicesRunner implements ApplicationRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServicesRunner.class);

    private Map<String, ProcessBuilder> processBuilders;

    public ServicesRunner(final Map<String, ProcessBuilder> processBuilders) {
        this.processBuilders = processBuilders;
    }

    Map<String, Process> launchApplications(final Map<String, ProcessBuilder> configurations) {
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
        // Start processes for each service configuration
        Map<String, Process> processes = launchApplications(processBuilders);
        LOGGER.info("Launched {} processes, waiting to join", processes.size());

        // Wait for retcodes
        Map<String, Integer> retCodes = joinProcesses(processes);
        LOGGER.info("Joined with retcodes: {}", retCodes.toString());
    }
}
