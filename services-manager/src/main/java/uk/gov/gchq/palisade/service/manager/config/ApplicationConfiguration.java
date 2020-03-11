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

    public static class ConfigurationMap {

        private Map<String, ServiceConfiguration> services = new HashMap<>();

        public Map<String, ServiceConfiguration> getServices() {
            System.out.println("Getting service configuration: " + this.services);
            return services;
        }

        public void setServices(final Map<String, ServiceConfiguration> services) {
            System.out.println("Setting service configuration: " + services);
            this.services = services;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("ConfigurationMap{\n");
            sb.append('\t').append(services.entrySet().stream()
                    .map(entry -> entry.toString().replace("\n", "\n\t"))
                    .collect(Collectors.joining("\n\t"))).append('\n');
            sb.append('}');
            return sb.toString();
        }
    }

    @Bean
    @ConfigurationProperties(prefix = "manager")
    ConfigurationMap configurationMap() {
        System.out.println("Configuration map time...");
        return new ConfigurationMap();
    }

    @Bean
    Map<String, ServiceConfiguration> serviceConfigurations(final ConfigurationMap configurationMap) {
        System.out.println("Service Configuration bean time");
        System.out.println();
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

    @Bean("runnerBuilders")
    public Map<String, ProcessBuilder> runnerBuilders(final Map<String, ServiceConfiguration> runnerConfigs) {
        System.out.println();
        System.out.println("Runner Configs: " + runnerConfigs);
        return runnerConfigs.entrySet().stream()
                .map(e -> {
                    ServiceConfiguration config = e.getValue();
                    ProcessBuilder builder = config.getProcessBuilder();
                    builder.directory(getServicesRoot());
                    return new SimpleEntry<>(e.getKey(), builder);
                })
                .collect(Collectors.toMap(SimpleEntry::getKey, SimpleEntry::getValue));
    }

}
