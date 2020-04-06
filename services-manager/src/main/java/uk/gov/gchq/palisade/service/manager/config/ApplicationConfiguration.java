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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import uk.gov.gchq.palisade.Generated;
import uk.gov.gchq.palisade.service.manager.runner.ConfigChecker;
import uk.gov.gchq.palisade.service.manager.runner.LoggingBouncer;
import uk.gov.gchq.palisade.service.manager.runner.ServicesRunner;
import uk.gov.gchq.palisade.service.manager.service.ManagedService;
import uk.gov.gchq.palisade.service.manager.web.ManagedClient;

import java.io.File;
import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

    @Value("${manager.root}")
    private String root;

    @Bean
    @ConfigurationProperties(prefix = "manager")
    ConfigurationMap configurationMap() {
        return new ConfigurationMap();
    }

    @Bean
    @ConfigurationProperties(prefix = "web")
    public ClientConfiguration clientConfiguration() {
        return new ClientConfiguration();
    }

    @Bean
    Map<String, ServiceConfiguration> serviceConfigurations(final ConfigurationMap configurationMap) {
        return configurationMap.getServices();
    }

    @Bean
    List<TaskConfiguration> taskConfigurations(final ConfigurationMap configurationMap) {
        return configurationMap.getTasks().stream()
                .map(taskConfiguration -> {
                    try {
                        LOGGER.debug("Processing task :: {}", taskConfiguration);
                        return new TaskConfiguration(taskConfiguration, configurationMap.getServices());
                    } catch (Exception e) {
                        LOGGER.error("An error occurred: ", e);
                        System.exit(-1);
                        return null;
                    }
                })
                .collect(Collectors.toList());
    }

    private File getServicesRoot() {
        File parent = new File(".").getAbsoluteFile();
        while (parent != null && !root.equals(parent.getName())) {
            parent = parent.getParentFile();
        }
        return parent;
    }

    @Bean("configChecker")
    @ConditionalOnProperty(name = "manager.mode", havingValue = "configChecker", matchIfMissing = true)
    public ApplicationRunner configCheckerRunner(final ConfigurationMap configurationMap) {
        LOGGER.info("Constructed ConfigChecker runner");
        return args -> {
            new ConfigChecker(configurationMap).run();
            System.exit(0);
        };
    }

    @Bean("loggingBouncer")
    @ConditionalOnProperty(name = "manager.mode", havingValue = "loggingBouncer")
    public ApplicationRunner loggingBouncerRunner(final Map<String, ServiceConfiguration> serviceConfigurationMap,
                                                  final Function<String, ManagedService> serviceProducer) {
        LOGGER.info("Constructed LoggingBouncer runner");
        return args -> {
            new LoggingBouncer(serviceConfigurationMap, serviceProducer).run();
            System.exit(0);
        };
    }

    @Bean("servicesRunner")
    @ConditionalOnProperty(name = "manager.mode", havingValue = "servicesRunner")
    public ApplicationRunner servicesRunner(final List<TaskConfiguration> taskConfigurations,
                                            final Function<String, ManagedService> serviceProducer) {
        LOGGER.info("Constructed ServicesRunner runner");
        return args -> {
            taskConfigurations.forEach(task -> {
                LOGGER.info("Starting task :: {}", task.getTaskName());
                LOGGER.debug("Will be running services :: {}", task.getSubTasks().keySet());
                ServicesRunner servicesRunner = new ServicesRunner(task.getProcessBuilders(getServicesRoot()), serviceProducer);
                Map<String, List<Supplier<Boolean>>> taskCompleteIndicators = servicesRunner.run();
                LOGGER.info("Waiting for task {} to complete", task.getTaskName());
                try {
                    Boolean complete = false;
                    while (!complete) {
                        Thread.sleep(5000);
                        complete = taskCompleteIndicators.entrySet().stream().allMatch(indicators -> {
                            String serviceName = indicators.getKey();
                            Boolean serviceComplete = indicators.getValue().stream().anyMatch(Supplier::get);
                            LOGGER.info("{} :: {}", serviceName, serviceComplete ? "Complete!" : "Waiting...");
                            return serviceComplete;
                        });
                    }
                } catch (Exception ex) {
                    LOGGER.error("There was an error: ", ex);
                    System.exit(-1);
                }
            });
            System.exit(0);
        };
    }

    @Bean("managedServiceSupplier")
    public Function<String, ManagedService> managedServiceProducer(final ManagedClient client, final ClientConfiguration clientConfig) {
        return serviceName -> {
            Supplier<Collection<URI>> uriSupplier = () -> {
                Collection<URI> clientUris = clientConfig.getClientUri(serviceName);
                LOGGER.debug("Service {} has client uris {}", serviceName, clientUris);
                return clientUris;
            };
            return new ManagedService(client, uriSupplier);
        };
    }

    public static class ConfigurationMap {

        private List<Map<String, List<String>>> tasks = new LinkedList<>();

        private Map<String, ServiceConfiguration> services = new HashMap<>();

        @Generated
        public List<Map<String, List<String>>> getTasks() {
            return tasks;
        }

        @Generated
        public void setTasks(final List<Map<String, List<String>>> tasks) {
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
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof ConfigurationMap)) {
                return false;
            }
            final ConfigurationMap that = (ConfigurationMap) o;
            return Objects.equals(tasks, that.tasks) &&
                    Objects.equals(services, that.services);
        }

        @Override
        @Generated
        public int hashCode() {
            return Objects.hash(tasks, services);
        }

        @Override
        @Generated
        public String toString() {
            return new StringJoiner(", ", ConfigurationMap.class.getSimpleName() + "[", "\n]")
                    .add("\n\ttasks=" + tasks)
                    .add("\n\tservices=" + services.toString().replace("\n", "\n\t"))
                    .add("\n\t" + super.toString())
                    .toString();
        }
    }
}
