/*
 * Copyright 2018-2021 Crown Copyright
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

package uk.gov.gchq.palisade.service.manager.config;

import uk.gov.gchq.palisade.service.manager.common.Generated;
import uk.gov.gchq.palisade.service.manager.runner.TaskRunner;
import uk.gov.gchq.palisade.service.manager.service.ManagedService;

import java.io.File;
import java.util.AbstractMap.SimpleEntry;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringJoiner;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * A task is a collection of services.
 * Given a number of services to all start up under one task, collect them and provide methods to work with them
 * Mostly just a spring yaml 'boilerplate' with some helper methods
 */
public class TaskConfiguration {
    private final Map<String, ServiceConfiguration> services;

    /**
     * Task Configuration constructor that populates the services map with a stream of services and serviceConfigurations
     *
     * @param services             a list of services that will get started under a task
     * @param serviceConfiguration a Map of Strings and ServiceConfigurations containing information related to each service
     */
    public TaskConfiguration(final List<String> services, final Map<String, ServiceConfiguration> serviceConfiguration) {
        this.services = serviceConfiguration.entrySet().stream()
                .filter(entry -> services.contains(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     * Map the loaded ServiceName - ServiceConfiguration collection to ServiceName - ProcessBuilder
     * Each ProcessBuilder may be .start()ed to start a new JVM running the given service as configured
     *
     * @param builderDirectory the working directory for the spawned processes
     * @return a map from serviceName to a ProcessBuilder for the configured service
     */
    public Map<String, ProcessBuilder> getProcessBuilders(final File builderDirectory) {
        return services.entrySet().stream()
                .map((Entry<String, ServiceConfiguration> e) -> {
                    ServiceConfiguration config = e.getValue();
                    ProcessBuilder builder = config.getProcessBuilder();
                    builder.directory(builderDirectory);
                    return new SimpleEntry<>(e.getKey(), builder);
                })
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Generated
    public Map<String, ServiceConfiguration> getServices() {
        return services;
    }

    /**
     * Run this TaskConfiguration as a TaskRunner, simply .start()ing all configured services and waiting until healthy or halted
     *
     * @param rootDir         the working directory for the spawned processes
     * @param serviceProducer a mapping from service names to REST clients for the given service name
     * @return a map of service names paired with a collection of indicators as to whether the service is 'done' for some metric (healthy, halted, etc)
     */
    public Map<String, List<Supplier<Boolean>>> runTask(final File rootDir, final Function<String, ManagedService> serviceProducer) {
        return new TaskRunner(getProcessBuilders(rootDir), serviceProducer).run();
    }

    @Override
    @Generated
    public String toString() {
        return new StringJoiner(", ", TaskConfiguration.class.getSimpleName() + "[", "]")
                .add("services=" + services)
                .add(super.toString())
                .toString();
    }


}
