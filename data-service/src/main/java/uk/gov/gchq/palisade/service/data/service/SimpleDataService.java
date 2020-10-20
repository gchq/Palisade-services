/*
 * Copyright 2018 Crown Copyright
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
import org.springframework.data.util.Pair;

import uk.gov.gchq.palisade.data.serialise.Serialiser;
import uk.gov.gchq.palisade.reader.common.DataFlavour;
import uk.gov.gchq.palisade.reader.common.DataReader;
import uk.gov.gchq.palisade.reader.request.DataReaderRequest;
import uk.gov.gchq.palisade.reader.request.DataReaderResponse;
import uk.gov.gchq.palisade.service.data.domain.AuthorisedRequestEntity;
import uk.gov.gchq.palisade.service.data.exception.ReadException;
import uk.gov.gchq.palisade.service.data.model.DataRequest;
import uk.gov.gchq.palisade.service.data.repository.PersistenceLayer;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Smple implementation of a data-service, which reads using a data-reader and audits the
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

    public CompletableFuture<Optional<DataReaderRequest>> authoriseRequest(final DataRequest dataRequest) {
        LOGGER.debug("Querying persistence for token {} and resource {}", dataRequest.getToken(), dataRequest.getLeafResourceId());
        CompletableFuture<Optional<AuthorisedRequestEntity>> futureRequestEntity = this.persistenceLayer.getAsync(dataRequest.getToken(), dataRequest.getLeafResourceId());
        return futureRequestEntity.thenApply(maybeEntity -> maybeEntity.map(
                entity -> new DataReaderRequest()
                        .context(entity.getContext())
                        .user(entity.getUser())
                        .resource(entity.getLeafResource())
                        .rules(entity.getRules())
                )
        );
    }

    public Pair<AtomicLong, AtomicLong> read(final DataReaderRequest readerRequest, final OutputStream out) {
        final AtomicLong recordsProcessed = new AtomicLong(0);
        final AtomicLong recordsReturned = new AtomicLong(0);

        LOGGER.debug("Reading from reader with request {}", readerRequest);
        DataReaderResponse readerResponse = this.dataReader.read(readerRequest, recordsProcessed, recordsReturned);

        LOGGER.debug("Writing reader response {} to output stream", readerResponse);
        try {
            readerResponse.getWriter().write(out);
            out.close();
        } catch (IOException ex) {
            throw new ReadException(readerRequest, ex);
        }

        LOGGER.debug("Output stream closed, {} processed and {} returned, auditing success with audit service", recordsProcessed.get(), recordsReturned.get());
        return Pair.of(recordsProcessed, recordsReturned);
    }

    @Override
    public Boolean addSerialiser(final DataFlavour flavour, final Serialiser<?> serialiser) {
        LOGGER.info("Adding serialiser {} for DataFlavour {}", serialiser, flavour);
        this.dataReader.addSerialiser(flavour, serialiser);
        return true;
    }

}
