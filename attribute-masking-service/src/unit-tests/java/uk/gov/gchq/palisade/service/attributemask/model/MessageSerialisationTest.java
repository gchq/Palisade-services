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
package uk.gov.gchq.palisade.service.attributemask.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import uk.gov.gchq.palisade.service.attributemask.ApplicationTestData;
import uk.gov.gchq.palisade.service.attributemask.config.ApplicationConfiguration;

import java.util.Map;
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
                            ApplicationTestData.REQUEST, Map.of("messagesSent", "23"))
                            .withError(new Throwable("test exception")))
            );
        }
    }

    @ParameterizedTest
    @ArgumentsSource(MessageTypeSource.class)
    <T> void testSerialiseDeserialiseIsConsistent(final T message) throws JsonProcessingException {
        // Given some test data

        // When a Request is serialised and deserialised
        var actualJson = MAPPER.writeValueAsString(message);
        var actualInstance = MAPPER.readValue(actualJson, message.getClass());

        // Then the deserialised object is unchanged (equal)
        assertThat(actualInstance)
                .as("Check that whilst using the objects toString method, the objects are the same")
                .isEqualTo(message);

        assertThat(actualInstance)
                .as("Ignoring the error message, check %s using recursion", message.getClass().getSimpleName())
                .usingRecursiveComparison()
                .ignoringFieldsOfTypes(Throwable.class)
                .isEqualTo(message);

        // Test the exception of the AuditErrorMessage Test data
        if (actualInstance instanceof AuditErrorMessage) {
            if (((AuditErrorMessage) actualInstance).getError() != null) {
                assertThat((AuditErrorMessage) actualInstance)
                        .as("Extracting the exception, check it has been deserialised successfully")
                        .extracting(AuditErrorMessage::getError)
                        .isExactlyInstanceOf(Throwable.class)
                        .extracting("Message")
                        .isEqualTo("test exception");
            }
        }
    }
}
