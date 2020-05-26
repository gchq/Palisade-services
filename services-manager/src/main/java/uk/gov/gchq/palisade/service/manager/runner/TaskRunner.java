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

public class TaskRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(TaskRunner.class);

    private Map<String, ProcessBuilder> processBuilders;
    private Function<String, ManagedService> serviceProducer;

    public TaskRunner(final Map<String, ProcessBuilder> processBuilders, final Function<String, ManagedService> serviceProducer) {
        this.processBuilders = processBuilders;
        this.serviceProducer = serviceProducer;
    }

    Map<String, Process> runServices() {
        return processBuilders.entrySet().stream()
                .map(entry -> {
                    try {
                        LOGGER.info("Starting {}", entry.getKey());
                        return new SimpleEntry<>(entry.getKey(), entry.getValue().start());
                    } catch (IOException ex) {
                        LOGGER.error("Error while starting service {}: {}", entry.getKey(), ex.getMessage());
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public Map<String, List<Supplier<Boolean>>> run() {
        // Start processes for each service configuration
        Map<String, Process> processes = runServices();

        return processes.entrySet().stream()
                .map(entry -> {
                    LinkedList<Supplier<Boolean>> indicators = new LinkedList<>();
                    indicators.addLast(() -> {
                        boolean alive = entry.getValue().isAlive();
                        LOGGER.info("Process for {} is {}", entry.getKey(), alive ? "RUNNING" : "HALTED");
                        return !alive;
                    });
                    indicators.addLast(() -> {
                        boolean healthy = serviceProducer.apply(entry.getKey()).isHealthy();
                        LOGGER.info("Health for {} is {}", entry.getKey(), healthy ? "UP" : "DOWN");
                        return healthy;
                    });
                    return new SimpleImmutableEntry<>(entry.getKey(), indicators);
                })
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
