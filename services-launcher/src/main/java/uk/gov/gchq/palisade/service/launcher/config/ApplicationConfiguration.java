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

package uk.gov.gchq.palisade.service.launcher.config;

import org.apache.tools.ant.util.JavaEnvUtils;
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
            return services;
        }

        public void setServices(final Map<String, ServiceConfiguration> services) {
            this.services = services;
        }

    }

    @Bean
    @ConfigurationProperties(prefix = "launcher")
    ConfigurationMap configurationMap() {
        return new ConfigurationMap();
    }

    @Value("${launcher.root}")
    private String root;

    private File getServicesRoot() {
        File parent = new File(".").getAbsoluteFile();
        while (parent != null && !root.equals(parent.getName())) {
            parent = parent.getParentFile();
        }
        return parent;
    }

    @Bean
    public Map<String, ProcessBuilder> serviceBuilders(final ConfigurationMap configurationMap) {
        return configurationMap.getServices().entrySet().stream()
                .map(e -> {
                    ServiceConfiguration config = e.getValue();
                    String[] command = new String[] {
                            JavaEnvUtils.getJreExecutable("java"),
                            "-cp", config.getClasspath(),
                            String.format("-Dspring.config.location=%s", config.getConfig()),
                            String.format("-Dspring.profiles.active=%s", config.getProfiles()),
                            String.format("-Dloader.main=%s", config.getMain()),
                            config.getLauncher()
                    };
                    ProcessBuilder pb = new ProcessBuilder()
                            .command(command)
                            .directory(getServicesRoot())
                            .redirectOutput(new File(config.getLog()))
                            .redirectError(new File(config.getLog()));
                    return new SimpleEntry<>(e.getKey(), pb);
                })
                .collect(Collectors.toMap(SimpleEntry::getKey, SimpleEntry::getValue));
    }
}
