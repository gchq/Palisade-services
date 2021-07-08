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

package uk.gov.gchq.palisade.component.audit.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import uk.gov.gchq.palisade.component.audit.CommonTestData;
import uk.gov.gchq.palisade.service.audit.model.Token;
import uk.gov.gchq.palisade.service.audit.service.KafkaProducerService;
import uk.gov.gchq.palisade.service.audit.web.AuditRestController;

import static org.assertj.core.api.Assertions.assertThat;

@WebMvcTest(controllers = {AuditRestController.class})
@ContextConfiguration(classes = {RestControllerWebMvcTest.class, AuditRestController.class})
class RestControllerWebMvcTest extends CommonTestData {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @MockBean
    KafkaProducerService kafkaProducerService;
    @Autowired
    private AuditRestController controller;
    @Autowired
    private MockMvc mockMvc;

    @Test
    void testContextLoads() {
        assertThat(controller)
                .as("Check that the AuditRestController has started successfully")
                .isNotNull();
        assertThat(mockMvc)
                .as("Check that MockMvc has started successfully")
                .isNotNull();
    }

    @Test
    void testControllerReturnsAcceptedForSuccessMessage() throws Exception {
        // When a request comes in to the controller
        var response = mockMvc.perform(MockMvcRequestBuilders.post("/api/success")
                .header(Token.HEADER, REQUEST_TOKEN)
                .content(MAPPER.writeValueAsBytes(AUDIT_SUCCESS_MESSAGE))
                .contentType("application/json"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();

        // Read the response body
        assertThat(response.getResponse().getContentAsString())
                .as("Check that the response body has been returned but is empty")
                .isEmpty();
    }

    @Test
    void testControllerReturnsAcceptedForErrorMessage() throws Exception {
        // When a request comes in to the controller
        var response = mockMvc.perform(MockMvcRequestBuilders.post("/api/error")
                .header(Token.HEADER, REQUEST_TOKEN)
                .content(MAPPER.writeValueAsBytes(AUDIT_ERROR_MESSAGE))
                .contentType("application/json"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();

        // Read the response body
        assertThat(response.getResponse().getContentAsString())
                .as("Check that the response body has been returned but is empty")
                .isEmpty();
    }
}
