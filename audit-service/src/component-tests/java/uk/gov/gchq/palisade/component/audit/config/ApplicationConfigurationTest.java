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

package uk.gov.gchq.palisade.component.audit.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import uk.gov.gchq.palisade.service.audit.common.audit.AuditService;
import uk.gov.gchq.palisade.service.audit.config.ApplicationConfiguration;
import uk.gov.gchq.palisade.service.audit.service.LoggerAuditService;
import uk.gov.gchq.palisade.service.audit.service.SimpleAuditService;
import uk.gov.gchq.palisade.service.audit.service.StroomAuditService;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ContextConfiguration(classes = ApplicationConfiguration.class)
class ApplicationConfigurationTest {

    @Test
    void testConfigurationDefinesLoadedServices(@Autowired final Map<String, AuditService> auditServices) {
        assertThat(auditServices.values())
                .as("Check that the different Audit Services have been started successfully")
                .extracting(as -> (Class) as.getClass())
                .containsExactlyInAnyOrder(
                        LoggerAuditService.class,
                        StroomAuditService.class,
                        SimpleAuditService.class);
    }
}
