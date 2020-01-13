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

import com.netflix.appinfo.InstanceInfo;
import javafx.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import uk.gov.gchq.palisade.service.manager.config.LoggingConfiguration;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component("logging-changer")
public class LoggingChanger extends EurekaUtils implements ApplicationRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingChanger.class);

    // Autowired through constructor
    private Map<String, LoggingConfiguration> loggingConfiguration;

    public LoggingChanger(Map<String, LoggingConfiguration> loggingConfiguration) {
        this.loggingConfiguration = loggingConfiguration;
    }

    Map<String, String> gatherLoggersActuators(Map<String, LoggingConfiguration> configuration) {
        return Collections.emptyMap();
    }

    Map<String, RestTemplate> preparePostRequests(Map<String, Pair<String, LoggingConfiguration>> actuators) {
        return Collections.emptyMap();
    }

    Map<String, ResponseEntity<String>> postToActuators(Map<String, RestTemplate> requests) {
        return Collections.emptyMap();
    }

    @Override
    public void run(final ApplicationArguments args) throws Exception {
        if (args.containsOption("logging")) {
            LOGGER.info("Loaded LoggerConfiguration: {}", loggingConfiguration);

            // Get running services and logging actuators to ping
            List<InstanceInfo> runningServices = getRunningServices();
        }
    }

}
