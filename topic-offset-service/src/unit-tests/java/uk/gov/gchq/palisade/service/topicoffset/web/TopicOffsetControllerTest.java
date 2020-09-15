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


import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.gchq.palisade.service.topicoffset.TopicOffsetApplication;
import uk.gov.gchq.palisade.service.topicoffset.request.StreamMarker;
import uk.gov.gchq.palisade.service.topicoffset.request.Token;
import uk.gov.gchq.palisade.service.topicoffset.request.TopicOffsetRequest;
import uk.gov.gchq.palisade.service.topicoffset.request.TopicOffsetResponse;
import uk.gov.gchq.palisade.service.topicoffset.service.ErrorHandlingService;
import uk.gov.gchq.palisade.service.topicoffset.service.TopicOffsetService;

import java.util.Optional;

/**
 * Unit test for the TopicOffsetController.
 */

@WebMvcTest(TopicOffsetController.class)
//@ContextConfiguration(classes = TopicOffsetApplication.class)

class TopicOffsetControllerTest {


    @Autowired
    private MockMvc mockMvc;

    @MockBean
    TopicOffsetService topicOffsetService;

    @MockBean
    ErrorHandlingService errorHandlingService;

    /**
     * Test for when there is a start marker indicating that this is the beginning of the messages that will come for a
     * given set of resources for a given request
     */
    @Test
    void testControllerWithAStartMessage() throws Exception {

     TopicOffsetController topicOffsetController = new TopicOffsetController(topicOffsetService, errorHandlingService);

        Optional<TopicOffsetRequest> requestBody = null;
        HttpHeaders  responseHeaders = new HttpHeaders();
        StreamMarker streamMarker = StreamMarker.START;

   //     topicOffsetController.serviceTopicOffset(
    //            "test-request-token",
     //           streamMarker,
     //           requestBody);


    }
}