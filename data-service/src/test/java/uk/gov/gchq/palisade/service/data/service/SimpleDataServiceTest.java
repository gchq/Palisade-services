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

import uk.gov.gchq.palisade.RequestId;
import uk.gov.gchq.palisade.reader.common.DataReader;
import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.service.data.request.AuditRequest.ReadRequestExceptionAuditRequest;
import uk.gov.gchq.palisade.service.data.request.ReadRequest;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;
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

    private void mockOtherServices() {
        auditService = Mockito.mock(AuditService.class);
        palisadeService = Mockito.mock(PalisadeService.class);
        dataReader = Mockito.mock(DataReader.class);
    }

}