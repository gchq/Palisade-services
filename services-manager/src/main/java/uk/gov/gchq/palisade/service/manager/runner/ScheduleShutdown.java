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

import uk.gov.gchq.palisade.service.manager.config.ApplicationConfiguration.ManagerConfiguration;
import uk.gov.gchq.palisade.service.manager.config.ServiceConfiguration;
import uk.gov.gchq.palisade.service.manager.service.ManagedService;

import java.util.Map;
import java.util.function.Function;

public class ScheduleShutdown implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScheduleShutdown.class);

    // Autowired through constructor
    private Map<String, ServiceConfiguration> serviceConfiguration;
    private Function<String, ManagedService> serviceProducer;

    public ScheduleShutdown(final ManagerConfiguration managerConfiguration, final Function<String, ManagedService> serviceProducer) {
        this.serviceConfiguration = managerConfiguration.getServices();
        this.serviceProducer = serviceProducer;
    }

    public void run() {
        serviceConfiguration.forEach((serviceName, config) -> {
            LOGGER.info("Shutting down {}", serviceName);
            ManagedService service = serviceProducer.apply(serviceName);
            service.shutdown();
        });
    }
}
