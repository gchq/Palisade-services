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

package uk.gov.gchq.palisade.service.manager.config;

import uk.gov.gchq.palisade.Generated;
import uk.gov.gchq.palisade.service.manager.runner.TaskRunner;
import uk.gov.gchq.palisade.service.manager.service.ManagedService;

import java.io.File;
import java.util.AbstractMap.SimpleEntry;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class TaskConfiguration {
    private final Map<String, ServiceConfiguration> services;

    public TaskConfiguration(final List<String> services, final Map<String, ServiceConfiguration> serviceConfiguration) {
        this.services = serviceConfiguration.entrySet().stream()
                .filter(entry -> services.contains(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public Map<String, ProcessBuilder> getProcessBuilders(final File builderDirectory) {
        return services.entrySet().stream()
                .map(e -> {
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
