/*
 * Copyright 2018-2021 Crown Copyright
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

import uk.gov.gchq.palisade.service.manager.config.ApplicationConfiguration.ManagerConfiguration;

/**
 * An ApplicationRunner to print out the loaded config for debugging purposes
 */
public class ConfigPrinter implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigPrinter.class);

    // Autowired through constructor
    private ManagerConfiguration config;

    public ConfigPrinter(final ManagerConfiguration config) {
        this.config = config;
    }

    public void run() {
        LOGGER.info("Loaded config...\n{}", config.toString());
    }
}
