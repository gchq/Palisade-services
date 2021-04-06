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
package uk.gov.gchq.palisade.component.palisade.web;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import uk.gov.gchq.palisade.component.palisade.CommonTestData;
import uk.gov.gchq.palisade.service.palisade.model.PalisadeClientResponse;
import uk.gov.gchq.palisade.service.palisade.service.PalisadeService;
import uk.gov.gchq.palisade.service.palisade.web.PalisadeRestController;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Component tests for PalisadeRestController.
 */
@WebMvcTest(controllers = {PalisadeRestController.class})
@ContextConfiguration(classes = {RestControllerWebMvcTest.class, PalisadeRestController.class})
class RestControllerWebMvcTest extends CommonTestData {

    @MockBean
    private PalisadeService palisadeService;
    @Autowired
    private PalisadeRestController controller;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        Mockito.when(palisadeService.registerDataRequest(any()))
                .thenReturn(CompletableFuture.completedFuture(COMMON_UUID.toString()));
    }

    @AfterEach
    void tearDown() {
        Mockito.reset(palisadeService);
    }

    @Test
    void testContextLoads() {
        assertThat(controller)
                .as("Check that the controller has been autowired successfully")
                .isNotNull();

        assertThat(mockMvc)
                .as("Check that mockMvc has been autowired successfully")
                .isNotNull();

        assertThat(mapper)
                .as("Check that the Object Mapper has been autowired successfully")
                .isNotNull();
    }


    /**
     * Tests that when a post is sent to the rest endpoint, a valid response is returned, and the body contains a token
     *
     * @throws Exception if there is an issue deseralising the object, or if the mockMVC.perform fails
     */
    @Test
    void testControllerReturnsAccepted() throws Exception {
        // When a request comes in to the controller
        MvcResult result = this.mockMvc.perform(post("/api/registerDataRequest")
                .headers(new HttpHeaders())
                .content(mapper.writeValueAsString(PALISADE_REQUEST))
                .contentType("application/json"))
                .andExpect(MockMvcResultMatchers.status().isAccepted())
                .andDo(print())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(content().string(containsString(COMMON_UUID.toString())))
                .andReturn();

        var response = mapper.readValue(result.getResponse().getContentAsString(), PalisadeClientResponse.class);

        //THEN
        Mockito.verify(palisadeService, times(1)).registerDataRequest(PALISADE_REQUEST);
        assertThat(response.getToken())
                .as("Check the token returned in the response is the one we expect")
                .isEqualTo(COMMON_UUID.toString());
    }

    /**
     * Test the error handling service gets called when there is an issue with the processing of a request.
     * The expectation is that the response for the request will return HTTP error with at 500 status, and the
     * ErrorHandlingService reportError method is executed.
     *
     * @throws Exception when test fails to execute
     */
    @Test
    void testControllerErrorHandling() throws Exception {
        Mockito.when(palisadeService.registerDataRequest(any()))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Something went wrong")));
        Mockito.when(palisadeService.errorMessage(any(), any(), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(null));

        MvcResult result = this.mockMvc.perform(post("/api/registerDataRequest")
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding(StandardCharsets.UTF_8.name())
                .headers(new HttpHeaders())
                .content(mapper.writeValueAsString(PALISADE_REQUEST)))
                .andExpect(status().is5xxServerError()) // a 500 server error
                .andDo(print())
                .andExpect(jsonPath(KEY_NULL_VALUE).doesNotExist()) // no message
                .andReturn();
        String response = result.getResponse().getContentAsString();

        // Verify the response value is empty
        assertThat(response)
                .as("Check the response value is empty")
                .isEmpty();

        // Verify the service methods have been called once, and only once
        Mockito.verify(palisadeService, times(1)).registerDataRequest(PALISADE_REQUEST);
        Mockito.verify(palisadeService, times(1)).errorMessage(any(), anyString(), any(), any());
    }
}
