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

import uk.gov.gchq.palisade.service.manager.config.ServiceConfiguration;
import uk.gov.gchq.palisade.service.manager.service.ManagedService;

import java.util.Map;
import java.util.function.Function;

public class LoggingBouncer {
    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingBouncer.class);

    // Autowired through constructor
    private Map<String, ServiceConfiguration> loggingConfiguration;
    private Function<String, ManagedService> serviceProducer;

    public LoggingBouncer(final Map<String, ServiceConfiguration> loggingConfiguration, final Function<String, ManagedService> serviceProducer) {
        this.loggingConfiguration = loggingConfiguration;
        this.serviceProducer = serviceProducer;
    }

    public void run() {
        LOGGER.debug("Loaded LoggerConfiguration: {}", loggingConfiguration);

        loggingConfiguration.forEach((serviceName, config) -> {
            LOGGER.info("Configuring logging for {}", serviceName);
            ManagedService service = serviceProducer.apply(serviceName);
            config.getLevel().forEach((module, level) -> {
                LOGGER.debug("Configuring service {} with module {} as {}", serviceName, module, level);
                try {
                    service.setLoggers(module, level);
                } catch (Exception ex) {
                    LOGGER.error("There was an error: ", ex);
                }
            });
        });
    }
}
