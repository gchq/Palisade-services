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

package uk.gov.gchq.palisade.service.data.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.gov.gchq.palisade.reader.common.DataReader;
import uk.gov.gchq.palisade.reader.request.DataReaderRequest;
import uk.gov.gchq.palisade.service.data.domain.AuthorisedRequestEntity;
import uk.gov.gchq.palisade.service.data.exception.ForbiddenException;
import uk.gov.gchq.palisade.service.data.exception.ReadException;
import uk.gov.gchq.palisade.service.data.model.AuthorisedDataRequest;
import uk.gov.gchq.palisade.service.data.model.DataRequest;
import uk.gov.gchq.palisade.service.data.repository.PersistenceLayer;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Simple implementation of a data-service, which reads using a data-reader and audits the
 * number of records processed and returned.
 */
public class SimpleDataService implements DataService {
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleDataService.class);
    private final PersistenceLayer persistenceLayer;
    private final DataReader dataReader;

    /**
     * Autowired constructor for Spring.
     *
     * @param persistenceLayer the persistence layer containing the authorised read requests
     * @param dataReader       an instance of a data-reader (eg a {@code new HadoopDataReader()})
     */
    public SimpleDataService(
            final PersistenceLayer persistenceLayer,
            final DataReader dataReader
    ) {
        this.persistenceLayer = persistenceLayer;
        this.dataReader = dataReader;
    }

    /**
     * Query for the references.  It will return the information needed to retrieve the resources.  If there is no
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

    /**
     * Includes the resources into the OutputStream that is to be provided to the client
     *
     * @param authorisedDataRequest the information for the resources in the context of the request
     * @param out                   an {@link OutputStream} to write the stream of resources to (after applying rules)
     * @param recordsProcessed      number of records that have been processed
     * @param recordsReturned       number of records that have been returned
     * @return true if indicating that the process has been successful
     * @throws ReadException if there is a failure during reading of the stream
     */
    public CompletableFuture<Boolean> read(final AuthorisedDataRequest authorisedDataRequest, final OutputStream out,
                                           final AtomicLong recordsProcessed, final AtomicLong recordsReturned) {

        return CompletableFuture.supplyAsync(() -> {
            LOGGER.debug("Reading from reader with request {}", authorisedDataRequest);
            var readerRequest = new DataReaderRequest()
                    .context(authorisedDataRequest.getContext())
                    .user(authorisedDataRequest.getUser())
                    .resource(authorisedDataRequest.getResource())
                    .rules(authorisedDataRequest.getRules());
            var readerResponse = dataReader.read(readerRequest, recordsProcessed, recordsReturned);

            LOGGER.debug("Writing reader response {} to output stream", readerResponse);
            try {
                readerResponse.getWriter().write(out);
                out.close();
            } catch (IOException ex) {
                throw new ReadException("Failed to write data out to the output stream.", ex);
            }

            LOGGER.debug("Output stream closed, {} processed and {} returned, auditing success with audit service", recordsProcessed.get(), recordsReturned.get());
            return true;
        });
    }
}
