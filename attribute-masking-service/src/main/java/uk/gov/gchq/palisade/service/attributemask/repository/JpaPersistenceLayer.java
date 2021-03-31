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
package uk.gov.gchq.palisade.service.attributemask.repository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.gov.gchq.palisade.service.attributemask.common.Context;
import uk.gov.gchq.palisade.service.attributemask.common.User;
import uk.gov.gchq.palisade.service.attributemask.common.resource.LeafResource;
import uk.gov.gchq.palisade.service.attributemask.common.rule.Rules;
import uk.gov.gchq.palisade.service.attributemask.model.AttributeMaskingRequest;

import javax.transaction.Transactional;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Java JPA implementation of a {@link PersistenceLayer} for the attribute-masking-service.
 * Delegates to a CrudRepository save method.
 */
public class JpaPersistenceLayer implements PersistenceLayer {
    private static final Logger LOGGER = LoggerFactory.getLogger(JpaPersistenceLayer.class);

    private final AuthorisedRequestsRepository authorisedRequestsRepository;
    private final Executor executor;

    /**
     * Constructor expected to be called by the ApplicationConfiguration, autowiring in the appropriate implementation of the repository (h2/redis/...)
     *
     * @param authorisedRequestsRepository the appropriate CrudRepository implementation
     * @param executor                     an async executor for running the put request
     */
    public JpaPersistenceLayer(final AuthorisedRequestsRepository authorisedRequestsRepository, final Executor executor) {
        this.authorisedRequestsRepository = Optional.ofNullable(authorisedRequestsRepository)
                .orElseThrow(() -> new IllegalArgumentException("authorisedRequestsRepository cannot be null"));
        this.executor = Optional.ofNullable(executor)
                .orElseThrow(() -> new IllegalArgumentException("executor cannot be null"));
    }

    @Override
    @Transactional
    public CompletableFuture<AttributeMaskingRequest> putAsync(final String token, final User user, final LeafResource resource, final Context context, final Rules<?> rules) {
        LOGGER.debug("Persisting authorised request for unique pair {}-{}", token, resource.getId());
        return CompletableFuture.supplyAsync(() -> {
            this.authorisedRequestsRepository.save(token, user, resource, context, rules);
            return AttributeMaskingRequest.Builder.create().withUserId(user.getUserId().getId())
                    .withResourceId(resource.getId())
                    .withContext(context)
                    .withUser(user)
                    .withResource(resource)
                    .withRules(rules);
        }, this.executor);
    }
}
