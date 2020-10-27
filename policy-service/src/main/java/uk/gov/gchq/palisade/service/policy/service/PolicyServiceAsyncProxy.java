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

import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.rule.Rules;

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
     * Async call to the {@link PolicyServiceHierarchyProxy} getResourceRules takes a resource to get the resource rules applied against it
     *
     * @param resource the resource the user wants access to
     * @return the rules for the LeafResource, if null then an exception will be thrown
     */
    public CompletableFuture<Rules<LeafResource>> getResourceRules(final LeafResource resource) {
        return CompletableFuture.supplyAsync(() -> service.getResourceRules(resource), executor);
    }

    /**
     * Async call to the {@link PolicyServiceHierarchyProxy} getRecordRules that takes a resource and gets the record rules applied against it
     *
     * @param resource the resource the user wants access to
     * @return the record rules for the LeafResource, if null then an exception will be thrown
     */
    public CompletableFuture<Rules<?>> getRecordRules(final LeafResource resource) {
        return CompletableFuture.supplyAsync(() -> service.getRecordRules(resource), executor);
    }
}
