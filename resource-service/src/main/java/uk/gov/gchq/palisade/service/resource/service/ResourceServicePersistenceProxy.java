/*
 * Copyright 2021 Crown Copyright
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

package uk.gov.gchq.palisade.service.resource.service;

import akka.NotUsed;
import akka.japi.pf.PFBuilder;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.service.ResourceService;
import uk.gov.gchq.palisade.service.resource.exception.NoSuchResourceException;
import uk.gov.gchq.palisade.service.resource.model.AuditErrorMessage;
import uk.gov.gchq.palisade.service.resource.model.AuditableResourceResponse;
import uk.gov.gchq.palisade.service.resource.model.ExceptionSource;
import uk.gov.gchq.palisade.service.resource.model.ResourceRequest;
import uk.gov.gchq.palisade.service.resource.model.ResourceResponse;
import uk.gov.gchq.palisade.service.resource.repository.PersistenceLayer;

import java.util.Collections;
import java.util.Optional;

/**
 * A proxy of (wrapper around) an instance of a {@link ResourceService}.
 * This adds a cache-like behaviour to the service by persisting requests/responses in a database.
 * Additionally, this is expected to be used by an asynchronous REST streaming response, so has further considerations
 * to properly support the callback.
 */
public class ResourceServicePersistenceProxy {
    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceServicePersistenceProxy.class);
    private static final String STORE = "Trying from persistence store";

    private final PersistenceLayer persistence;
    private final ResourceService delegate;

    /**
     * Construct a StreamingResourceServiceProxy, but without any {@link uk.gov.gchq.palisade.service.ResourcePrepopulationFactory} prepopulation
     *
     * @param persistence a {@link PersistenceLayer} for persisting resources in, as if it were a cache
     * @param delegate    a 'real' {@link ResourceService} to delegate requests to when not found in the persistence layer
     */
    public ResourceServicePersistenceProxy(final PersistenceLayer persistence, final ResourceService delegate) {
        this.persistence = persistence;
        this.delegate = delegate;
    }

    /**
     * Uses a resource to get any {@link LeafResource}s associated with the it.
     *
     * @param request the the {@link ResourceRequest} that contains the resourceId used to retrieve resources
     * @return a {@link Source} of {@link LeafResource}s associated with the resource
     */
    public Source<AuditableResourceResponse, NotUsed> getResourcesByResource(final ResourceRequest request) {
        return this.getResourcesById(request);
    }

    /**
     * Uses a resourceId to get any {@link LeafResource}s associated with the it.
     *
     * @param request the the {@link ResourceRequest} that contains the resourceId used to retrieve resources
     * @return a {@link Source} of {@link LeafResource}s associated with the resourceId
     */
    public Source<AuditableResourceResponse, NotUsed> getResourcesById(final ResourceRequest request) {
        LOGGER.info(STORE);
        // Try first from persistence
        return persistence.getResourcesById(request.resourceId)
                // If persistence returned a "cache hit"
                .map(persisted -> persisted
                        // Wrap with a success
                        .map(leafResource -> AuditableResourceResponse.Builder.create()
                                .withResourceResponse(ResourceResponse.Builder.create(request)
                                        .withResource(leafResource)))
                        // Persistence threw an error, create an AuditErrorMessage
                        .recover(new PFBuilder<Throwable, AuditableResourceResponse>()
                                .match(Exception.class, ex -> AuditableResourceResponse.Builder.create()
                                        .withAuditErrorMessage(AuditErrorMessage.Builder.create(request,
                                                Collections.singletonMap(ExceptionSource.ATTRIBUTE_KEY, ExceptionSource.PERSISTENCE.toString()))
                                                .withError(new NoSuchResourceException("Exception thrown while querying persistence", ex)))).build()))
                // If persistence is empty, a "cache miss"
                .orElseGet(() -> Source
                        .fromIterator(() -> {
                            try {
                                // Try to call out to implemented delegate service
                                return FunctionalIterator.fromIterator(delegate.getResourcesById(request.resourceId))
                                        // Wrap with a success
                                        .map(leafResource -> AuditableResourceResponse.Builder.create()
                                                .withResourceResponse(ResourceResponse.Builder.create(request)
                                                        .withResource(leafResource)))
                                        // An error occurred when requesting resource from delegate, create an AuditErrorMessage
                                        .exceptionally(ex -> AuditableResourceResponse.Builder.create()
                                                .withAuditErrorMessage(AuditErrorMessage.Builder.create(request,
                                                        Collections.singletonMap(ExceptionSource.ATTRIBUTE_KEY, ExceptionSource.REQUEST.toString()))
                                                        .withError(new NoSuchResourceException(ex.getMessage(), ex))));
                            } catch (RuntimeException ex) {
                                LOGGER.error("Exception encountered connecting to the service: {}", ex.getMessage());
                                // If the initial request to the service fails, audit as a service error rather than a request error
                                return Collections.singleton(AuditableResourceResponse.Builder.create()
                                        .withAuditErrorMessage(AuditErrorMessage.Builder.create(request,
                                                Collections.singletonMap(ExceptionSource.ATTRIBUTE_KEY, ExceptionSource.SERVICE.toString()))
                                                .withError(new NoSuchResourceException(ex.getMessage(), ex))))
                                        .iterator();
                            }
                        })
                        .alsoTo(Flow.<AuditableResourceResponse>create()
                                // Add the returned result to the persistence
                                // If it wasn't an error, get the leaf resource
                                .map(auditableResponse -> Optional.ofNullable(auditableResponse.getResourceResponse())
                                        .map(ResourceResponse::getResource))
                                .filter(Optional::isPresent)
                                .map(Optional::get)
                                // Persist the leaf resource
                                .via(persistence.withPersistenceById(request.resourceId))
                                // We don't care about the result as we will return the AuditableResourceResponse
                                .to(Sink.ignore()))
                );
    }

    /**
     * Uses a type to get any {@link LeafResource}s associated with the it.
     *
     * @param request the the {@link ResourceRequest} that contains the resourceId used to retrieve resources
     * @param type    the type to be queried
     * @return a {@link Source} of {@link LeafResource}s associated with the type
     */
    public Source<AuditableResourceResponse, NotUsed> getResourcesByType(final ResourceRequest request, final String type) {
        // Try first from persistence
        LOGGER.info(STORE);
        return persistence.getResourcesByType(type)
                .orElseGet(() -> Source.fromIterator(() -> delegate.getResourcesByType(type))
                        .via(persistence.withPersistenceByType(type)))
                // Wrap with a success
                .map(leafResource -> AuditableResourceResponse.Builder.create()
                        .withResourceResponse(ResourceResponse.Builder.create(request)
                                .withResource(leafResource)));
    }

    /**
     * Uses a serialisedFormat to get any {@link LeafResource}s associated with the it.
     *
     * @param request          the the {@link ResourceRequest} that contains the resourceId used to retrieve resources
     * @param serialisedFormat the serialisedFormat to be queried
     * @return a {@link FunctionalIterator} of {@link LeafResource}s associated with the type
     */
    public Source<AuditableResourceResponse, NotUsed> getResourcesBySerialisedFormat(final ResourceRequest request, final String serialisedFormat) {
        // Try first from persistence
        LOGGER.debug(STORE);
        return persistence.getResourcesBySerialisedFormat(serialisedFormat)
                .orElseGet(() -> Source.fromIterator(() -> delegate.getResourcesBySerialisedFormat(serialisedFormat))
                        .via(persistence.withPersistenceBySerialisedFormat(serialisedFormat)))
                // Wrap with a success
                .map(leafResource -> AuditableResourceResponse.Builder.create()
                        .withResourceResponse(ResourceResponse.Builder.create(request)
                                .withResource(leafResource)));

    }

    /**
     * Add a single resource to the service (if possible) and on success, also add it to persistence.
     *
     * @param leafResource the new leafResource created at runtime
     * @return whether the add operation succeeded or failed
     */
    public Boolean addResource(final LeafResource leafResource) {
        boolean success = delegate.addResource(leafResource);
        if (success) {
            persistence.addResource(leafResource);
        }
        return success;
    }
}
