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

package uk.gov.gchq.palisade.component.user.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import uk.gov.gchq.palisade.service.user.ApplicationTestData;
import uk.gov.gchq.palisade.service.user.model.Token;
import uk.gov.gchq.palisade.service.user.stream.ConsumerTopicConfiguration;
import uk.gov.gchq.palisade.service.user.stream.ProducerTopicConfiguration.Topic;
import uk.gov.gchq.palisade.service.user.stream.config.AkkaSystemConfig;
import uk.gov.gchq.palisade.service.user.web.UserRestController;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

@WebMvcTest(controllers = {UserRestController.class})
@ContextConfiguration(classes = {AkkaSinkTestConfiguration.class, UserRestController.class, AkkaSystemConfig.class})
class RestControllerWebMvcTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @SpyBean
    ConsumerTopicConfiguration topicConfiguration;
    @Autowired
    private UserRestController controller;
    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        Topic topic = new Topic();
        topic.setPartitions(1);
        topic.setName("input-topic");
        Mockito.when(topicConfiguration.getTopics()).thenReturn(Collections.singletonMap("input-topic", topic));
    }

    @AfterEach
    void tearDown() {
        Mockito.reset(topicConfiguration);
    }

    @Test
    void testContextLoads() {
        assertThat(controller).isNotNull();
        assertThat(mockMvc).isNotNull();
    }

    @Test
    void testControllerReturnsAccepted() throws Exception {
        // When a request comes in to the controller
        mockMvc.perform(MockMvcRequestBuilders.post("/api/user")
                .header(Token.HEADER, ApplicationTestData.REQUEST_TOKEN)
                .content(MAPPER.writeValueAsBytes(ApplicationTestData.REQUEST))
                .contentType("application/json"))
                .andExpect(MockMvcResultMatchers.status().isAccepted());
    }

}
