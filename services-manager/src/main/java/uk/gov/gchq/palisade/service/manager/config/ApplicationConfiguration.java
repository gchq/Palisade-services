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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import uk.gov.gchq.palisade.Generated;

import java.io.File;
import java.util.AbstractMap.SimpleEntry;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

@Configuration
@EnableConfigurationProperties
@EnableAutoConfiguration
public class ApplicationConfiguration {

    @Value("${manager.root}")
    private String root;

    @Bean
    @ConfigurationProperties(prefix = "manager")
    ConfigurationMap configurationMap() {
        return new ConfigurationMap();
    }

    @Bean
    Map<String, ServiceConfiguration> serviceConfigurations(final ConfigurationMap configurationMap) {
        return configurationMap.getServices();
    }

    private File getServicesRoot() {
        File parent = new File(".").getAbsoluteFile();
        while (parent != null && !root.equals(parent.getName())) {
            parent = parent.getParentFile();
        }
        return parent;
    }

    @Bean("runnerBuilders")
    public Map<String, ProcessBuilder> runnerBuilders(final Map<String, ServiceConfiguration> runnerConfigs) {
        return runnerConfigs.entrySet().stream()
                .map(e -> {
                    ServiceConfiguration config = e.getValue();
                    ProcessBuilder builder = config.getProcessBuilder();
                    builder.directory(getServicesRoot());
                    return new SimpleEntry<>(e.getKey(), builder);
                })
                .collect(Collectors.toMap(SimpleEntry::getKey, SimpleEntry::getValue));
    }

    public static class ConfigurationMap {

        private Map<String, ServiceConfiguration> services = new HashMap<>();

        public Map<String, ServiceConfiguration> getServices() {
            return services;
        }

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
            return Objects.equals(services, that.services);
        }

        @Override
        @Generated
        public int hashCode() {
            return Objects.hash(services);
        }

        @Override
        @Generated
        public String toString() {
            // A non-standard equals function with some newlines and indents
            // Let JaCoCo treat it as @Generated anyway
            final StringBuilder sb = new StringBuilder("ConfigurationMap{\n");
            sb.append('\t').append(services.entrySet().stream()
                    .map(entry -> entry.toString().replace("\n", "\n\t"))
                    .collect(Collectors.joining("\n\t"))).append('\n');
            sb.append('}');
            return sb.toString();
        }
    }
}
