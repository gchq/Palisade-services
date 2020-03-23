/*
 * Copyright 2019 Crown Copyright
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
package uk.gov.gchq.palisade.service.data.service;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ReflectionUtils;

import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.RequestId;
import uk.gov.gchq.palisade.User;
import uk.gov.gchq.palisade.reader.common.DataReader;
import uk.gov.gchq.palisade.reader.exception.NoCapacityException;
import uk.gov.gchq.palisade.reader.request.DataReaderRequest;
import uk.gov.gchq.palisade.reader.request.DataReaderResponse;
import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.rule.Rules;
import uk.gov.gchq.palisade.service.data.request.AuditRequest.ReadRequestExceptionAuditRequest;
import uk.gov.gchq.palisade.service.data.request.GetDataRequestConfig;
import uk.gov.gchq.palisade.service.data.request.NoInputReadResponse;
import uk.gov.gchq.palisade.service.data.request.ReadRequest;
import uk.gov.gchq.palisade.service.request.DataRequestConfig;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(JUnit4.class)
public class SimpleDataServiceTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleDataServiceTest.class);

    private AuditService auditService;
    private PalisadeService palisadeService;
    private SimpleDataService simpleDataService;
    private DataReader dataReader;
    String token = "token";
    @Mock
    ReadRequest readRequest;
    @Mock
    RequestId requestId;
    @Mock
    LeafResource leafResource;


    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mockOtherServices();
        simpleDataService = new SimpleDataService(auditService, palisadeService, dataReader);
        LOGGER.info("Simple Data Service created: {}", simpleDataService);
    }

    @Test
    public void auditRequestReceivedExceptionTest() {
        //given
        ReadRequest readRequest = Mockito.mock(ReadRequest.class);
        Throwable throwable = Mockito.mock(Throwable.class);
        Method method = ReflectionUtils.findMethod(SimpleDataService.class, "auditRequestReceivedException", ReadRequest.class, Throwable.class);
        ReflectionUtils.makeAccessible(method);
        when(readRequest.getToken()).thenReturn(token);
        when(readRequest.getOriginalRequestId()).thenReturn(requestId);
        when(readRequest.getResource()).thenReturn(leafResource);


        //when
        ReflectionUtils.invokeMethod(method, simpleDataService, readRequest, throwable);


        //then
        ArgumentCaptor<ReadRequestExceptionAuditRequest> readRequestExceptionAuditRequestArgumentCaptor = ArgumentCaptor.forClass(ReadRequestExceptionAuditRequest.class);
        verify(auditService, times(1)).audit(readRequestExceptionAuditRequestArgumentCaptor.capture());
        ReadRequestExceptionAuditRequest readRequestExceptionAuditRequest = readRequestExceptionAuditRequestArgumentCaptor.getValue();

        Field tokenField = ReflectionUtils.findField(ReadRequestExceptionAuditRequest.class, "token");
        ReflectionUtils.makeAccessible(tokenField);
        String generatedToken = (String) ReflectionUtils.getField(tokenField, readRequestExceptionAuditRequest);
        assertEquals(token, generatedToken);

        Field leafField = ReflectionUtils.findField(ReadRequestExceptionAuditRequest.class, "leafResource");
        ReflectionUtils.makeAccessible(leafField);
        LeafResource generatedLeafResource = (LeafResource) ReflectionUtils.getField(leafField, readRequestExceptionAuditRequest);
        assertEquals(leafResource, generatedLeafResource);

        Field exceptionField = ReflectionUtils.findField(ReadRequestExceptionAuditRequest.class, "exception");
        ReflectionUtils.makeAccessible(exceptionField);
        Throwable generatedException = (Throwable) ReflectionUtils.getField(exceptionField, readRequestExceptionAuditRequest);
        assertEquals(throwable, generatedException);
    }

    @Test
    public void getPalisadeServiceTest() {
        //given

        //when
        PalisadeService test = simpleDataService.getPalisadeService();

        //Then
        assertEquals(test, palisadeService);
    }

    @Test
    public void getDataReaderTest() {
        //given

        //when
        DataReader test = simpleDataService.getDataReader();

        //Then
        assertEquals(test, dataReader);
    }

    @Test
    public void getAuditServiceTest() {
        //given

        //when
        AuditService test = simpleDataService.getAuditService();

        //Then
        assertEquals(test, auditService);
    }

    @Ignore
    @Test
    public void readTest() throws IOException {
        //given
        when(readRequest.getToken()).thenReturn(token);
        when(readRequest.getOriginalRequestId()).thenReturn(requestId);
        when(readRequest.getResource()).thenReturn(leafResource);
        DataReaderResponse dataReaderResponse = Mockito.mock(DataReaderResponse.class);
        when(dataReader.read(any(DataReaderRequest.class), any(), any())).thenReturn(dataReaderResponse);
        DataRequestConfig dataRequestConfig = Mockito.mock(DataRequestConfig.class);
        when(dataRequestConfig.getOriginalRequestId()).thenReturn(requestId);
        User user = Mockito.mock(User.class);
        when(dataRequestConfig.getUser()).thenReturn(user);
        Context context = Mockito.mock(Context.class);
        when(dataRequestConfig.getContext()).thenReturn(context);
        Map<LeafResource, Rules> leafResourceToRules = new HashMap<>();
        Rules rules = Mockito.mock(Rules.class);
        leafResourceToRules.put(leafResource, rules);
        when(dataRequestConfig.getRules()).thenReturn(leafResourceToRules);
        CompletableFuture<DataRequestConfig> dataRequestConfigCompletableFuture = CompletableFuture.supplyAsync(() -> dataRequestConfig);
        when(palisadeService.getDataRequestConfig(any(GetDataRequestConfig.class))).thenReturn(dataRequestConfigCompletableFuture);
        //given
        PipedInputStream expectedStream = new PipedInputStream();
        PipedOutputStream expectedInputSinker = new PipedOutputStream(expectedStream);
        new NoInputReadResponse(dataReaderResponse).writeTo(expectedInputSinker);

        //when
        Consumer<OutputStream> readCallback = simpleDataService.read(readRequest);
        PipedInputStream inputStream = new PipedInputStream();
        PipedOutputStream outputSink = new PipedOutputStream(inputStream);
        readCallback.accept(outputSink);

        //then
        assertThat(inputStream.readAllBytes(), equalTo(expectedStream.readAllBytes()));
    }

    @Ignore
    @Test
    public void readExceptionTest() {
        //given
        when(readRequest.getToken()).thenReturn(token);
        when(readRequest.getOriginalRequestId()).thenReturn(requestId);
        when(readRequest.getResource()).thenReturn(leafResource);
        DataReaderResponse dataReaderResponse = Mockito.mock(DataReaderResponse.class);
        when(dataReader.read(any(DataReaderRequest.class), any(), any())).thenReturn(dataReaderResponse);
        DataRequestConfig dataRequestConfig = Mockito.mock(DataRequestConfig.class);
        User user = Mockito.mock(User.class);
        when(dataRequestConfig.getUser()).thenReturn(user);
        Context context = Mockito.mock(Context.class);
        when(dataRequestConfig.getContext()).thenReturn(context);
        Map<LeafResource, Rules> leafResourceToRules = new HashMap<>();
        Rules rules = Mockito.mock(Rules.class);
        leafResourceToRules.put(leafResource, rules);
        when(dataRequestConfig.getRules()).thenReturn(leafResourceToRules);
        when(palisadeService.getDataRequestConfig(any(GetDataRequestConfig.class))).thenThrow(new NoCapacityException("failed"));

        //when
        try {
            Consumer<OutputStream> readCallback = simpleDataService.read(readRequest);
            PipedInputStream inputStream = new PipedInputStream();
            PipedOutputStream outputSink = new PipedOutputStream(inputStream);
            readCallback.accept(outputSink);
            fail();
        } catch (Exception ex) {
        }

        //then
        ArgumentCaptor<ReadRequestExceptionAuditRequest> readRequestExceptionAuditRequestArgumentCaptor = ArgumentCaptor.forClass(ReadRequestExceptionAuditRequest.class);
        verify(auditService, times(1)).audit(readRequestExceptionAuditRequestArgumentCaptor.capture());
        ReadRequestExceptionAuditRequest readRequestExceptionAuditRequest = readRequestExceptionAuditRequestArgumentCaptor.getValue();

        Field tokenField = ReflectionUtils.findField(ReadRequestExceptionAuditRequest.class, "token");
        ReflectionUtils.makeAccessible(tokenField);
        String generatedToken = (String) ReflectionUtils.getField(tokenField, readRequestExceptionAuditRequest);
        assertEquals(token, generatedToken);

        Field leafField = ReflectionUtils.findField(ReadRequestExceptionAuditRequest.class, "leafResource");
        ReflectionUtils.makeAccessible(leafField);
        LeafResource generatedLeafResource = (LeafResource) ReflectionUtils.getField(leafField, readRequestExceptionAuditRequest);
        assertEquals(leafResource, generatedLeafResource);

    }

    private void mockOtherServices() {
        auditService = Mockito.mock(AuditService.class);
        palisadeService = Mockito.mock(PalisadeService.class);
        dataReader = Mockito.mock(DataReader.class);
    }

}