/*
 * Copyright 2020 Crown Copyright
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

package uk.gov.gchq.palisade.service.data.repository;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import uk.gov.gchq.palisade.service.data.domain.AuthorisedRequestEntity;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;

import static org.assertj.core.api.Assertions.assertThat;

class JpaPersistenceLayerTest {

    @Test
    void testPersistenceLayerGetsFromRepository() {
        // Given the repository is mocked
        AuthorisedRequestsRepository requestsRepository = Mockito.spy(AuthorisedRequestsRepository.class);
        Mockito.when(requestsRepository.findById(Mockito.anyString())).thenReturn(Optional.of(new AuthorisedRequestEntity()));

        // Given the JpaPersistenceLayer is using this mocked repository
        JpaPersistenceLayer persistenceLayer = new JpaPersistenceLayer(requestsRepository, new ForkJoinPool(1));

        // When an entity is requested from the persistence layer
        CompletableFuture<Optional<AuthorisedRequestEntity>> entity = persistenceLayer.getAsync("test-request-token", "/resource/id");

        // Then the configured entity is returned
        assertThat(entity.join())
                .isNotNull()
                .isPresent()
                .get()
                .isEqualTo(new AuthorisedRequestEntity());

        // Then the mock was called
        Mockito.verify(requestsRepository, Mockito.atLeastOnce()).findById(Mockito.anyString());
    }

}
