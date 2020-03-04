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

package uk.gov.gchq.palisade.service.palisade.service;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;
import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.RequestId;
import uk.gov.gchq.palisade.User;
import uk.gov.gchq.palisade.UserId;
import uk.gov.gchq.palisade.policy.PassThroughRule;
import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.resource.impl.DirectoryResource;
import uk.gov.gchq.palisade.resource.impl.FileResource;
import uk.gov.gchq.palisade.resource.impl.SystemResource;
import uk.gov.gchq.palisade.rule.Rules;
import uk.gov.gchq.palisade.service.ConnectionDetail;
import uk.gov.gchq.palisade.service.SimpleConnectionDetail;
import uk.gov.gchq.palisade.service.palisade.repository.PersistenceLayer;
import uk.gov.gchq.palisade.service.palisade.request.AuditRequest;
import uk.gov.gchq.palisade.service.palisade.request.RegisterDataRequest;
import uk.gov.gchq.palisade.service.palisade.web.AuditClient;
import uk.gov.gchq.palisade.service.request.DataRequestResponse;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(JUnit4.class)
public class ResultAggregationServiceTest {

    private AuditClient auditClient = Mockito.mock(AuditClient.class);
    private AuditService auditService;
    private PersistenceLayer persistenceLayer = Mockito.mock(PersistenceLayer.class);
    private ResultAggregationService service;
    private DataRequestResponse response = new DataRequestResponse();

    private RegisterDataRequest request;
    private User user;
    private Map<LeafResource, ConnectionDetail> resources = new HashMap<>();
    private Map<LeafResource, Rules> rules = new HashMap<>();
    private RequestId requestId = new RequestId().id(UUID.randomUUID().toString());
    private RequestId originalRequestId = new RequestId().id("OriginalId");
    private ExecutorService executor;

    @Before
    public void setup() {
        executor = Executors.newSingleThreadExecutor();
        auditService = new AuditService(auditClient, executor);
        service = new ResultAggregationService(auditService, persistenceLayer);
        request = new RegisterDataRequest().userId(new UserId().id("Bob")).context(new Context().purpose("Testing")).resourceId("/path/to/new/bob_file.txt");
        request.originalRequestId(originalRequestId);
        user = new User().userId("Bob").roles("Role1", "Role2").auths("Auth1", "Auth2");

        FileResource resource = new FileResource();
        resource.id("/path/to/new/bob_file.txt").type("bob").serialisedFormat("txt");
        resource.parent(new DirectoryResource().id("/path/to/new/")
                .parent(new DirectoryResource().id("/path/to/")
                        .parent(new DirectoryResource().id("/path/")
                                .parent(new SystemResource().id("/")))));
        ConnectionDetail connectionDetail = new SimpleConnectionDetail().uri("http://localhost:8082");
        resources.put(resource, connectionDetail);

        Rules rule = new Rules().rule("Rule1", new PassThroughRule());
        rules.put(resource, rule);

        response.originalRequestId(originalRequestId);
        response.resources(resources);
    }

    @Test
    public void aggregateDataRequestResultsTest() throws Exception {

        //Given
        when(auditService.audit(any(AuditRequest.class))).thenReturn(true);

        //When
        DataRequestResponse actual = service.aggregateDataRequestResults(request, user, resources, rules, requestId, originalRequestId).toCompletableFuture().get();

        //Then
        assertEquals(response.getResources(), actual.getResources());
    }

    @Test(expected = RuntimeException.class)
    public void aggregateDataRequestResultsWithErrorTest() throws Exception {

        //Given
        when(auditService.audit(any(AuditRequest.class))).thenReturn(true);

        //When
        DataRequestResponse actual = service.aggregateDataRequestResults(request, null, resources, rules, requestId, originalRequestId).toCompletableFuture().get();
    }
}
