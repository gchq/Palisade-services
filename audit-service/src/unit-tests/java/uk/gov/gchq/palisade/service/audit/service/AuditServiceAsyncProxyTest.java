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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import uk.gov.gchq.palisade.service.audit.common.audit.AuditService;
import uk.gov.gchq.palisade.service.audit.model.AuditMessage;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static uk.gov.gchq.palisade.service.audit.ApplicationTestData.auditErrorMessage;
import static uk.gov.gchq.palisade.service.audit.ApplicationTestData.auditSuccessMessage;
import static uk.gov.gchq.palisade.service.audit.service.ServiceName.DATA_SERVICE;
import static uk.gov.gchq.palisade.service.audit.service.ServiceName.FILTERED_RESOURCE_SERVICE;
import static uk.gov.gchq.palisade.service.audit.service.ServiceName.RESOURCE_SERVICE;
import static uk.gov.gchq.palisade.service.audit.service.ServiceName.USER_SERVICE;

class AuditServiceAsyncProxyTest {

    private static final List<Boolean> TRUE_RESULT = Collections.singletonList(true);
    private static final List<Boolean> FALSE_RESULT = Collections.singletonList(false);
    private static final String TOKEN = "token";

    private AuditServiceAsyncProxy proxy;
    private final SimpleAuditService auditService = new SimpleAuditService();
    private final Map<String, AuditService> auditServiceMap = new HashMap<>();

    @BeforeEach
    void setup() {
        auditServiceMap.put("simple", auditService);
        proxy = new AuditServiceAsyncProxy(auditServiceMap);
    }

    private static Stream<Arguments> messages() {
        return Stream.of(
                arguments("good success", USER_SERVICE, auditErrorMessage(USER_SERVICE), TRUE_RESULT),
                arguments("good success", DATA_SERVICE, auditSuccessMessage(DATA_SERVICE), TRUE_RESULT),
                arguments("bad success", RESOURCE_SERVICE, auditSuccessMessage(RESOURCE_SERVICE), FALSE_RESULT),
                arguments("error message", FILTERED_RESOURCE_SERVICE, auditSuccessMessage(FILTERED_RESOURCE_SERVICE), TRUE_RESULT));
    }

    @DisplayName("Test proxy result")
    @ParameterizedTest(name = "{index} => Test {0}, serviceName=''{1}'', expectedResult={3}")
    @MethodSource("messages")
    void testMessage(
            final String text,
            final ServiceName serviceName,
            final AuditMessage message,
            final List<Boolean> expectedResult) {

        var actualResult = proxy.audit(TOKEN, message).join();
        assertThat(actualResult)
                .as("Check that the messages have been audited successfully")
                .isEqualTo(expectedResult);

    }

}
