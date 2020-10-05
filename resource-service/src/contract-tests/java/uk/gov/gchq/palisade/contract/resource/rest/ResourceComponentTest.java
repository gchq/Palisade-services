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

package uk.gov.gchq.palisade.contract.resource.rest;

import feign.Response;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ActiveProfiles;

import uk.gov.gchq.palisade.contract.resource.config.ResourceTestConfiguration;
import uk.gov.gchq.palisade.contract.resource.config.web.ResourceClient;
import uk.gov.gchq.palisade.contract.resource.config.web.ResourceClientWrapper;
import uk.gov.gchq.palisade.service.ResourceService;
import uk.gov.gchq.palisade.service.resource.ResourceApplication;

import java.util.Collections;
import java.util.Map;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

@EnableFeignClients(basePackageClasses = {ResourceClient.class})
@Import(ResourceTestConfiguration.class)
@SpringBootTest(classes = ResourceApplication.class, webEnvironment = WebEnvironment.DEFINED_PORT)
@EnableJpaRepositories(basePackages = {"uk.gov.gchq.palisade.service.resource.repository"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
@ActiveProfiles({"h2", "web"})
class ResourceComponentTest {

    @Autowired
    private Map<String, ResourceService> serviceMap;

    @Autowired
    private ResourceClientWrapper client;

    @Test
    void contextLoads() {
        assertNotNull(serviceMap);
        assertNotEquals(serviceMap, Collections.emptyMap());
    }

    @Test
    void isUp() {
        Response health = client.getHealth();

        assertThat(health.status(), equalTo(200));
    }

}