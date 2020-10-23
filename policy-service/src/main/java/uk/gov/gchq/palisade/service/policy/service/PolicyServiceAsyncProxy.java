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

package uk.gov.gchq.palisade.service.policy.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.User;
import uk.gov.gchq.palisade.resource.Resource;
import uk.gov.gchq.palisade.service.request.Policy;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * An asynchronous service for processing a cacheable method.
 */
public class PolicyServiceAsyncProxy {

    private static final Logger LOGGER = LoggerFactory.getLogger(PolicyServiceAsyncProxy.class);
    private final PolicyServiceHierarchyProxy service;
    private final Executor executor;

    /**
     * Constructor for instantiating the {@link PolicyServiceAsyncProxy}
     *
     * @param service  the PolicyServiceCachingProxy
     * @param executor an executor for any {@link CompletableFuture}s
     */
    public PolicyServiceAsyncProxy(final PolicyServiceHierarchyProxy service, final Executor executor) {
        LOGGER.debug("Initialised the PolicyServiceAsyncProxy");
        this.service = service;
        this.executor = executor;
    }

    /**
     * Async call to the caching canAccess method
     *
     * @param <R>      the type of resource (may be a supertype)
     * @param user     the user requesting access to the resource
     * @param context  the context for the resource they want to access
     * @param resource the resource that they want to access
     * @return the completable future of the cacheable canAccess response
     */
    public <R extends Resource> CompletableFuture<Optional<R>> canAccess(final User user, final Context context, final R resource) {
        LOGGER.debug("Running canAccess from policy cache with values: user {}, context {}, and resource {}", user, context, resource);
        return CompletableFuture.supplyAsync(() -> service.canAccess(user, context, resource), executor);
    }

    /**
     * Async call to the caching getPolicy method
     *
     * @param resource the {@link Resource} they want to retrieve the recordRules for
     * @return the completableFuture containing an Optional of the recordRules from the cacheable getPolicy method
     */
    public CompletableFuture<Optional<Policy>> getPolicy(final Resource resource) {
        LOGGER.debug("Running getPolicy from policy cache with resource {}", resource);
        return CompletableFuture.supplyAsync(() -> service.getPolicy(resource), executor);
    }
}
