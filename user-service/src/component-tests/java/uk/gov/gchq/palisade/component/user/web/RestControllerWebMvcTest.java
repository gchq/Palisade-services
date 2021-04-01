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

package uk.gov.gchq.palisade.component.user.web;

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

import uk.gov.gchq.palisade.component.user.common.CommonTestData;
import uk.gov.gchq.palisade.service.user.common.Token;
import uk.gov.gchq.palisade.service.user.service.KafkaProducerService;
import uk.gov.gchq.palisade.service.user.web.UserRestController;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

@WebMvcTest(controllers = {UserRestController.class})
@ContextConfiguration(classes = {RestControllerWebMvcTest.class, UserRestController.class})
class RestControllerWebMvcTest extends CommonTestData {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @MockBean
    KafkaProducerService kafkaProducerService;

    @Autowired
    private UserRestController controller;

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        Mockito.when(kafkaProducerService.processRequest(Mockito.any(), Mockito.any()))
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
        // When a request comes in to the controller
        mockMvc.perform(MockMvcRequestBuilders.post("/api/user")
                .header(Token.HEADER, REQUEST_TOKEN)
                .content(MAPPER.writeValueAsBytes(USER_REQUEST))
                .contentType("application/json"))
                .andExpect(MockMvcResultMatchers.status().isAccepted());
    }

}
