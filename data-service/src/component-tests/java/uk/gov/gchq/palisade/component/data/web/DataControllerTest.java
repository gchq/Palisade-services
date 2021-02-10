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
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import uk.gov.gchq.palisade.service.data.service.AuditMessageService;
import uk.gov.gchq.palisade.service.data.service.AuditableDataService;
import uk.gov.gchq.palisade.service.data.web.DataController;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.gchq.palisade.component.data.common.CommonTestData.AUDITABLE_DATA_REQUEST;
import static uk.gov.gchq.palisade.component.data.common.CommonTestData.AUDITABLE_DATA_REQUEST_WITH_ERROR;
import static uk.gov.gchq.palisade.component.data.common.CommonTestData.AUDITABLE_DATA_RESPONSE;
import static uk.gov.gchq.palisade.component.data.common.CommonTestData.DATA_REQUEST;

/**
 * Tests for the DataController web service endpoint.
 */
@WebMvcTest(controllers = {DataController.class})
@ContextConfiguration(classes = {DataControllerTest.class, DataController.class})
class DataControllerTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    private DataController controller;
    @MockBean
    private AuditableDataService serviceMock;
    @MockBean
    private AuditMessageService auditMessageServiceMock;
    @Autowired
    private MockMvc mockMvc;

    @Test
    void testContextLoads() {
        assertThat(controller).isNotNull();
        assertThat(mockMvc).isNotNull();
    }

    /**
     * Tests that the endpoint is expecting a Json string for a DataRequestModel.
     * A return will be a OutputStream.
     *
     * @throws Exception if the test fails to run
     */
    @Test
    void testControllerReturnsAccepted() throws Exception {
        when(serviceMock.authoriseRequest(any()))
                .thenReturn(CompletableFuture.completedFuture(AUDITABLE_DATA_REQUEST));

        when(serviceMock.read(any(), any()))
                .thenReturn(CompletableFuture.completedFuture(AUDITABLE_DATA_RESPONSE));

        when(auditMessageServiceMock.auditMessage(any()))
                .thenReturn(CompletableFuture.completedFuture(null));

        MvcResult result = mockMvc.perform(post("/data/read/chunked")
                .contentType("application/json")
                .characterEncoding(StandardCharsets.UTF_8.name())
                .content(MAPPER.writeValueAsBytes(DATA_REQUEST)))
                .andExpect(request().asyncStarted())
                .andReturn();

        ResultActions resultActions = mockMvc.perform(asyncDispatch(result))
                .andDo(print())
                .andExpect(status().isAccepted());

        verify(serviceMock, times(1)).authoriseRequest(any());
        verify(auditMessageServiceMock, times(1)).auditMessage(any());
    }

    /**
     * Tests that the endpoint is expecting a Json string for a DataRequestModel.  A return will be a OutputStream.
     *
     * @throws Exception if the test fails to run
     */
    @Test
    void testControllerWithForbiddenException() throws Exception {
        when(serviceMock.authoriseRequest(any()))
                .thenReturn(CompletableFuture.completedFuture(AUDITABLE_DATA_REQUEST_WITH_ERROR));

        when(serviceMock.read(any(), any()))
                .thenReturn(CompletableFuture.completedFuture(AUDITABLE_DATA_RESPONSE));

        when(auditMessageServiceMock.auditMessage(any()))
                .thenReturn(CompletableFuture.completedFuture(null));

        MvcResult result = mockMvc.perform(post("/data/read/chunked")
                .contentType("application/json")
                .characterEncoding(StandardCharsets.UTF_8.name())
                .content(MAPPER.writeValueAsBytes(DATA_REQUEST)))
                .andExpect(request().asyncNotStarted())
                .andExpect(status().is5xxServerError())
                .andDo(print())
                .andReturn();

        verify(serviceMock, times(1)).authoriseRequest(any());
        verify(auditMessageServiceMock, times(1)).auditMessage(any());
    }
}
