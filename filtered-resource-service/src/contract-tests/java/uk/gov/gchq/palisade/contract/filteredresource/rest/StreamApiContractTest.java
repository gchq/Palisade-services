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

package uk.gov.gchq.palisade.contract.filteredresource.rest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import uk.gov.gchq.palisade.service.filteredresource.ApplicationTestData;
import uk.gov.gchq.palisade.service.filteredresource.FilteredResourceApplication;
import uk.gov.gchq.palisade.service.filteredresource.message.Token;
import uk.gov.gchq.palisade.service.filteredresource.message.TopicOffsetMessage;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = FilteredResourceApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("dbtest")
class StreamApiContractTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void contextLoads() {
        assertThat(restTemplate).isNotNull();
    }

    @Test
    void postTopicOffsetReturnsAccepted() {
        // Given we have some request data
        TopicOffsetMessage request = ApplicationTestData.OFFSET_MESSAGE;
        HttpHeaders headers = new HttpHeaders();
        headers.add(Token.HEADER, ApplicationTestData.REQUEST_TOKEN);

        // When the request is POSTed to the service's REST endpoint
        HttpEntity<TopicOffsetMessage> requestWithHeaders = new HttpEntity<>(request, headers);
        ResponseEntity<Void> response = restTemplate.postForEntity("/streamApi/topicOffset", requestWithHeaders, Void.class);

        // Then the response is as expected
        // The topic offset was accepted by the service
        assertThat(response.getStatusCodeValue()).isEqualTo(202);
    }

}
