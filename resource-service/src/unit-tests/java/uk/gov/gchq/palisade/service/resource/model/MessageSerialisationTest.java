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
package uk.gov.gchq.palisade.service.resource.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import uk.gov.gchq.palisade.service.resource.ApplicationTestData;
import uk.gov.gchq.palisade.service.resource.config.ApplicationConfiguration;

import java.util.Collections;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class MessageSerialisationTest {
    private static final ObjectMapper MAPPER = new ApplicationConfiguration().objectMapper();

    static class MessageTypeSource implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(final ExtensionContext extensionContext) {
            return Stream.of(
                    Arguments.of(ApplicationTestData.REQUEST),
                    Arguments.of(ApplicationTestData.RESPONSE),
                    Arguments.of(AuditErrorMessage.Builder.create(
                            ApplicationTestData.REQUEST,
                            Collections.emptyMap())
                            .withError(new Throwable("test exception")))
            );
        }
    }

    @ParameterizedTest
    @ArgumentsSource(MessageTypeSource.class)
    <T> void testSerialisingAndDeseralising(final T message) throws JsonProcessingException {
        // Given some test data

        // When a Request is serialised and deserialised
        String serialisedRequest = MAPPER.writeValueAsString(message);
        Object deserialisedRequest = MAPPER.readValue(serialisedRequest, message.getClass());

        // Then the deserialised object is unchanged (equal)
        assertThat(deserialisedRequest)
                .as("Check that the request has been deserialised correctly")
                .usingRecursiveComparison()
                .ignoringFieldsOfTypes(Throwable.class)
                .isEqualTo(message);

        if (deserialisedRequest instanceof AuditErrorMessage) {
            assertThat(deserialisedRequest)
                    .as("Check that the AuditErrorMessage contains all the correct information after deseralising")
                    .extracting("Error")
                    .isInstanceOf(Throwable.class)
                    .extracting("Message")
                    .isEqualTo("test exception");
        }
    }
}
