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
package uk.gov.gchq.palisade.service.audit.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static uk.gov.gchq.palisade.service.audit.ApplicationTestData.auditErrorMessage;
import static uk.gov.gchq.palisade.service.audit.ApplicationTestData.auditSuccessMessage;
import static uk.gov.gchq.palisade.service.audit.service.ServiceName.DATA_SERVICE;
import static uk.gov.gchq.palisade.service.audit.service.ServiceName.FILTERED_RESOURCE_SERVICE;
import static uk.gov.gchq.palisade.service.audit.service.ServiceName.POLICY_SERVICE;
import static uk.gov.gchq.palisade.service.audit.service.ServiceName.RESOURCE_SERVICE;
import static uk.gov.gchq.palisade.service.audit.service.ServiceName.USER_SERVICE;

class MessageSerialisationTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    static class MessageTypeSource implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(final ExtensionContext extensionContext) {
            return Stream.of(
                arguments(auditErrorMessage(USER_SERVICE)),
                arguments(auditErrorMessage(RESOURCE_SERVICE)),
                arguments(auditErrorMessage(POLICY_SERVICE)),
                arguments(auditSuccessMessage(DATA_SERVICE)),
                arguments(auditSuccessMessage(FILTERED_RESOURCE_SERVICE)),
                arguments(auditSuccessMessage(USER_SERVICE))
            );
        }
    }

    @ParameterizedTest
    @ArgumentsSource(MessageTypeSource.class)
    void testSerialiseDeserialiseIsConsistent(final Object expectedMessage) throws JsonProcessingException {

        var type = expectedMessage.getClass();
        var actualMessage = MAPPER.readValue(MAPPER.writeValueAsString(expectedMessage), type);

        assertThat(actualMessage)
            .as("check deserialised %s is the same as the original using recursive comparison", type)
            .usingRecursiveComparison()
            .ignoringFieldsOfTypes(Throwable.class)
            .isEqualTo(expectedMessage);

        assertThat(actualMessage)
            .as("check deserialisaed %s is the same as the original using equals", type)
            .isEqualTo(expectedMessage);

    }
}
