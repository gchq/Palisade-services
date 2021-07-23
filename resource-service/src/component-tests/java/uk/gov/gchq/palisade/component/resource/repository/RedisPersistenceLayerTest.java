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

package uk.gov.gchq.palisade.component.resource.repository;

import akka.stream.Materializer;
import akka.stream.javadsl.Keep;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.data.redis.DataRedisTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import uk.gov.gchq.palisade.contract.resource.redis.RedisInitialiser;
import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.resource.impl.FileResource;
import uk.gov.gchq.palisade.resource.impl.SimpleConnectionDetail;
import uk.gov.gchq.palisade.service.resource.config.ApplicationConfiguration;
import uk.gov.gchq.palisade.service.resource.config.RedisConfiguration;
import uk.gov.gchq.palisade.service.resource.repository.AbstractReactiveRepositoryRedisAdapter;
import uk.gov.gchq.palisade.service.resource.repository.AbstractReactiveRepositoryRedisAdapter.CompletenessRepositoryAdapter;
import uk.gov.gchq.palisade.service.resource.repository.AbstractReactiveRepositoryRedisAdapter.ResourceRepositoryAdapter;
import uk.gov.gchq.palisade.service.resource.repository.AbstractReactiveRepositoryRedisAdapter.SerialisedFormatRepositoryAdapter;
import uk.gov.gchq.palisade.service.resource.repository.AbstractReactiveRepositoryRedisAdapter.TypeRepositoryAdapter;
import uk.gov.gchq.palisade.service.resource.repository.ReactivePersistenceLayer;
import uk.gov.gchq.palisade.service.resource.stream.config.AkkaSystemConfig;
import uk.gov.gchq.palisade.util.AbstractResourceBuilder;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@DataRedisTest(properties = {
        "spring.data.redis.repositories.key-prefix=test:"
})
@ContextConfiguration(initializers = {RedisInitialiser.class},
        classes = {ApplicationConfiguration.class, RedisConfiguration.class, AkkaSystemConfig.class})
@EnableAutoConfiguration
@ActiveProfiles({"redis", "testcontainers"})
@DirtiesContext(classMode = ClassMode.BEFORE_CLASS)
class RedisPersistenceLayerTest {
    private static final Duration TTL_EXPIRY_DURATION = Duration.ofSeconds(2);

    @Autowired
    private ReactivePersistenceLayer persistenceLayer;
    @Autowired
    private Materializer materialiser;

    @Autowired
    private CompletenessRepositoryAdapter completenessRepository;
    @Autowired
    private TypeRepositoryAdapter typeRepository;
    @Autowired
    private SerialisedFormatRepositoryAdapter serialisedFormatRepository;
    @Autowired
    private ResourceRepositoryAdapter resourceRepository;

    private LeafResource resource;

    @BeforeEach
    public void setUp() {
        resource = ((FileResource) AbstractResourceBuilder.create("file:/root/test-file-id"))
                .type("test-type")
                .serialisedFormat("test-format")
                .connectionDetail(new SimpleConnectionDetail().serviceName("data-service"));
    }

    void saveResourceOverridingResourceTtl(final LeafResource leafResource, final Duration resourceTtl) {
        try {
            var ttlField = AbstractReactiveRepositoryRedisAdapter.class.getDeclaredField("ttl");
            ttlField.setAccessible(true);
            ttlField.set(completenessRepository, TTL_EXPIRY_DURATION);
            ttlField.set(typeRepository, TTL_EXPIRY_DURATION);
            ttlField.set(serialisedFormatRepository, TTL_EXPIRY_DURATION);
            ttlField.set(resourceRepository, resourceTtl);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            e.printStackTrace();
        }

        // addResource is only appropriate for runtime updates to an existing set, whereas put is appropriate for initialisation
        Source.single(leafResource)
                .via(persistenceLayer.withPersistenceById(leafResource.getId()))
                .via(persistenceLayer.withPersistenceByType(leafResource.getType()))
                .via(persistenceLayer.withPersistenceBySerialisedFormat(leafResource.getSerialisedFormat()))
                .runWith(Sink.ignore(), materialiser)
                .toCompletableFuture().join();
    }

    @Test
    void testSpringDiscoversPersistenceLayer() {
        // When the spring application is started
        assertThat(persistenceLayer)
                .as("Check the persistenceLayer has been autowired successfully")
                .isNotNull();
    }

