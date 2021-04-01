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
package uk.gov.gchq.palisade.component.topicoffset.web;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import uk.gov.gchq.palisade.service.topicoffset.common.Token;
import uk.gov.gchq.palisade.service.topicoffset.model.StreamMarker;
import uk.gov.gchq.palisade.service.topicoffset.model.TopicOffsetRequest;
import uk.gov.gchq.palisade.service.topicoffset.model.TopicOffsetResponse;
import uk.gov.gchq.palisade.service.topicoffset.service.KafkaProducerService;
import uk.gov.gchq.palisade.service.topicoffset.web.TopicOffsetRestController;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Component tests for TopicOffsetRestController.
 * A request of type {@link TopicOffsetRequest} as a Json string, and HTTP header with a required Token
 * and optionally a StreamMarker is sent.  The response will either be a {@link TopicOffsetResponse} or null
 */
@WebMvcTest(controllers = {TopicOffsetRestController.class})
@ContextConfiguration(classes = {TopicOffsetRestControllerTest.class, TopicOffsetRestController.class})
class TopicOffsetRestControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private TopicOffsetRestController controller;

    @MockBean
    private KafkaProducerService serviceMock;

    public static final String SERVICE_ENDPOINT_URL = "/api/offset";
    public static final String TEST_REQUEST_TOKEN = "test-request-token";
    public static final String MESSAGE_BODY_KEY = "$";

    @BeforeEach
    void setUp() {
        Mockito.when(serviceMock.topicOffsetMulti(Mockito.any(), Mockito.any()))
                .thenReturn(CompletableFuture.completedFuture(null));
    }

    @AfterEach
    void tearDown() {
        Mockito.reset(serviceMock);
    }

    @Test
    void testContextLoads() {
        assertThat(controller)
                .as("Check that the controller is loaded correctly")
                .isNotNull();

        assertThat(mockMvc)
                .as("Check mockito has been autowired successfully")
                .isNotNull();
    }

    /**
     * This permutation is for a request that comes in for an end of messages indicator, StreamMarker is set to START.
     * Test should accept an incoming request including {@link TopicOffsetRequest}, {@link Token}, and a {@link StreamMarker}
     * as a POST to the the URL "/api/offset" as a JSON String. Response should be a 2XX status and null body.
     *
     * @throws Exception if it fails to process the request
     */
    @Test
    void testControllerWithOffsetForTopic() throws Exception {
        // Given this is an offset for the topic

        // When a request is made to the service
        HttpHeaders headers = new HttpHeaders();
        headers.add(Token.HEADER, TEST_REQUEST_TOKEN);
        headers.add(StreamMarker.HEADER, StreamMarker.START.toString());
        ResultActions action = this.mockMvc.perform(post(SERVICE_ENDPOINT_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .headers(headers));

        // Then it is accepted by the rest controller
        action.andExpect(status().isAccepted()) // a HTTP 2XX
                .andDo(print())
                .andExpect(jsonPath(MESSAGE_BODY_KEY).doesNotExist()) // no message
                .andReturn();
    }

}
