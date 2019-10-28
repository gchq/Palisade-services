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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Bean config and dependency injection graph
 */
@Configuration
@EnableConfigurationProperties({DefaultsConfiguration.class, ServicesConfiguration.class})
public class ApplicationConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationConfiguration.class);

    @Autowired
    DefaultsConfiguration defaultsConfiguration;

    @Autowired
    ServicesConfiguration servicesConfiguration;

    @Bean
    List<ProcessBuilder> serviceProcesses() {
        return servicesConfiguration.getServices().stream()
                .map(this::constructServiceProcess).collect(Collectors.toList());
    }

    private ProcessBuilder constructServiceProcess(final OverriddenConfiguration config) {
        config.defaults(defaultsConfiguration);
        String[] command = new String[] {
                JavaEnvUtils.getJreExecutable("java"),
                "-jar", config.getTarget(),
                String.format("--spring-config-location=%s", config.getConfig())
        };
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectOutput(new File(config.getLog()));
        builder.redirectError(new File(config.getLog()));
        LOGGER.info(String.format("Constructed ProcessBuilder %s [%s]", Arrays.toString(command), builder.redirectOutput().toString()));
        return builder;
    }
}
