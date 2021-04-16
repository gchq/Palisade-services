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

package uk.gov.gchq.palisade.component.resource.service;

import akka.NotUsed;
import akka.stream.Materializer;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import uk.gov.gchq.palisade.service.resource.common.Context;
import uk.gov.gchq.palisade.service.resource.common.resource.LeafResource;
import uk.gov.gchq.palisade.service.resource.common.resource.impl.SimpleConnectionDetail;
import uk.gov.gchq.palisade.service.resource.common.user.User;
import uk.gov.gchq.palisade.service.resource.common.util.ResourceBuilder;
import uk.gov.gchq.palisade.service.resource.config.ApplicationConfiguration;
import uk.gov.gchq.palisade.service.resource.config.DefaultConfiguration;
import uk.gov.gchq.palisade.service.resource.config.R2dbcConfiguration;
import uk.gov.gchq.palisade.service.resource.exception.NoSuchResourceException;
import uk.gov.gchq.palisade.service.resource.model.AuditErrorMessage;
import uk.gov.gchq.palisade.service.resource.model.AuditableResourceResponse;
import uk.gov.gchq.palisade.service.resource.model.ResourceRequest;
import uk.gov.gchq.palisade.service.resource.model.ResourceResponse;
import uk.gov.gchq.palisade.service.resource.repository.ReactivePersistenceLayer;
import uk.gov.gchq.palisade.service.resource.service.ResourceServicePersistenceProxy;
import uk.gov.gchq.palisade.service.resource.stream.config.AkkaSystemConfig;

import java.io.File;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests to verify the handling of exceptions,and the population of audit objects during stream processing
 */
@DataR2dbcTest
@ContextConfiguration(classes = {ApplicationConfiguration.class, DefaultConfiguration.class, R2dbcConfiguration.class, AkkaSystemConfig.class})
@EntityScan(basePackages = {"uk.gov.gchq.palisade.service.resource.domain"})
@EnableR2dbcRepositories(basePackages = {"uk.gov.gchq.palisade.service.resource.repository"})
@ActiveProfiles({"db-test"})
@DirtiesContext(classMode = ClassMode.BEFORE_CLASS)
class ResourceServicePersistenceProxyTest {

    private static final SimpleConnectionDetail DETAIL = new SimpleConnectionDetail().serviceName("data-service");
    private static final LeafResource FILE_1 = ((LeafResource) ResourceBuilder.create("file:/test/resourceId/data1.txt"))
            .type("data")
            .serialisedFormat("txt")
            .connectionDetail(DETAIL);
    private final Function<Integer, ResourceRequest> requestFactoryObj = i -> ResourceRequest.Builder.create()
            .withUserId("user-id")
            .withResourceId(String.format("file:/test/resourceId/data%d.txt", i))
            .withContext(new Context().purpose("test-purpose"))
            .withUser(new User().userId("test-user"));
    @Autowired
    private ResourceServicePersistenceProxy resourceServiceAsyncProxy;
    @Autowired
    private ReactivePersistenceLayer persistenceLayer;
    @Autowired
    private Materializer materializer;

    @BeforeEach
    void setup() throws InterruptedException {
        Source.single(FILE_1)
                .via(persistenceLayer.withPersistenceById(FILE_1.getId()))
                .via(persistenceLayer.withPersistenceByType(FILE_1.getType()))
                .via(persistenceLayer.withPersistenceBySerialisedFormat(FILE_1.getSerialisedFormat()))
                .runWith(Sink.seq(), materializer);

        TimeUnit.SECONDS.sleep(1);
    }

    @Test
    void testContextLoads() {
        assertThat(resourceServiceAsyncProxy)
                .as("Check the resourceProxy has been autowired successfully")
                .isNotNull();

        assertThat(persistenceLayer)
                .as("Check the persistenceLayer has been autowired successfully")
                .isNotNull();
    }

    @Test
    void testGetResourcesByIdSuccess() {
        // Given a user request
        final ResourceRequest resourceRequest = requestFactoryObj.apply(1);

        // Then retrieving a resource from the cache
        final Source<AuditableResourceResponse, NotUsed> subject = this.resourceServiceAsyncProxy.getResourcesById(resourceRequest);
        final CompletionStage<List<AuditableResourceResponse>> future = subject.runWith(Sink.seq(), materializer);
        final List<AuditableResourceResponse> result = future.toCompletableFuture().join();

        // Then check there is no error and check the returned resource ID
        assertThat(result.get(0))
                .as("Check that there is no AuditErrorMessage")
                .extracting(AuditableResourceResponse::getAuditErrorMessage)
                .isNull();

        assertThat(result.get(0))
                .as("Check the resourceResponse has the correct resource attached")
                .extracting(AuditableResourceResponse::getResourceResponse)
                .extracting(ResourceResponse::getResourceId)
                .isEqualTo(FILE_1.getId());
    }

    @Test
    void testGetResourceByIdFailure() {
        // Given a user request
        final ResourceRequest resourceRequest = requestFactoryObj.apply(2);

        // Then retrieving a different resource from the cache
        final List<AuditableResourceResponse> result = this.resourceServiceAsyncProxy.getResourcesById(resourceRequest)
                .runWith(Sink.seq(), materializer)
                .toCompletableFuture().join();

        // Then check there is an error
        assertThat(result.get(0))
                .as("Check that there is no ResourceResponse")
                .extracting(AuditableResourceResponse::getResourceResponse)
                .isNull();

        assertThat(result.get(0))
                .as("Check that the resourceServiceAsyncProxy throws the correct error when processing an invalid request")
                .extracting(AuditableResourceResponse::getAuditErrorMessage)
                .extracting(AuditErrorMessage::getError)
                .isExactlyInstanceOf(NoSuchResourceException.class)
                .extracting("Message")
                .isEqualTo("Failed to walk path " + File.separator + "test" + File.separator + "resourceId" + File.separator + "data2.txt");
    }
}
