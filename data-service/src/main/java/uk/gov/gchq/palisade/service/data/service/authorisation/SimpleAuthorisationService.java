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

package uk.gov.gchq.palisade.service.data.service.authorisation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.gov.gchq.palisade.service.data.domain.AuthorisedRequestEntity;
import uk.gov.gchq.palisade.service.data.exception.ForbiddenException;
import uk.gov.gchq.palisade.service.data.model.AuthorisedDataRequest;
import uk.gov.gchq.palisade.service.data.model.DataRequest;
import uk.gov.gchq.palisade.service.data.repository.PersistenceLayer;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Simple implementation of a Data Service, which reads using a data-reader and audits the
 * number of records processed and returned.
 */
public class SimpleAuthorisationService implements AuthorisationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleAuthorisationService.class);
    private final PersistenceLayer persistenceLayer;

    /**
     * Autowired constructor for Spring.
     *
     * @param persistenceLayer the persistence layer containing the authorised read requests
     */
    public SimpleAuthorisationService(final PersistenceLayer persistenceLayer) {
        this.persistenceLayer = persistenceLayer;
    }

    /**
     * Query for the references. It will return the information needed to retrieve the resources. If there is no
     * data to be returned, a {@link ForbiddenException} is thrown.
     *
     * @param dataRequest data provided by the client for requesting the resource
     * @return reference to the resources that are to be returned to client
     * @throws ForbiddenException if there is no authorised data for the request
     */
    public CompletableFuture<AuthorisedDataRequest> authoriseRequest(final DataRequest dataRequest) {
        LOGGER.debug("Querying persistence for token {} and resource {}", dataRequest.getToken(), dataRequest.getLeafResourceId());
        CompletableFuture<Optional<AuthorisedRequestEntity>> futureRequestEntity = persistenceLayer.getAsync(dataRequest.getToken(), dataRequest.getLeafResourceId());
        return futureRequestEntity.thenApply(maybeEntity -> maybeEntity.map(
                entity -> AuthorisedDataRequest.Builder.create()
                        .withResource(entity.getLeafResource())
                        .withUser(entity.getUser())
                        .withContext(entity.getContext())
                        .withRules(entity.getRules())
                ).orElseThrow(() -> new ForbiddenException(String.format("There is no data for the request, with token %s and resource %s", dataRequest.getToken(), dataRequest.getLeafResourceId())))
        );
    }

}
