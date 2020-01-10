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

package uk.gov.gchq.palisade.service.manager.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.util.AbstractMap.SimpleEntry;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
@EnableConfigurationProperties
@EnableAutoConfiguration
public class ApplicationConfiguration {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationConfiguration.class);

    public static class ConfigurationMap {

        private Map<String, ServiceConfiguration> services = new HashMap<>();

        public Map<String, ServiceConfiguration> getServices() {
            return services;
        }

        public void setServices(final Map<String, ServiceConfiguration> services) {
            this.services = services;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("ConfigurationMap{");
            sb.append("services=").append(services);
            sb.append('}');
            return sb.toString();
        }
    }

    @Bean
    @ConfigurationProperties(prefix = "manager")
    ConfigurationMap configurationMap() {
        return new ConfigurationMap();
    }

    @Bean
    Map<String, ServiceConfiguration> serviceConfigurations(ConfigurationMap configurationMap) {
        LOGGER.info(configurationMap.getServices().toString());
        return configurationMap.getServices();
    }

    @Value("${manager.root}")
    private String root;

    private File getServicesRoot() {
        File parent = new File(".").getAbsoluteFile();
        while (parent != null && !root.equals(parent.getName())) {
            parent = parent.getParentFile();
        }
        return parent;
    }

    @Bean
    public Map<String, ProcessBuilder> runnerBuilders(final ConfigurationMap configurationMap) {
        return configurationMap.getServices().entrySet().stream()
                .map(e -> {
                    RunnerConfiguration runnerConfig = e.getValue().getRunner();
                    ProcessBuilder runnerBuilder = runnerConfig.getProcessBuilder()
                            .directory(getServicesRoot());
                    return new SimpleEntry<>(e.getKey(), runnerBuilder);
                })
                .peek(e -> LOGGER.info(e.toString()))
                .collect(Collectors.toMap(SimpleEntry::getKey, SimpleEntry::getValue));
    }

    @Bean
    public Map<String, RunnerConfiguration> runnerConfigs(final ConfigurationMap configurationMap) {
        return configurationMap.getServices().entrySet().stream()
                .map(e -> new SimpleEntry<>(e.getKey(), e.getValue().getRunner()))
                .peek(e -> LOGGER.info(e.toString()))
                .collect(Collectors.toMap(SimpleEntry::getKey, SimpleEntry::getValue));
    }

    @Bean
    public Map<String, LoggingConfiguration> loggingConfigs(final ConfigurationMap configurationMap) {
        return configurationMap.getServices().entrySet().stream()
                .map(e -> new SimpleEntry<>(e.getKey(), e.getValue().getLogging()))
                .peek(e -> LOGGER.info(e.toString()))
                .collect(Collectors.toMap(SimpleEntry::getKey, SimpleEntry::getValue));
    }
}
