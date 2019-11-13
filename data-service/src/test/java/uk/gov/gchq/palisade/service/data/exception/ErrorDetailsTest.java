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

import com.google.inject.internal.cglib.core.$AbstractClassGenerator;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.JsonNode;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Date;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

@RunWith(JUnit4.class)
public class ErrorDetailsTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(ErrorDetailsTest.class);
    private final ObjectMapper mapper = new ObjectMapper();
    private StackTraceElement[] stackTrace = {};

    @Test
    public void ErrorDetailsToJsonTest() throws IOException {
        // Given
        final ErrorDetails details = new ErrorDetails(new Date(1546300800), "Test Message", "Test Details", stackTrace);

        // When
        JsonNode node = this.mapper.readTree(this.mapper.writeValueAsString(details));
        Iterable<String> iterable = node::fieldNames;

        // Then
        LOGGER.debug("Json String: {}", node.toString());
        assertThat("ErrorDetails could not be parsed to json",
                StreamSupport.stream(iterable.spliterator(), false).collect(Collectors.joining(", ")),
                is(equalTo("date, message, details, stackTrace")));
    }

    @Test
    public void ErrorDetailsFromJsonTest() throws IOException {
        // Given
        final ErrorDetails details = new ErrorDetails(new Date(1546300800), "Test Message", "Test Details", stackTrace);

        final String jsonString = "{\"date\":1546300800,\"message\":\"Test Message\",\"details\":\"Test Details\",\"stackTrace\":[]}";

        // When
        ErrorDetails result = this.mapper.readValue(jsonString, ErrorDetails.class);

        // Then
        assertThat("ErrorDetails could not be parsed from json",
                result.getDate(),
                equalTo(new Date(1546300800)));
        assertThat("ErrorDetails could not be parsed from json",
                result.getMessage(),
                equalTo("Test Message"));
        assertThat("ErrorDetails could not be parsed from json",
                result.getDetails(),
                equalTo("Test Details"));
    }
}
