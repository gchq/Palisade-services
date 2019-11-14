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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.JsonNode;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertEquals;
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
        assertThat("ErrorDetails could not be parsed to json",
                StreamSupport.stream(iterable.spliterator(), false).collect(Collectors.joining(", ")),
                is(equalTo("date, message, details, stackTrace")));
    }

    @Test
    public void ErrorDetailsFromJsonTest() throws IOException {
        // Given
        final String jsonString = "{\"date\":1546300800000,\"message\":\"Test Message\",\"details\":\"Test Details\",\"stackTrace\":[]}";

        // When
        ErrorDetails result = this.mapper.readValue(jsonString, ErrorDetails.class);

        // Then
        assertThat("ErrorDetails could not be parsed from json",
                result.getDate(),
                equalTo(new Date(1546300800000L)));
        assertThat("ErrorDetails could not be parsed from json",
                result.getMessage(),
                equalTo("Test Message"));
        assertThat("ErrorDetails could not be parsed from json",
                result.getDetails(),
                equalTo("Test Details"));
    }

    @Test
    public void toStringTest() {
        // Given
        final ErrorDetails details = new ErrorDetails();
        details.setDate(new Date(1546300800000L));
        details.setMessage("Test Message");
        details.setDetails("Test Details");
        details.setStackTrace(Arrays.asList(stackTrace));

        String expected = "ErrorDetails[date=Tue Jan 01 00:00:00 GMT 2019,message=Test Message,details=Test Details,stackTrace=[]]";

        // When
        String actual = details.toString();

        // Then
        assertEquals(actual, expected);
    }
}
