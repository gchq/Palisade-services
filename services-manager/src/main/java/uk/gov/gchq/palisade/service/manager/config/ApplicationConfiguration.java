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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import uk.gov.gchq.palisade.Generated;
import uk.gov.gchq.palisade.service.manager.runner.ConfigPrinter;
import uk.gov.gchq.palisade.service.manager.runner.LoggingBouncer;
import uk.gov.gchq.palisade.service.manager.runner.ScheduleRunner;
import uk.gov.gchq.palisade.service.manager.runner.ScheduleShutdown;
import uk.gov.gchq.palisade.service.manager.service.ManagedService;
import uk.gov.gchq.palisade.service.manager.web.ManagedClient;

import java.io.File;
import java.net.URI;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

@Configuration
@EnableConfigurationProperties
@EnableAutoConfiguration
public class ApplicationConfiguration {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationConfiguration.class);

    @Bean
    @ConfigurationProperties(prefix = "manager")
    ManagerConfiguration managerConfiguration() {
        return new ManagerConfiguration();
    }

    @Bean
    @ConfigurationProperties(prefix = "web")
    public WebConfiguration webConfiguration() {
        return new WebConfiguration();
    }

    @Bean
    public ApplicationRunner managerApplicationRunner(final ManagerConfiguration managerConfiguration, final Function<String, ManagedService> serviceProducer) {
        Runnable runner;
        switch (managerConfiguration.getMode()) {
            case RUN:
                runner = new ScheduleRunner(managerConfiguration, serviceProducer);
                break;
            case SHUTDOWN:
                runner = new ScheduleShutdown(managerConfiguration, serviceProducer);
                break;
            case LOGGERS:
                runner = new LoggingBouncer(managerConfiguration, serviceProducer);
                break;
            case CONFIG:
            default:
                runner = new ConfigPrinter(managerConfiguration);
                break;
        }
        LOGGER.info("Constructed runner for {} mode: {}", managerConfiguration.getMode(), runner);

        return args -> {
            LOGGER.info("Running runner for manager: {}", runner);
            runner.run();
            System.exit(0);
        };
    }

    @Bean("managedServiceProducer")
    public Function<String, ManagedService> managedServiceProducer(final ManagedClient client, final WebConfiguration clientConfig) {
        return serviceName -> {
            Supplier<Collection<URI>> uriSupplier = () -> {
                Collection<URI> clientUris = clientConfig.getClientUri(serviceName);
                LOGGER.debug("Service {} has client uris {}", serviceName, clientUris);
                return clientUris;
            };
            return new ManagedService(client, uriSupplier);
        };
    }

    // === Yaml things ===

    // Intentionally-used inner-class
    // Due to the nested type in the yaml (services :: String -> ServiceConfiguration), both the ManagerConfiguration
    // and ServiceConfiguration must be known to Spring. The easiest way to achieve this is an inner-class in this
    // @Configuration rather than messing around with @EnableConfigurationProperties({...}) and @ConfigurationProperties
    // annotations.
    // Using this approach, all classes can remain unannotated.
    public static class ManagerConfiguration {
        private String root;
        private String mode;
        private List<String> schedule = new LinkedList<>();
        private Map<String, List<String>> tasks = new HashMap<>();
        private Map<String, ServiceConfiguration> services = new HashMap<>();

        public File getRoot() {
            File parent = new File(".").getAbsoluteFile();
            while (parent != null && !root.equals(parent.getName())) {
                parent = parent.getParentFile();
            }
            return parent;
        }

        @Generated
        public void setRoot(final String root) {
            requireNonNull(root);
            this.root = root;
        }

        public ManagerMode getMode() {
            return ManagerMode.valueOf(mode.toUpperCase());
        }

        @Generated
        public void setMode(final String mode) {
            requireNonNull(mode);
            this.mode = mode;
        }

        public List<Map.Entry<String, TaskConfiguration>> getSchedule() {
            return schedule.stream()
                    .map(task -> new SimpleImmutableEntry<>(task, getTasks().get(task)))
                    .collect(Collectors.toList());
        }

        @Generated
        public void setSchedule(final List<String> schedule) {
            requireNonNull(schedule);
            this.schedule = schedule;
        }

        public Map<String, TaskConfiguration> getTasks() {
            return tasks.entrySet().stream()
                    .map(taskEntry -> {
                        try {
                            LOGGER.debug("Processing task :: {}", taskEntry);
                            return new SimpleImmutableEntry<>(taskEntry.getKey(), new TaskConfiguration(taskEntry.getValue(), getServices()));
                        } catch (Exception e) {
                            LOGGER.error("An error occurred: ", e);
                            System.exit(-1);
                            return null;
                        }
                    })
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }

        @Generated
        public void setTasks(final Map<String, List<String>> tasks) {
            requireNonNull(tasks);
            this.tasks = tasks;
        }

        @Generated
        public Map<String, ServiceConfiguration> getServices() {
            return services;
        }

        @Generated
        public void setServices(final Map<String, ServiceConfiguration> services) {
            requireNonNull(services);
            this.services = services;
        }

        @Override
        @Generated
        public String toString() {
            return new StringJoiner(", ", ManagerConfiguration.class.getSimpleName() + "[", "\n]")
                    .add("\n\troot='" + root + "'")
                    .add("\n\tmode='" + mode + "'")
                    .add("\n\tschedule=" + schedule)
                    .add("\n\ttasks=" + tasks)
                    .add("\n\tservices=" + services.toString().replace("\n", "\n\t"))
                    .add("\n\t" + super.toString())
                    .toString();
        }
    }
}
