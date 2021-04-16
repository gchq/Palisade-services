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

package uk.gov.gchq.palisade.component.policy.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import uk.gov.gchq.palisade.component.policy.CommonTestData;
import uk.gov.gchq.palisade.component.policy.MapperConfiguration;
import uk.gov.gchq.palisade.service.policy.common.Token;
import uk.gov.gchq.palisade.service.policy.service.KafkaProducerService;
import uk.gov.gchq.palisade.service.policy.web.PolicyRestController;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@WebMvcTest(controllers = {PolicyRestController.class})
@ContextConfiguration(classes = {RestControllerWebMvcTest.class, PolicyRestController.class, MapperConfiguration.class})
class RestControllerWebMvcTest extends CommonTestData {

    @MockBean
    private KafkaProducerService kafkaProducerService;
    @Autowired
    private PolicyRestController controller;
    @Autowired
    private MockMvc mockMvc;
    @Qualifier("objectMapper")
    @Autowired
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        Mockito.when(kafkaProducerService.policyMulti(Mockito.any(), Mockito.any()))
                .thenReturn(CompletableFuture.completedFuture(null));
    }

    @AfterEach
    void tearDown() {
        Mockito.reset(kafkaProducerService);
    }

    @Test
    void testContextLoads() {
        assertAll(
                () -> assertThat(controller)
                        .as("The 'controller' should not be null")
                        .isNotNull(),

                () -> assertThat(mockMvc)
                        .as("The 'mockMvc' should not be null")
                        .isNotNull()
        );
    }

    @Test
    void testControllerReturnsAccepted() throws Exception {
        // When a request comes in to the controller
        mockMvc.perform(MockMvcRequestBuilders.post("/api/policy")
                .header(Token.HEADER, REQUEST_TOKEN)
                .content(mapper.writeValueAsBytes(POLICY_REQUEST))
                .contentType("application/json"))
                .andExpect(MockMvcResultMatchers.status().isAccepted());
    }
}
