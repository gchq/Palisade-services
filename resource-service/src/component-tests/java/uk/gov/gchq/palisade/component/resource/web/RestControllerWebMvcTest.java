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

package uk.gov.gchq.palisade.component.resource.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.service.resource.model.ResourceRequest;
import uk.gov.gchq.palisade.service.resource.model.Token;
import uk.gov.gchq.palisade.service.resource.service.KafkaProducerService;
import uk.gov.gchq.palisade.service.resource.web.ResourceRestController;
import uk.gov.gchq.palisade.user.User;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

@WebMvcTest(controllers = {ResourceRestController.class})
@ContextConfiguration(classes = {RestControllerWebMvcTest.class, ResourceRestController.class})
class RestControllerWebMvcTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @MockBean
    KafkaProducerService kafkaProducerService;

    @Autowired
    private ResourceRestController controller;

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        Mockito.when(kafkaProducerService.resourceRequestMulti(Mockito.any(), Mockito.any()))
                .thenReturn(CompletableFuture.completedFuture(null));
    }

    @AfterEach
    void tearDown() {
        Mockito.reset(kafkaProducerService);
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

    @Test
    void testControllerReturnsAccepted() throws Exception {
        String REQUEST_TOKEN = "test-request-token";

        var resourceRequest = ResourceRequest.Builder.create()
                .withUserId("test-user-id")
                .withResourceId("/test/resourceId")
                .withContext(new Context().purpose("test-purpose"))
                .withUser(new User().userId("test-user-id"));

        // When a request comes in to the controller
        var response = mockMvc.perform(MockMvcRequestBuilders.post("/api/resource")
                .header(Token.HEADER, REQUEST_TOKEN)
                .content(MAPPER.writeValueAsBytes(resourceRequest))
                .contentType("application/json"))
                .andExpect(MockMvcResultMatchers.status().isAccepted())
                .andReturn();

        // Read the response body
        var responseBody = response.getResponse().getContentAsString();
        assertThat(responseBody)
                .as("Check that the response body has been returned but is empty")
                .isEmpty();
    }
}
