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

package uk.gov.gchq.palisade.component.audit.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import uk.gov.gchq.palisade.service.audit.config.ApplicationConfiguration;
import uk.gov.gchq.palisade.service.audit.service.AuditService;
import uk.gov.gchq.palisade.service.audit.service.LoggerAuditService;
import uk.gov.gchq.palisade.service.audit.service.SimpleAuditService;
import uk.gov.gchq.palisade.service.audit.service.StroomAuditService;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ContextConfiguration(classes = ApplicationConfiguration.class)
public class ApplicationConfigurationTest {

    private static final Set<Class<? extends AuditService>> EXPECTED_AUDITS = new HashSet<>();

    static {
        EXPECTED_AUDITS.add(LoggerAuditService.class);
        EXPECTED_AUDITS.add(StroomAuditService.class);
        EXPECTED_AUDITS.add(SimpleAuditService.class);
    }

    @Autowired
    private Map<String, AuditService> auditServices;

    @Test
    public void testAuditServicesLoads() {
        assertThat(auditServices).isNotNull();
    }

    @Test
    public void testConfigurationDefinesLoadedServices() {
        // Given - expectedAudits
        // Then
        for (AuditService auditService : auditServices.values()) {
            assertThat(EXPECTED_AUDITS).contains(auditService.getClass());
        }
    }
}