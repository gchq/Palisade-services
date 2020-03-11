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

package uk.gov.gchq.palisade.service.user.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
@EnableConfigurationProperties
@EnableAutoConfiguration
public class ConfigurationMap {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationMap.class);

    private Map<String, UserConfiguration> userConfigs = new HashMap<>();

    public Map<String, UserConfiguration> getUserConfigs() {
        LOGGER.info("Getting UserConfiguration details: {}", this.userConfigs);
        return userConfigs;
    }

    public void setUserConfigs(final Map<String, UserConfiguration> userConfigs) {
        LOGGER.info("Setting UserConfiguration details: {}", userConfigs);
        this.userConfigs = userConfigs;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ConfigurationMap{\n");
        sb.append('\t').append(userConfigs.entrySet().stream()
                .map(entry -> entry.toString().replace("\n", "\n\t"))
                .collect(Collectors.joining("\n\t"))).append('\n');
        sb.append('}');
        return sb.toString();
    }
}
