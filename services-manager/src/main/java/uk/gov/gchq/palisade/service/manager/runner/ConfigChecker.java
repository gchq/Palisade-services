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

package uk.gov.gchq.palisade.service.manager.runner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import uk.gov.gchq.palisade.service.manager.config.ApplicationConfiguration.ConfigurationMap;

@Component("config-checker")
public class ConfigChecker implements ApplicationRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigChecker.class);

    // Autowired through constructor
    private ConfigurationMap config;

    public ConfigChecker(final ConfigurationMap config) {
        this.config = config;
    }

    @Override
    public void run(final ApplicationArguments args) throws Exception {
        if (args.containsOption("config")) {
            LOGGER.info("Loaded config:\n{}", config.toString());

            System.exit(0);
        }
    }
}