    @Test
    void testPersistenceTtlForResourceId() throws InterruptedException {
        // Given the resource has been added to the persistence
        saveResourceOverridingResourceTtl(resource, TTL_EXPIRY_DURATION);

        // When getting a resource from the persistence layer by resourceId
        var idResult = persistenceLayer.getResourcesById(resource.getId())
                .join().orElseThrow()
                .runWith(Sink.seq(), materialiser)
                .toCompletableFuture()
                .join();
        // Then the returned resource should match the created resource
        assertThat(idResult)
                .as("Check that when getting a resource by its Id, the correct resource is returned")
                .containsOnly(resource)
                .first()
                .usingRecursiveComparison()
                .isEqualTo(resource);

        // Then sleep to imitate a persistence timeout
        TimeUnit.SECONDS.sleep(TTL_EXPIRY_DURATION.toSeconds());

        // When getting a resource from the persistence layer by resourceId
        var resourceOptional = persistenceLayer.getResourcesById(resource.getId())
                .join();

        // Then no resources are returned
        assertThat(resourceOptional)
                .as("Check that the list is empty as the resource has expired from the persistence")
                .isEmpty();
    }

    @Test
    void testPersistenceTtlForResourceType() throws InterruptedException {
        // Given the resource has been added to the persistence
        saveResourceOverridingResourceTtl(resource, TTL_EXPIRY_DURATION);

        // When getting a resource from the persistence layer by resource type
        var idResult = persistenceLayer.getResourcesByType(resource.getType())
                .join().orElseThrow()
                .runWith(Sink.seq(), materialiser)
                .toCompletableFuture()
                .join();
        // Then the returned resource should match the created resource
        assertThat(idResult)
                .as("Check that when getting a resource by its type, the correct resource is returned")
                .containsOnly(resource)
                .first()
                .usingRecursiveComparison()
                .isEqualTo(resource);

        // Then sleep to imitate a persistence timeout
        TimeUnit.SECONDS.sleep(TTL_EXPIRY_DURATION.toSeconds());

        // When getting a resource from the persistence layer by resourceId
        var resourceOptional = persistenceLayer.getResourcesByType(resource.getType())
                .join();

        // Then no resources are returned
        assertThat(resourceOptional)
                .as("Check that the list is empty as the resource has expired from the persistence")
                .isEmpty();
    }

    @Test
    void testPersistenceTtlForResourceFormat() throws InterruptedException {
        // Given the resource has been added to the persistence
        saveResourceOverridingResourceTtl(resource, TTL_EXPIRY_DURATION);

        // When getting a resource from the persistence layer by serialisedFormat
        var idResult = persistenceLayer.getResourcesBySerialisedFormat(resource.getSerialisedFormat())
                .join().orElseThrow()
                .toMat(Sink.seq(), Keep.right()).run(materialiser)
                .toCompletableFuture()
                .join();

        // Then the returned resource should match the created resource
        assertThat(idResult)
                .as("Check that when getting a resource by its serialised format, the correct resource is returned")
                .containsOnly(resource)
                .first()
                .usingRecursiveComparison()
                .isEqualTo(resource);

        // Then sleep to imitate a persistence timeout
        TimeUnit.SECONDS.sleep(TTL_EXPIRY_DURATION.toSeconds());

        // When getting a resource from the persistence layer by resourceId
        var resourceOptional = persistenceLayer.getResourcesBySerialisedFormat(resource.getSerialisedFormat())
                .join();

        // Then no resources are returned
        assertThat(resourceOptional)
                .as("Check that the list is empty as the resource has expired from the persistence")
                .isEmpty();
    }

    @Test
    void testPersistenceTtlForInFlightResources() throws InterruptedException {
        // Given the resource has been added to the persistence
        // Note the longer resource TTL
        saveResourceOverridingResourceTtl(resource, TTL_EXPIRY_DURATION.multipliedBy(2));

        // When getting a resource from the persistence layer by resourceId, imitating a slow request
        var inFlight = persistenceLayer.getResourcesById(resource.getId())
                .join()
                .orElseThrow();

        // Then Sleep for 2 seconds to imitate a slow request
        // The persistence evict for completeness happens here, but not for resources
        TimeUnit.SECONDS.sleep(TTL_EXPIRY_DURATION.toSeconds());

        // Now complete the retrieval of the request
        var idResult = inFlight.runWith(Sink.seq(), materialiser)
                .toCompletableFuture()
                .join();

        assertThat(idResult)
                .as("Check that when getting a resource by its Id, the correct resource is returned")
                .containsOnly(resource);
    }


    @Test
    void testPersistenceTtlForExpiredInFlightResources() throws InterruptedException {
        // Given the resource has been added to the persistence
        saveResourceOverridingResourceTtl(resource, TTL_EXPIRY_DURATION);

        // When getting a resource from the persistence layer by resourceId, imitating a slow request
        var inFlight = persistenceLayer.getResourcesById(resource.getId())
                .join().orElseThrow();

        // Then Sleep for 3 seconds to imitate a slow request
        // The persistence evict for completeness happens here and also for resources
        TimeUnit.SECONDS.sleep(TTL_EXPIRY_DURATION.toSeconds());

        // Now complete the retrieval of the request
        var idResult = inFlight.runWith(Sink.seq(), materialiser)
                .toCompletableFuture()
                .join();

        assertThat(idResult)
                .as("Check that the list is empty as the resource has expired from the persistence")
                .isEmpty();
    }
}
