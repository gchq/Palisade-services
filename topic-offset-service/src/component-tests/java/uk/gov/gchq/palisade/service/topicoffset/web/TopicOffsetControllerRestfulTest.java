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
package uk.gov.gchq.palisade.service.topicoffset.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import uk.gov.gchq.palisade.service.topicoffset.message.StreamMarker;
import uk.gov.gchq.palisade.service.topicoffset.message.Token;
import uk.gov.gchq.palisade.service.topicoffset.message.TopicOffsetRequest;
import uk.gov.gchq.palisade.service.topicoffset.message.TopicOffsetResponse;
import uk.gov.gchq.palisade.service.topicoffset.service.ErrorHandlingService;
import uk.gov.gchq.palisade.service.topicoffset.service.TopicOffsetService;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Component tests for TopicOffsetController.
 * A request of type {@link TopicOffsetRequest} as a Json string, and HTTP header with a required Token
 * and optionally a StreamMarker is sent.  The response will either be a {@link TopicOffsetResponse} or null
 */
@WebMvcTest(TopicOffsetController.class)
public class TopicOffsetControllerRestfulTest {

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TopicOffsetService topicOffsetService;

    @MockBean
    private ErrorHandlingService errorHandlingService;

    public static final TopicOffsetResponse startResponse = TopicOffsetResponse.Builder.create().withOffset(1111L);

    public static final String SERVICE_ENDPOINT_URL = "/stream-api/topicOffset";

    /**
     * This permutation is for a request that comes in for a start of messages indicator, StreamMarker is set to START.
     * Test should accept an incoming request including TopicOffsetRequest, Token, and a StreamMarker as a Post at
     * the the URL "/stream-api/topicOffset" as a Json string. Response should be a TopicOffsetResponse.
     *
     * @throws Exception if it fails to process the request
     */
    @Test
    void testControllerWithAStartMessage() throws Exception {

        TopicOffsetController topicOffsetController = new TopicOffsetController(topicOffsetService, errorHandlingService);

        Optional<TopicOffsetRequest> requestBody = null;
        HttpHeaders headers = new HttpHeaders();
        headers.add(Token.HEADER, "test-request-token");
        headers.add(StreamMarker.HEADER, StreamMarker.START.toString());

        Mockito.when(topicOffsetService.createTopicOffsetResponse(any())).thenReturn(startResponse);


        MvcResult result = this.mockMvc.perform(post(SERVICE_ENDPOINT_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding(StandardCharsets.UTF_8.name())
                .headers(headers))
                .andExpect(status().isAccepted())
                .andDo(print())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andReturn();

        TopicOffsetResponse response = mapper.readValue(result.getResponse().getContentAsString(), TopicOffsetResponse.class);
        assertThat(response.getCommitOffset()).isEqualTo(startResponse.getCommitOffset());
    }


    /**
     * This permutation is for a request that comes in for a start of messages indicator, StreamMarker is set to END.
     * Test should accept an incoming request including TopicOffsetRequest, Token, and a StreamMarker as a Post to
     * the the URL "/stream-api/topicOffset" as a Json String. Response should be a null.
     *
     * @throws Exception if it fails to process the request
     */
    @Test
    void testControllerWithAnEndMessage() throws Exception {

        TopicOffsetController topicOffsetController = new TopicOffsetController(topicOffsetService, errorHandlingService);

        Optional<TopicOffsetRequest> requestBody = null;
        HttpHeaders headers = new HttpHeaders();
        headers.add(Token.HEADER, "test-request-token");
        headers.add(StreamMarker.HEADER, StreamMarker.END.toString());

        Mockito.when(topicOffsetService.createTopicOffsetResponse(any())).thenReturn(null);

        MvcResult result = this.mockMvc.perform(post(SERVICE_ENDPOINT_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding(StandardCharsets.UTF_8.name())
                .headers(headers))
                .andExpect(status().isAccepted())
                .andDo(print())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andReturn();

        TopicOffsetResponse response = mapper.readValue(result.getResponse().getContentAsString(), TopicOffsetResponse.class);

        assertThat(response).isNull();
    }

    /**
     * This permutation is for a request that comes in for a standard message, containing the data related to the
     * client's request and no StreamMarker in the headers.
     * Test should accept an incoming request including TopicOffsetRequest, Token, and no StreamMarker as a Post at
     * the the URL "/stream-api/topicOffset" as a Json String. Response should be a null.
     *
     * @throws Exception if it fails to process the request
     */
    @Test
    void testControllerWithAnStardardMessage() throws Exception {

        TopicOffsetController topicOffsetController = new TopicOffsetController(topicOffsetService, errorHandlingService);

        Optional<TopicOffsetRequest> requestBody = null;
        HttpHeaders headers = new HttpHeaders();
        headers.add(Token.HEADER, "test-request-token");

        Mockito.when(topicOffsetService.createTopicOffsetResponse(any())).thenReturn(null);


        MvcResult result = this.mockMvc.perform(post(SERVICE_ENDPOINT_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding(StandardCharsets.UTF_8.name())
                .headers(headers))
                .andExpect(status().isAccepted())
                .andDo(print())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andReturn();

        TopicOffsetResponse response = mapper.readValue(result.getResponse().getContentAsString(), TopicOffsetResponse.class);
        assertThat(response).isNull();
    }
}