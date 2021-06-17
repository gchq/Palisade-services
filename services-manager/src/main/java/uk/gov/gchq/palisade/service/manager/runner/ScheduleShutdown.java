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
import uk.gov.gchq.palisade.service.manager.service.ManagedService;

import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * An ApplicationRunner to shutdown all available services in the reverse-order to the start-up schedule
 * There is no verification that services have successfully shut down
 * This could be implemented by checking the health endpoint
 */
public class ScheduleShutdown implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScheduleShutdown.class);

    // Autowired through constructor
    private final List<String> serviceNames;
    private final Function<String, ManagedService> serviceProducer;

    /**
     * Constructor taking 2 arguments
     *
     * @param managerConfiguration the configuration for the service manager
     * @param serviceProducer the function to be actioned
     */
    public ScheduleShutdown(final ManagerConfiguration managerConfiguration, final Function<String, ManagedService> serviceProducer) {
        // Initially not reversed
        List<String> reversedServiceNames = managerConfiguration.getSchedule().stream()
                .map(Entry::getValue)
                .flatMap(task -> task.getServices().keySet().stream())
                .collect(Collectors.toList());
        Collections.reverse(reversedServiceNames);
        // Now reversedServiceNames is actually reversed

        this.serviceNames = reversedServiceNames;
        this.serviceProducer = serviceProducer;
    }

    public void run() {
        serviceNames.forEach((String serviceName) -> {
            LOGGER.info("Shutting down {}", serviceName);
            ManagedService service = serviceProducer.apply(serviceName);
            service.shutdown();
        });
    }
}
