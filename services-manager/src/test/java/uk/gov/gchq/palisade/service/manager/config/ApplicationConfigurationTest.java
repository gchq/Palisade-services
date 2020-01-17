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

package uk.gov.gchq.palisade.service.manager.config;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.notNullValue;

@SpringBootTest
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = ApplicationConfiguration.class)
public class ApplicationConfigurationTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationConfigurationTest.class);

    @Autowired
    Map<String, ServiceConfiguration> services;

    @Test
    public void testConfigurationIsLoaded() {
        String serviceKey = "my-service";
        LOGGER.debug("Loaded service configuration: {}", services.toString());

        assertThat(services.keySet(), contains(serviceKey));

        ServiceConfiguration myService = services.get(serviceKey);
        assertThat(myService, notNullValue());

        ProcessBuilder myBuilder = myService.getProcessBuilder();
        assertThat(myBuilder, notNullValue());

        Map<String, HttpEntity<String>> myLogging = myService.getLoggingChangeEntities();
        assertThat(myLogging, notNullValue());
    }
}
