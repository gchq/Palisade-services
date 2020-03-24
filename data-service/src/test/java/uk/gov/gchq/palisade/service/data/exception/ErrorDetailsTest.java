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

package uk.gov.gchq.palisade.service.data.exception;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.JsonNode;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

@RunWith(JUnit4.class)
public class ErrorDetailsTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private StackTraceElement[] stackTrace = {};

    @Test
    public void ErrorDetailsToJsonTest() throws IOException {
        // Given
        final ZonedDateTime dateTime = ZonedDateTime.now();
        final ErrorDetails details = new ErrorDetails(dateTime, "Test Message", "Test Details", stackTrace);

        // When
        JsonNode node = this.mapper.readTree(this.mapper.writeValueAsString(details));
        Iterable<String> iterable = node::fieldNames;

        // Then
        assertThat("ErrorDetails could not be parsed to json",
                StreamSupport.stream(iterable.spliterator(), false).collect(Collectors.joining(", ")),
                is(equalTo("date, message, details, stackTrace")));
    }

    @Test
    public void ErrorDetailsFromJsonTest() throws IOException {

        // Given
        Optional<String> testDetails = Optional.of("Test Details");
        final String jsonString = "{\"date\":\"2019-01-01T00:00:00.000001Z\",\"message\":\"Test Message\",\"details\":\"Test Details\",\"stackTrace\":[]}";

        // When
        ErrorDetails result = this.mapper.readValue(jsonString, ErrorDetails.class);

        System.out.println(result.getDetails());

        // Then
        assertThat("ErrorDetails could not be parsed from json",
                result.getDate(),
                equalTo(ZonedDateTime.parse("2019-01-01T00:00:00.000001Z")));
        assertThat("ErrorDetails could not be parsed from json",
                result.getMessage(),
                equalTo("Test Message"));
        assertThat("ErrorDetails could not be parsed from json",
                result.getDetails(),
                equalTo(testDetails));
    }

}
