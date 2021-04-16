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

package uk.gov.gchq.palisade.service.resource.service;

import akka.NotUsed;
import akka.japi.pf.PFBuilder;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Source;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.gov.gchq.palisade.service.resource.common.resource.LeafResource;
import uk.gov.gchq.palisade.service.resource.common.resource.ResourcePrepopulationFactory;
import uk.gov.gchq.palisade.service.resource.common.resource.ResourceService;
import uk.gov.gchq.palisade.service.resource.exception.NoSuchResourceException;
import uk.gov.gchq.palisade.service.resource.model.AuditErrorMessage;
import uk.gov.gchq.palisade.service.resource.model.AuditableResourceResponse;
import uk.gov.gchq.palisade.service.resource.model.ExceptionSource;
import uk.gov.gchq.palisade.service.resource.model.ResourceRequest;
import uk.gov.gchq.palisade.service.resource.model.ResourceResponse;
import uk.gov.gchq.palisade.service.resource.repository.PersistenceLayer;
import uk.gov.gchq.palisade.service.resource.stream.util.ConditionalGraph;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

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
     * Construct a StreamingResourceServiceProxy, but without any {@link ResourcePrepopulationFactory} prepopulation
     *
     * @param persistence a {@link PersistenceLayer} for persisting resources in, as if it were a cache
     * @param delegate    a 'real' {@link ResourceService} to delegate requests to when not found in the persistence layer
     */
    public ResourceServicePersistenceProxy(final PersistenceLayer persistence, final ResourceService delegate) {
        this.persistence = persistence;
        this.delegate = delegate;
    }

    /**
     * Uses a resource to get any {@link LeafResource}s associated with it.
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
        return Source.completionStageSource(persistence.getResourcesById(request.resourceId)
                .thenApply(persistenceHit -> persistenceHit
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
                                                        .withError(new NoSuchResourceException("Exception thrown while querying persistence", ex)))).build())
                        )
                        // If persistence is empty, a "cache miss"
                        .orElseGet(() -> Source.fromIterator(() -> this.delegateGetResourcesById(request))
                                .via(ConditionalGraph.map((AuditableResourceResponse response) -> {
                                    if (response.getAuditErrorMessage() != null) {
                                        return 0;
                                    } else {
                                        return 1;
                                    }
                                }, Map.of(
                                        0, Flow.create(),
                                        1, getResourceResponseFlow(request)
                                )))
                        )
                )
        ).mapMaterializedValue(ignored -> NotUsed.notUsed());

        /**/
    }

    /**
     * Delegate call out to the 'real' resource-service as there was a cache miss
     *
     * @param request the the {@link ResourceRequest} that contains the resourceId used to retrieve resources
     * @return an {@link Iterator} of auditable responses, containing {@link LeafResource}s associated with the resourceId
     */
    private Iterator<AuditableResourceResponse> delegateGetResourcesById(final ResourceRequest request) {
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
    }

    /**
     * Uses a type to get any {@link LeafResource}s associated with the it.
     *
     * @param request the the {@link ResourceRequest} that contains the resourceId used to retrieve resources
     * @param type    the type to be queried
     * @return a {@link Source} of {@link AuditableResourceResponse}s associated with the type
     */
    public Source<AuditableResourceResponse, NotUsed> getResourcesByType(final ResourceRequest request, final String type) {
        // Try first from persistence
        LOGGER.info(STORE);
        return Source.completionStageSource(persistence.getResourcesByType(type)
                .thenApply(persistenceHit -> persistenceHit
                        .orElseGet(() -> Source.fromIterator(() -> delegate.getResourcesByType(type))
                                .via(persistence.withPersistenceByType(type)))
                        // Wrap with a success
                        .map(leafResource -> AuditableResourceResponse.Builder.create()
                                .withResourceResponse(ResourceResponse.Builder.create(request)
                                        .withResource(leafResource)))
                )
        ).mapMaterializedValue(ignored -> NotUsed.notUsed());
    }

    /**
     * Uses a serialisedFormat to get any {@link LeafResource}s associated with the it.
     *
     * @param request          the {@link ResourceRequest} that contains the resourceId used to retrieve resources
     * @param serialisedFormat the serialisedFormat to be queried
     * @return a {@link Source} of {@link AuditableResourceResponse}s associated with the serialisedFormat
     */
    public Source<AuditableResourceResponse, NotUsed> getResourcesBySerialisedFormat(final ResourceRequest request, final String serialisedFormat) {
        // Try first from persistence
        LOGGER.debug(STORE);
        return Source.completionStageSource(persistence.getResourcesBySerialisedFormat(serialisedFormat)
                .thenApply(persistenceHit -> persistenceHit
                        .orElseGet(() -> Source.fromIterator(() -> delegate.getResourcesBySerialisedFormat(serialisedFormat))
                                .via(persistence.withPersistenceBySerialisedFormat(serialisedFormat)))
                        // Wrap with a success
                        .map(leafResource -> AuditableResourceResponse.Builder.create()
                                .withResourceResponse(ResourceResponse.Builder.create(request)
                                        .withResource(leafResource)))
                )
        ).mapMaterializedValue(ignored -> NotUsed.notUsed());

    }

    private Flow<AuditableResourceResponse, AuditableResourceResponse, NotUsed> getResourceResponseFlow(final ResourceRequest request) {
        return Flow
                .<AuditableResourceResponse>create()
                // Add the returned result to the persistence
                // If it wasn't an error, get the leaf resource
                .map(AuditableResourceResponse::getResourceResponse)
                .map(ResourceResponse::getResource)
                // Persist the leaf resource
                .via(persistence.withPersistenceById(request.resourceId))
                .map(leafResource -> AuditableResourceResponse.Builder.create()
                        .withResourceResponse(ResourceResponse.Builder.create(request)
                                .withResource(leafResource)));
    }
}
