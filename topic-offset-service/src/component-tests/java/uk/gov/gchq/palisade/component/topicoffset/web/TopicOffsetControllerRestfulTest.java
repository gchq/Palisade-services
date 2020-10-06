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
package uk.gov.gchq.palisade.component.topicoffset.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.resource.impl.FileResource;
import uk.gov.gchq.palisade.resource.impl.SystemResource;
import uk.gov.gchq.palisade.service.SimpleConnectionDetail;
import uk.gov.gchq.palisade.service.topicoffset.TopicOffsetApplication;
import uk.gov.gchq.palisade.service.topicoffset.model.StreamMarker;
import uk.gov.gchq.palisade.service.topicoffset.model.Token;
import uk.gov.gchq.palisade.service.topicoffset.model.TopicOffsetRequest;
import uk.gov.gchq.palisade.service.topicoffset.model.TopicOffsetResponse;
import uk.gov.gchq.palisade.service.topicoffset.service.ErrorHandlingService;
import uk.gov.gchq.palisade.service.topicoffset.service.TopicOffsetService;
import uk.gov.gchq.palisade.service.topicoffset.web.TopicOffsetController;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Component tests for TopicOffsetController.
 * A request of type {@link TopicOffsetRequest} as a Json string, and HTTP header with a required Token
 * and optionally a StreamMarker is sent.  The response will either be a {@link TopicOffsetResponse} or null
 */
@ContextConfiguration(classes = TopicOffsetApplication.class)
@WebMvcTest(TopicOffsetController.class)
class TopicOffsetControllerRestfulTest {

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TopicOffsetService topicOffsetService;

    @MockBean
    private ErrorHandlingService errorHandlingService;

    public static final TopicOffsetResponse startResponse = TopicOffsetResponse.Builder.create().withOffset(123L);
    public static final String SERVICE_ENDPOINT_URL = "/stream-api/topicOffset";
    public static final String TEST_REQUEST_TOKEN = "test-request-token";
    public static final String USER_ID = "testUserId";
    public static final String RESOURCE_ID = "/test/resourceId";
    public static final String KEY_NULL_VALUE = "$.keyToNull";
    public static final String RESOURCE_TYPE = "uk.gov.gchq.palisade.test.TestType";
    public static final String RESOURCE_FORMAT = "avro";
    public static final String RESOURCE_CONNECTION = "test-data-service";
    public static final String RESOURCE_PARENT_ID = "/test";
    public static final String CONTEXT_PURPOSE = "testPurpose";

