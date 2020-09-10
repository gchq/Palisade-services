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

package uk.gov.gchq.palisade.contract.attributemask.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;

import uk.gov.gchq.palisade.contract.attributemask.ApplicationTestData;
import uk.gov.gchq.palisade.service.attributemask.AttributeMaskingApplication;
import uk.gov.gchq.palisade.service.attributemask.request.AttributeMaskingRequest;
import uk.gov.gchq.palisade.service.attributemask.request.AttributeMaskingResponse;
import uk.gov.gchq.palisade.service.attributemask.request.StreamMarker;
import uk.gov.gchq.palisade.service.attributemask.request.Token;
import uk.gov.gchq.palisade.service.attributemask.service.AttributeMaskingService;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = AttributeMaskingApplication.class, webEnvironment = WebEnvironment.DEFINED_PORT)
class RestContractTest {

    @Autowired
    private TestRestTemplate restTemplate;
    @Autowired
    private AttributeMaskingService service;

    @Test
    void postToServiceReturnsMaskedResource() throws JsonProcessingException {
        // Given we have some request data (not a stream marker)
        AttributeMaskingRequest request = ApplicationTestData.REQUEST;
        HttpHeaders headers = new HttpHeaders();
        headers.add(Token.HEADER, ApplicationTestData.REQUEST_TOKEN);

        // When the request is POSTed to the service's REST endpoint
        HttpEntity<AttributeMaskingRequest> requestWithHeaders = new HttpEntity<>(request, headers);
        AttributeMaskingResponse response = restTemplate.postForObject("/stream-api/maskAttributes", requestWithHeaders, AttributeMaskingResponse.class);

        // Then the response is as expected
        // LeafResource is 'masked' by the service
        assertThat(response.getResource())
                .isEqualTo(service.maskResourceAttributes(request.getResource()));
        // Everything else is the same
        assertThat(response.getUserId()).isEqualTo(request.getUserId());
        assertThat(response.getResourceId()).isEqualTo(request.getResourceId());
        assertThat(response.getContext()).isEqualTo(request.getContext());
    }

    @Test
    void streamMarkerIsSkippedByService() {
        // Given we have some request data (a stream marker)
        AttributeMaskingRequest request = null;
        HttpHeaders headers = new HttpHeaders();
        headers.add(Token.HEADER, ApplicationTestData.REQUEST_TOKEN);
        headers.add(StreamMarker.HEADER, StreamMarker.START.toString());
        headers.add(HttpHeaders.CONTENT_TYPE, "application/json");

        // When the request is POSTed to the service's REST endpoint
        HttpEntity<AttributeMaskingRequest> requestWithHeaders = new HttpEntity<>(request, headers);
        HttpEntity<AttributeMaskingResponse> response = restTemplate.postForEntity("/stream-api/maskAttributes", requestWithHeaders, AttributeMaskingResponse.class);

        // Then the response is as expected
        // Body is null
        assertThat(response.getBody()).isNull();
        // Token header and StreamMarker header are unchanged
        assertThat(response.getHeaders().get(Token.HEADER)).isEqualTo(headers.get(Token.HEADER));
        assertThat(response.getHeaders().get(StreamMarker.HEADER)).isEqualTo(headers.get(StreamMarker.HEADER));
    }

}
