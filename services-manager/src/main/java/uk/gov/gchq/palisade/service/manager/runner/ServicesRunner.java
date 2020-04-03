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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.gov.gchq.palisade.service.manager.service.ManagedService;

import java.io.IOException;
import java.util.AbstractMap.SimpleEntry;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class ServicesRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServicesRunner.class);

    // Autowired through constructor
    private Map<String, ProcessBuilder> processBuilders;
    private Function<String, ManagedService> serviceProducer;

    public ServicesRunner(final Map<String, ProcessBuilder> processBuilders, final Function<String, ManagedService> serviceProducer) {
        this.processBuilders = processBuilders;
        this.serviceProducer = serviceProducer;
    }

    Map<String, Process> runApplications(final Map<String, ProcessBuilder> configurations) {
        return configurations.entrySet().stream()
                .map(e -> {
                    try {
                        LOGGER.info("Starting service: {}", e.getKey());
                        return new SimpleEntry<>(e.getKey(), e.getValue().start());
                    } catch (IOException ex) {
                        LOGGER.error("Error while starting service {}: {}", e.getKey(), ex.getMessage());
                        ex.printStackTrace();
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .peek(e -> LOGGER.debug("Running process: {}", e.toString()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public Map<String, List<Supplier<Boolean>>> run() {
        LOGGER.debug("Loaded ProcessBuilders: {}", processBuilders);

        // Start processes for each service configuration
        Map<String, Process> processes = runApplications(processBuilders);
        LOGGER.info("Started {} new processes", processes.size());

        return processes.entrySet().stream()
                .map(entry -> {
                    LinkedList<Supplier<Boolean>> indicators = new LinkedList<>();
                    indicators.addLast(() -> entry.getValue().isAlive());
                    indicators.addLast(() -> serviceProducer.apply(entry.getKey()).isHealthy());
                    return new SimpleImmutableEntry<>(entry.getKey(), indicators);
                })
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