    /**
     * This permutation is for a request that comes in for a start of messages indicator, StreamMarker is set to START.
     * Test should accept an incoming request including TopicOffsetRequest, Token, and a StreamMarker as a Post at
     * the the URL "/stream-api/topicOffset" as a Json string. Response should be a TopicOffsetResponse.
     *
     * @throws Exception if it fails to process the request
     */
    @Test
    void testControllerWithAStartMessage() throws Exception {
        Optional<TopicOffsetRequest> requestBody = Optional.empty();
        HttpHeaders headers = new HttpHeaders();
        headers.add(Token.HEADER, TEST_REQUEST_TOKEN);
        headers.add(StreamMarker.HEADER, StreamMarker.START.toString());

        Mockito.when(topicOffsetService.isOffsetForTopic(any())).thenReturn(true);

        MvcResult result = this.mockMvc.perform(post(SERVICE_ENDPOINT_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding(StandardCharsets.UTF_8.name())
                .headers(headers)
                .content(mapper.writeValueAsString(requestBody)))
                .andExpect(status().isAccepted())  //a H2XX status
                .andDo(print())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andReturn();

        TopicOffsetResponse response = mapper.readValue(result.getResponse().getContentAsString(),
                TopicOffsetResponse.class);
        assertThat(response.getCommitOffset()).isEqualTo(startResponse.getCommitOffset());
    }

    /**
     * This permutation is for a request that comes in for an end of messages indicator, StreamMarker is set to END.
     * Test should accept an incoming request including TopicOffsetRequest, Token, and a StreamMarker as a Post to
     * the the URL "/stream-api/topicOffset" as a Json String. Response should be a null.
     *
     * @throws Exception if it fails to process the request
     */
    @Test
    void testControllerWithAnEndMessage() throws Exception {
        Optional<TopicOffsetRequest> requestBody = Optional.empty();
        HttpHeaders headers = new HttpHeaders();
        headers.add(Token.HEADER, TEST_REQUEST_TOKEN);
        headers.add(StreamMarker.HEADER, StreamMarker.END.toString());

        Mockito.when(topicOffsetService.isOffsetForTopic(any())).thenReturn(false);

        this.mockMvc.perform(post(SERVICE_ENDPOINT_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding(StandardCharsets.UTF_8.name())
                .headers(headers)
                .content(mapper.writeValueAsString(requestBody)))
                .andExpect(status().isAccepted()) //a HTTP 2XX
                .andDo(print())
                .andExpect(jsonPath(KEY_NULL_VALUE).doesNotExist()) //no message
                .andReturn();
    }

    /**
     * This permutation is for a request that comes in for a standard message, containing the data related to the
     * client's request and no StreamMarker in the headers.
     * Test should accept an incoming request including (optionally) TopicOffsetRequest, Token, and no StreamMarker
     * as a Post at the the URL "/stream-api/topicOffset" as a Json String. Response should be a null with HTTP code for
     * message accepted.
     *
     * @throws Exception if it fails to process the request
     */
    @Test
    void testControllerWithAnStandardMessage() throws Exception {
        Optional<TopicOffsetRequest> requestBody = Optional.empty();
        HttpHeaders headers = new HttpHeaders();
        headers.add(Token.HEADER, TEST_REQUEST_TOKEN);

        Mockito.when(topicOffsetService.isOffsetForTopic(any())).thenReturn(false);

        this.mockMvc.perform(post(SERVICE_ENDPOINT_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding(StandardCharsets.UTF_8.name())
                .headers(headers)
                .content(mapper.writeValueAsString(requestBody)))
                .andExpect(status().isAccepted())  // a HTTP 200
                .andDo(print())
                .andExpect(jsonPath(KEY_NULL_VALUE).doesNotExist())  //no message
                .andReturn();
    }

    /**
     * This permutation is for a request that throws an error during the processing of the request.
     * Test should accept an incoming request including (optionally) TopicOffsetRequest, Token, and no StreamMarker as a
     * POST message to the the URL "/stream-api/topicOffset" as a Json String. Response should be a null with HTTP code for
     * server error and the ErrorHandlingService's reportError method should be called.
     *
     * @throws Exception if it fails to process the request
     */
    @Test
    void testControllerErrorHandling() throws Exception {
        LeafResource leafResource = new FileResource()
                .id(RESOURCE_ID)
                .type(RESOURCE_TYPE)
                .serialisedFormat(RESOURCE_FORMAT)
                .connectionDetail(new SimpleConnectionDetail().serviceName(RESOURCE_CONNECTION))
                .parent(new SystemResource().id(RESOURCE_PARENT_ID));

        TopicOffsetRequest requestBody = TopicOffsetRequest.
                Builder.create().
                withUserId(USER_ID)
                .withResourceId(RESOURCE_ID)
                .withContext(new Context().purpose(CONTEXT_PURPOSE))
                .withResource(leafResource);
        HttpHeaders headers = new HttpHeaders();
        headers.add(Token.HEADER, TEST_REQUEST_TOKEN);

        RuntimeException somethingWentWrong = new RuntimeException("Something went wrong");
        Mockito.when(topicOffsetService.isOffsetForTopic(any()))
                .thenThrow(somethingWentWrong);

        this.mockMvc.perform(post(SERVICE_ENDPOINT_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding(StandardCharsets.UTF_8.name())
                .headers(headers)
                .content(mapper.writeValueAsString(requestBody)))
                .andExpect(status().is5xxServerError()) //a 500 server error
                .andDo(print())
                .andExpect(jsonPath(KEY_NULL_VALUE).doesNotExist()) //no message
                .andReturn();

        verify(errorHandlingService, times(1)).reportError(any(), any(), any());
    }
}
