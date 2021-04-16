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
package uk.gov.gchq.palisade.component.data.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import uk.gov.gchq.palisade.service.data.service.AuditMessageService;
import uk.gov.gchq.palisade.service.data.service.AuditableDataService;
import uk.gov.gchq.palisade.service.data.web.DataController;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.gchq.palisade.component.data.common.CommonTestData.AUDITABLE_DATA_REQUEST;
import static uk.gov.gchq.palisade.component.data.common.CommonTestData.AUDITABLE_DATA_REQUEST_WITH_ERROR;
import static uk.gov.gchq.palisade.component.data.common.CommonTestData.AUDITABLE_DATA_RESPONSE;
import static uk.gov.gchq.palisade.component.data.common.CommonTestData.DATA_REQUEST;

/**
 * Tests for the DataController web service endpoint.
 */
@WebMvcTest(DataController.class)
@ContextConfiguration(classes = {DataController.class})
class DataControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    @MockBean
    private AuditableDataService serviceMock;

    @MockBean
    private AuditMessageService auditMessageServiceMock;

    @Test
    void testContextLoads() {
        assertThat(mockMvc)
                .as("Check MockMvc has been autowired")
                .isNotNull();

        assertThat(serviceMock)
                .as("Check AuditableDataService has been autowired")
                .isNotNull();

        assertThat(auditMessageServiceMock)
                .as("Check AuditMessageService has been autowired")
                .isNotNull();
    }

    /**
     * Tests the Data Service endpoint.  It is expecting a Json string to be sent representing a DataRequestModel.
     * There are three expected service calls related to to this one web request.  The first two service calls are to
     * the {@link AuditableDataService} with the first for the authorisation for the resource request and the second
     * for the creation of an {@code OutputStream}.  The third service call is for the {@link AuditMessageService}
     * which is for sending a message to the Audit Service.
     *
     * @throws Exception if the test fails to run
     */
    @Test
    void testControllerReturnsAccepted() throws Exception {

        when(serviceMock.authoriseRequest(any()))
                .thenReturn(CompletableFuture.completedFuture(AUDITABLE_DATA_REQUEST));

        when(serviceMock.read(any(), any()))
                .thenReturn(CompletableFuture.completedFuture(AUDITABLE_DATA_RESPONSE));

        var response = mockMvc.perform(post("/read/chunked")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .characterEncoding(StandardCharsets.UTF_8.name())
                .content(mapper.writeValueAsBytes(DATA_REQUEST)))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        // Read the response body
        var responseBody = response.getResponse().getContentAsString();
        assertThat(responseBody)
                .as("Check that the response body has been returned but is empty")
                .isEmpty();

        //verifies the three service calls the Controller is expected to make
        verify(serviceMock, timeout(3000).times(1)).authoriseRequest(any());
        verify(serviceMock, timeout(3000).times(1)).read(any(), any());
        verify(auditMessageServiceMock, timeout(3000).times(1)).auditMessage(any());
    }

    /**
     * Tests the Data Service endpoint for an invalid request.  The expected response will be an HTTP error status
     * code of 500 and the body will be empty.  There will also be a call to the {@link AuditMessageService} to forward
     * a message to the Audit Service.
     *
     * @throws Exception if the test fails to run
     */
    @Test
    void testControllerWithForbiddenException() throws Exception {

        when(serviceMock.authoriseRequest(any()))
                .thenReturn(CompletableFuture.completedFuture(AUDITABLE_DATA_REQUEST_WITH_ERROR));

        when(serviceMock.read(any(), any()))
                .thenReturn(CompletableFuture.completedFuture(AUDITABLE_DATA_RESPONSE));

        mockMvc.perform(post("/read/chunked")
                .contentType("application/json")
                .characterEncoding(StandardCharsets.UTF_8.name())
                .content(mapper.writeValueAsBytes(DATA_REQUEST)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$").doesNotExist())
                .andDo(print())
                .andReturn();

        //verifies the two service calls the Controller is expected to make and confirms it does not call the read method
        verify(serviceMock, times(1)).authoriseRequest(any());
        verify(serviceMock, times(0)).read(any(), any());
        verify(auditMessageServiceMock, times(1)).auditMessage(any());
    }
}
