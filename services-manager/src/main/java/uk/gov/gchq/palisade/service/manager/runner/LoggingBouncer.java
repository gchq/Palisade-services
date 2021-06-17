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
import uk.gov.gchq.palisade.service.manager.config.ServiceConfiguration;
import uk.gov.gchq.palisade.service.manager.service.ManagedService;

import java.io.IOException;
import java.util.Map;
import java.util.function.Function;

/**
 * An ApplicationRunner to change the logging level of *all* available services
 */
public class LoggingBouncer implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingBouncer.class);

    // Autowired through constructor
    private final Map<String, ServiceConfiguration> loggingConfiguration;
    private final Function<String, ManagedService> serviceProducer;

    /**
     * Constructor taking 2 arguments
     *
     * @param managerConfiguration the configuration for the service manager
     * @param serviceProducer the function to be actioned
     */
    public LoggingBouncer(final ManagerConfiguration managerConfiguration, final Function<String, ManagedService> serviceProducer) {
        this.loggingConfiguration = managerConfiguration.getServices();
        this.serviceProducer = serviceProducer;
    }

    /**
     * Runs the function for each entry in the configuration map
     */
    public void run() {
        loggingConfiguration.forEach((String serviceName, ServiceConfiguration config) -> {
            LOGGER.info("Configuring logging for {}", serviceName);
            ManagedService service = serviceProducer.apply(serviceName);
            config.getLevel().forEach((String packageName, String level) -> {
                LOGGER.debug("Configuring service {} with package {} as {}", serviceName, packageName, level);
                try {
                    service.setLoggers(packageName, level);
                } catch (IOException ex) {
                    LOGGER.error("Error while changing logging level: ", ex);
                }
            });
        });
    }
}
