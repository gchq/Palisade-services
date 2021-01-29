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

package uk.gov.gchq.palisade.service.audit.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import uk.gov.gchq.palisade.service.audit.ApplicationTestData;
import uk.gov.gchq.palisade.service.audit.model.AuditErrorMessage;
import uk.gov.gchq.palisade.service.audit.model.AuditSuccessMessage;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AuditServiceAsyncProxyTest {

    private AuditServiceAsyncProxy proxy;
    private final SimpleAuditService auditService = new SimpleAuditService();
    private final Map<String, AuditService> auditServiceMap = new HashMap<>();


    @BeforeEach
    void setup() {
        auditServiceMap.put("simple", auditService);
        proxy = new AuditServiceAsyncProxy(auditServiceMap);
    }

    @ParameterizedTest
    @ValueSource(strings = {"data-service", "filtered-resource-service"})
    void testGoodSuccessMessage(final String serviceName) {
        // Given an Audit Success Message
        AuditSuccessMessage message = ApplicationTestData.auditSuccessMessage(serviceName);

        // When processed by the proxy service
        List<Boolean> result = proxy.audit("token", message).join();

        // Then check the returned value
        assertThat(result).isEqualTo(Collections.singletonList(true));
    }

    @Test
    void testBadSuccessMessage() {
        // Given an Audit Success Message
        AuditSuccessMessage message = ApplicationTestData.auditSuccessMessage("resource-service");

        // When processed by the proxy service
        List<Boolean> result = proxy.audit("token", message).join();

        // Then check the returned value
        assertThat(result).isEqualTo(Collections.singletonList(false));
    }

    @Test
    void testErrorMessage() {
        // Given an Audit Success Message
        AuditErrorMessage message = ApplicationTestData.auditErrorMessage("user-service");

        // When processed by the proxy service
        List<Boolean> result = proxy.audit("token", message).join();

        // Then check the returned value
        assertThat(result).isEqualTo(Collections.singletonList(true));
    }
}
