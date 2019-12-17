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

import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.RequestId;
import uk.gov.gchq.palisade.User;
import uk.gov.gchq.palisade.reader.common.AuditRequestCompleteReceiver;
import uk.gov.gchq.palisade.reader.common.DataReader;
import uk.gov.gchq.palisade.reader.request.DataReaderRequest;
import uk.gov.gchq.palisade.reader.request.DataReaderResponse;
import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.rule.Rules;
import uk.gov.gchq.palisade.service.data.repository.BackingStore;
import uk.gov.gchq.palisade.service.data.repository.SimpleCacheService;
import uk.gov.gchq.palisade.service.data.request.AuditRequest.ReadRequestExceptionAuditRequest;
import uk.gov.gchq.palisade.service.data.request.AuditRequestReceiver;
import uk.gov.gchq.palisade.service.data.request.GetDataRequestConfig;
import uk.gov.gchq.palisade.service.data.request.NoInputReadResponse;
import uk.gov.gchq.palisade.service.data.request.ReadRequest;
import uk.gov.gchq.palisade.service.data.request.ReadResponse;
import uk.gov.gchq.palisade.service.request.DataRequestConfig;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(JUnit4.class)
public class SimpleDataServiceTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleDataServiceTest.class);

    private AuditService auditService;
    private SimpleCacheService cacheService;
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


    private class AuditRequestReceiverTest extends AuditRequestReceiver {

        public AuditRequestReceiverTest(AuditService auditService) {
            super(auditService);
        }
    }

    private AuditRequestReceiver auditRequestReceiver;


    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        setupCacheService();
        mockOtherServices();
        simpleDataService = new SimpleDataService(cacheService, auditService, palisadeService, dataReader, auditRequestReceiver);
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
        {
            Field field = ReflectionUtils.findField(ReadRequestExceptionAuditRequest.class, "token");
            ReflectionUtils.makeAccessible(field);
            String generatedToken = (String) ReflectionUtils.getField(field, readRequestExceptionAuditRequest);
            assertEquals(token, generatedToken);
        }

        {
            Field field = ReflectionUtils.findField(ReadRequestExceptionAuditRequest.class, "leafResource");
            ReflectionUtils.makeAccessible(field);
            LeafResource generatedLeafResource = (LeafResource) ReflectionUtils.getField(field, readRequestExceptionAuditRequest);
            assertEquals(leafResource, generatedLeafResource);
        }

        {
            Field field = ReflectionUtils.findField(ReadRequestExceptionAuditRequest.class, "exception");
            ReflectionUtils.makeAccessible(field);
            Throwable generatedException = (Throwable) ReflectionUtils.getField(field, readRequestExceptionAuditRequest);
            assertEquals(throwable, generatedException);
        }
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
    public void getCacheServiceTest() {
        //given

        //when
        CacheService test = simpleDataService.getCacheService();

        //Then
        assertEquals(test, cacheService);
    }


    @Test
    public void getAuditServiceTest() {
        //given

        //when
        AuditService test = simpleDataService.getAuditService();

        //Then
        assertEquals(test, auditService);
    }


    @Test
    public void readTest() {
        //given
        when(readRequest.getToken()).thenReturn(token);
        when(readRequest.getOriginalRequestId()).thenReturn(requestId);
        when(readRequest.getResource()).thenReturn(leafResource);
        DataReaderResponse dataReaderResponse = Mockito.mock(DataReaderResponse.class);
        when(dataReader.read(any(DataReaderRequest.class), any(), any(AuditRequestCompleteReceiver.class))).thenReturn(dataReaderResponse);

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

        //when
        CompletableFuture<ReadResponse> readResponseCompletableFuture = simpleDataService.read(readRequest);
        ReadResponse readResponse = readResponseCompletableFuture.join();

        //then
        assertEquals(readResponse.getClass(), NoInputReadResponse.class);
        NoInputReadResponse noInputReadResponse = (NoInputReadResponse) readResponse;
        {
            Field field = ReflectionUtils.findField(NoInputReadResponse.class, "readerResponse");
            ReflectionUtils.makeAccessible(field);
            DataReaderResponse generatedDataReaderResponse = (DataReaderResponse) ReflectionUtils.getField(field, noInputReadResponse);
            assertEquals(dataReaderResponse, generatedDataReaderResponse);
        }
    }

    @Test
    public void readExceptionTest() {
        //given
        when(readRequest.getToken()).thenReturn(token);
        when(readRequest.getOriginalRequestId()).thenReturn(requestId);
        when(readRequest.getResource()).thenReturn(leafResource);
        DataReaderResponse dataReaderResponse = Mockito.mock(DataReaderResponse.class);
        when(dataReader.read(any(DataReaderRequest.class), any(), any(AuditRequestCompleteReceiver.class))).thenReturn(dataReaderResponse);
        DataRequestConfig dataRequestConfig = Mockito.mock(DataRequestConfig.class);
        User user = Mockito.mock(User.class);
        when(dataRequestConfig.getUser()).thenReturn(user);
        Context context = Mockito.mock(Context.class);
        when(dataRequestConfig.getContext()).thenReturn(context);
        Map<LeafResource, Rules> leafResourceToRules = new HashMap<>();
        Rules rules = Mockito.mock(Rules.class);
        leafResourceToRules.put(leafResource, rules);
        when(dataRequestConfig.getRules()).thenReturn(leafResourceToRules);
        CompletableFuture<DataRequestConfig> dataRequestConfigCompletableFuture = CompletableFuture.supplyAsync(() -> {
            throw new RuntimeException("failed");
        });
        when(palisadeService.getDataRequestConfig(any(GetDataRequestConfig.class))).thenReturn(dataRequestConfigCompletableFuture);

        //when
        CompletableFuture<ReadResponse> readResponseCompletableFuture = simpleDataService.read(readRequest);
        Boolean success = false;
        try {
            ReadResponse readResponse = readResponseCompletableFuture.join();
        } catch (RuntimeException ex) {
            success = true; //we're expecting an exception because the completablefuture is throwing an exception
        }

        assertEquals(success, true);

        //then
        ArgumentCaptor<ReadRequestExceptionAuditRequest> readRequestExceptionAuditRequestArgumentCaptor = ArgumentCaptor.forClass(ReadRequestExceptionAuditRequest.class);
        verify(auditService, times(1)).audit(readRequestExceptionAuditRequestArgumentCaptor.capture());
        ReadRequestExceptionAuditRequest readRequestExceptionAuditRequest = readRequestExceptionAuditRequestArgumentCaptor.getValue();
        {
            Field field = ReflectionUtils.findField(ReadRequestExceptionAuditRequest.class, "token");
            ReflectionUtils.makeAccessible(field);
            String generatedToken = (String) ReflectionUtils.getField(field, readRequestExceptionAuditRequest);
            assertEquals(token, generatedToken);
        }

        {
            Field field = ReflectionUtils.findField(ReadRequestExceptionAuditRequest.class, "leafResource");
            ReflectionUtils.makeAccessible(field);
            LeafResource generatedLeafResource = (LeafResource) ReflectionUtils.getField(field, readRequestExceptionAuditRequest);
            assertEquals(leafResource, generatedLeafResource);
        }
    }

    private void setupCacheService() {
        final BackingStore store = Mockito.mock(BackingStore.class);
        cacheService = new SimpleCacheService();
        cacheService.backingStore(store);
    }

    private void mockOtherServices() {
        auditService = Mockito.mock(AuditService.class);
        auditRequestReceiver = new AuditRequestReceiverTest(auditService);
        palisadeService = Mockito.mock(PalisadeService.class);
        dataReader = Mockito.mock(DataReader.class);
    }


}