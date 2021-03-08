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

package uk.gov.gchq.palisade.component.data.repository;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.User;
import uk.gov.gchq.palisade.resource.impl.FileResource;
import uk.gov.gchq.palisade.rule.Rules;
import uk.gov.gchq.palisade.service.data.config.ApplicationConfiguration;
import uk.gov.gchq.palisade.service.data.domain.AuthorisedRequestEntity;
import uk.gov.gchq.palisade.service.data.repository.AuthorisedRequestsRepository;
import uk.gov.gchq.palisade.service.data.repository.JpaPersistenceLayer;
import uk.gov.gchq.palisade.service.data.stream.config.AkkaSystemConfig;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ContextConfiguration(classes = {ApplicationConfiguration.class, TestAsyncConfiguration.class, AkkaSystemConfig.class})
@EnableAutoConfiguration
@EntityScan(basePackageClasses = {AuthorisedRequestEntity.class})
@EnableJpaRepositories(basePackages = {"uk.gov.gchq.palisade.service.data.repository"})
@ActiveProfiles({"h2test"})
class JpaPersistenceLayerTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(JpaPersistenceLayerTest.class);
    @Autowired
    private JpaPersistenceLayer persistenceLayer;
    @Autowired
    private AuthorisedRequestsRepository requestsRepository;

    private static final String TOKEN = "test-request-token";
    private static final String RESOURCE_ID = "/resource/id";

    private final AuthorisedRequestEntity entity1 = new AuthorisedRequestEntity(
            TOKEN + "1",
            new User().userId("user-id"),
            new FileResource().id(RESOURCE_ID + "1"),
            new Context(),
            new Rules<>()
    );
    private final AuthorisedRequestEntity entity2 = new AuthorisedRequestEntity(
            TOKEN + "2",
            new User().userId("user-id"),
            new FileResource().id(RESOURCE_ID + "1"),
            new Context(),
            new Rules<>()
    );
    private final AuthorisedRequestEntity entity3 = new AuthorisedRequestEntity(
            TOKEN + "1",
            new User().userId("user-id"),
            new FileResource().id(RESOURCE_ID + "3"),
            new Context(),
            new Rules<>()
    );

    @Test
    void testSpringDiscoversJpaPersistenceLayer() {
        // When the spring application is started
        // Then
        assertThat(persistenceLayer).isNotNull();
        assertThat(requestsRepository).isNotNull();
    }

    @Test
    @Transactional(TxType.NEVER)
    void testEmptyGetReturnsEmpty() {
        // When
        LOGGER.info("get");
        Optional<AuthorisedRequestEntity> missingEntity = persistenceLayer.getAsync("not-a-token", "not-a-leafresource").join();

        // Then
        LOGGER.info("assert");
        assertThat(missingEntity)
                .isEmpty();
    }

    /**
     * @implNote
     * Because of the CompletableFuture, we will be scheduling a job in Spring-managed test within the scope of a Spring transaction.
     * Therefore the transaction will never be committed, and the external scheduler and worker threads won't see the new job record in the database.
     * To fix this, for this specific case (synchronous save, then asynchronous find), we must disable the test transaction.
     * Similar tests (asynchronous save, then synchronous find) are unaffected by this quirk (eg. the dual to this data store in the attribute-masking-service).
     */
    @Transactional(TxType.NEVER)
    @Test
    void testGetReturnsAuthorisedRequest() {
        // Given
        List<AuthorisedRequestEntity> entities = List.of(entity1, entity2, entity3);
        requestsRepository.saveAll(entities);

        // When
        Map<AuthorisedRequestEntity, Optional<AuthorisedRequestEntity>> persistedEntities = entities.stream()
                .map(ent -> new SimpleImmutableEntry<>(ent, persistenceLayer.getAsync(ent.getToken(), ent.getResourceId()).join()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        // Then
        persistedEntities.forEach((entity, persisted) ->
                assertThat(persisted)
                        .isPresent()
                        .get()
                        .isEqualTo(entity));
    }
}
