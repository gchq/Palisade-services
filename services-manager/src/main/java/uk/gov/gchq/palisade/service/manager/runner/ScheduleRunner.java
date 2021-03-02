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
import uk.gov.gchq.palisade.service.manager.config.TaskConfiguration;
import uk.gov.gchq.palisade.service.manager.service.ManagedService;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * An ApplicationRunner to run through a start-up schedule.
 * A schedule is an ordered collection of tasks
 * A task is an unordered collection of services
 * Every service in a task must complete before the next task is started
 * All tasks must complete successfully for the schedule to complete successfully
 */
public class ScheduleRunner implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScheduleRunner.class);
    private static final String BAR = "========================";
    private static final int POLL_SECONDS = 5;

    // Autowired through constructor
    private final File rootDir;
    private final List<Map.Entry<String, TaskConfiguration>> schedule;
    private final Function<String, ManagedService> serviceProducer;

    public ScheduleRunner(final ManagerConfiguration managerConfiguration, final Function<String, ManagedService> serviceProducer) {
        this.rootDir = managerConfiguration.getRoot();
        this.schedule = managerConfiguration.getSchedule();
        this.serviceProducer = serviceProducer;
    }

    // Suppress System.exit warning
    @SuppressWarnings("java:S1147")
    public void waitUntilComplete(final Map<String, List<Supplier<Boolean>>> taskCompleteIndicators) {
        try {
            boolean complete = false;
            while (!complete) {
                TimeUnit.SECONDS.sleep(POLL_SECONDS);
                complete = taskCompleteIndicators.entrySet().stream().allMatch((Entry<String, List<Supplier<Boolean>>> indicators) -> {
                    String serviceName = indicators.getKey();
                    boolean serviceComplete = indicators.getValue().stream().anyMatch(Supplier::get);
                    if (!serviceComplete) {
                        LOGGER.info("Waiting for {}", serviceName);
                    }
                    return serviceComplete;
                });
            }
        } catch (InterruptedException ex) {
            LOGGER.error("Error while waiting for services: ", ex);
            Thread.currentThread().interrupt();
            System.exit(1);
        }
    }

    public void run() {
        schedule.forEach((Entry<String, TaskConfiguration> taskEntry) -> {
            LOGGER.info("");
            LOGGER.info(BAR);
            LOGGER.info("STARTING TASK :: {}", taskEntry.getKey());
            LOGGER.info(BAR);
            LOGGER.debug("Will be running {}", taskEntry.getValue());

            Map<String, List<Supplier<Boolean>>> taskCompleteIndicators = taskEntry.getValue().runTask(rootDir, serviceProducer);
            LOGGER.info("");
            waitUntilComplete(taskCompleteIndicators);

            LOGGER.info("");
            LOGGER.info("Task complete");
            LOGGER.info(BAR);
            LOGGER.info("");
        });
    }
}
