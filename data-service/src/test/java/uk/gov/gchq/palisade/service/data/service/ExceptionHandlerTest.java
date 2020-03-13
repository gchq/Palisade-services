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

package uk.gov.gchq.palisade.service.data.service;

import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.WebRequest;

import uk.gov.gchq.palisade.service.data.exception.NoPolicyException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

@RunWith(Theories.class)
public class ExceptionHandlerTest {
    private static Exception baseException = Mockito.mock(Exception.class);
    private WebRequest request = Mockito.mock(WebRequest.class);

    private static Map<Exception, HttpStatus> expectedStatuses = new HashMap<>();

    static {
        // Requested entity not found (policy, resource etc.)
        expectedStatuses.put(new NoPolicyException(baseException), HttpStatus.NOT_FOUND);
        expectedStatuses.put(new FileNotFoundException(), HttpStatus.NOT_FOUND);
        // Malformed request (null/empty field)
        expectedStatuses.put(new NullPointerException(), HttpStatus.BAD_REQUEST);
        // Anything else reported generic 500 error
        expectedStatuses.put(new IOException(), HttpStatus.INTERNAL_SERVER_ERROR);
        expectedStatuses.put(new RuntimeException(), HttpStatus.INTERNAL_SERVER_ERROR);
        expectedStatuses.put(new Exception(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @DataPoints
    private static Set<Exception> exceptionSet = expectedStatuses.keySet();



    @Theory
    public void handlerReturnsCorrectStatus(final Exception exception) {
        // Given
        ExceptionHandler handler = new ExceptionHandler();

        // When
        ResponseEntity<Object> responseEntity = handler.handleCustomException(exception, request);

        // Then
        HttpStatus expected = expectedStatuses.get(exception);
        assertThat(responseEntity.getStatusCode(), equalTo(expected));
    }
}
