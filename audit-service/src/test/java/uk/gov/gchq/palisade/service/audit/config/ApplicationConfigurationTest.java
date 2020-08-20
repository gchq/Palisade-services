/*
 * Copyright 2019 Crown Copyright
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

package uk.gov.gchq.palisade.service.audit.config;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import uk.gov.gchq.palisade.service.audit.service.AuditService;
import uk.gov.gchq.palisade.service.audit.service.LoggerAuditService;
import uk.gov.gchq.palisade.service.audit.service.SimpleAuditService;
import uk.gov.gchq.palisade.service.audit.service.StroomAuditService;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.isIn;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

@SpringBootTest
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = ApplicationConfiguration.class)
@ActiveProfiles(profiles = "")
public class ApplicationConfigurationTest {

    private static final Set<Class> EXPECTED_AUDITS = new HashSet<>();

    static {
        EXPECTED_AUDITS.add(LoggerAuditService.class);
        EXPECTED_AUDITS.add(StroomAuditService.class);
        EXPECTED_AUDITS.add(SimpleAuditService.class);
    }

    @Autowired
    private Map<String, AuditService> auditServices;

    @Test
    public void auditServicesLoaded() {
        assertThat(auditServices, not(equalTo(nullValue())));
    }

    @Test
    public void configurationDefinesLoadedServices() {
        // Given - expectedAudits
        // Then
        for (AuditService auditService : auditServices.values()) {
            assertThat(auditService.getClass(), isIn(EXPECTED_AUDITS));
        }
    }
}
