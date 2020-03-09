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

import uk.gov.gchq.palisade.RequestId;
import uk.gov.gchq.palisade.data.serialise.Serialiser;
import uk.gov.gchq.palisade.reader.common.CachedSerialisedDataReader.MapWrap;
import uk.gov.gchq.palisade.reader.common.DataFlavour;
import uk.gov.gchq.palisade.reader.common.DataReader;
import uk.gov.gchq.palisade.reader.request.DataReaderRequest;
import uk.gov.gchq.palisade.reader.request.DataReaderResponse;
import uk.gov.gchq.palisade.service.CacheService;
import uk.gov.gchq.palisade.service.Service;
import uk.gov.gchq.palisade.service.data.request.AddSerialiserRequest;
import uk.gov.gchq.palisade.service.data.request.AuditRequest;
import uk.gov.gchq.palisade.service.data.request.GetDataRequestConfig;
import uk.gov.gchq.palisade.service.data.request.NoInputReadResponse;
import uk.gov.gchq.palisade.service.data.request.ReadRequest;
import uk.gov.gchq.palisade.service.data.request.ReadResponse;
import uk.gov.gchq.palisade.service.request.AddCacheRequest;
import uk.gov.gchq.palisade.service.request.DataRequestConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

import static java.util.Objects.requireNonNull;

/**
 * <p>
 * A SimpleDataService is a simple implementation of {@link DataService} that
 * applies the required policy rules before returning data.
 * </p>
 * <p>
 * It should only be used for examples/demos.
 * </p>
 * <p>
 * It does not currently apply any validation of the {@link ReadRequest}, so users are able to
 * request any data they want, which could be different to the original data they requested from Palisade.
 * </p>
 */
public class SimpleDataService implements DataService {
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleDataService.class);
    public static final String SERIALISER_KEY = "cached.serialiser.map";

    private PalisadeService palisadeService;
    private DataReader dataReader;
    private CacheService cacheService;
    private AuditService auditService;

    public SimpleDataService(final CacheService cacheService, final AuditService auditService, final PalisadeService palisadeService, final DataReader dataReader) {
        this.cacheService = cacheService;
        this.auditService = auditService;
        this.palisadeService = palisadeService;
        this.dataReader = dataReader;
    }

    public SimpleDataService auditService(final AuditService auditService) {
        requireNonNull(auditService, "The audit service cannot be set to null");
        this.auditService = auditService;
        return this;
    }

    public SimpleDataService palisadeService(final PalisadeService palisadeService) {
        requireNonNull(palisadeService, "The palisade service cannot be set to null.");
        this.palisadeService = palisadeService;
        return this;
    }

    public SimpleDataService reader(final DataReader reader) {
        requireNonNull(reader, "The data reader cannot be set to null.");
        this.dataReader = reader;
        return this;
    }

    public SimpleDataService cacheService(final CacheService cacheService) {
        requireNonNull(cacheService, "The cache service cannot be set to null.");
        this.cacheService = cacheService;
        return this;
    }

    private void auditRequestReceivedException(final ReadRequest request, final Throwable ex) {
        LOGGER.error("Error while handling request: {}", request);
        LOGGER.error("{} was: {}", ex.getClass(), ex.getMessage());
        LOGGER.info("Auditing error with audit service");
        auditService.audit(AuditRequest.ReadRequestExceptionAuditRequest.create(request.getOriginalRequestId())
                .withToken(request.getToken())
                .withLeafResource(request.getResource())
                .withException(ex));
    }

    private void auditRequestComplete(final DataReaderRequest request, final AtomicLong recordsProcessed, final AtomicLong recordsReturned) {
        LOGGER.info("Auditing completed read with audit service");
        auditService.audit(AuditRequest.ReadRequestCompleteAuditRequest.create(request.getOriginalRequestId())
                .withUser(request.getUser())
                .withLeafResource(request.getResource())
                .withContext(request.getContext())
                .withRulesApplied(request.getRules())
                .withNumberOfRecordsReturned(recordsReturned.get())
                .withNumberOfRecordsProcessed(recordsProcessed.get()));
    }

    @Override
    public CompletableFuture<ReadResponse> read(final ReadRequest request) {
        requireNonNull(request, "The request cannot be null.");

        LOGGER.debug("Creating async read: {}", request);
        return CompletableFuture.supplyAsync(() -> {
            LOGGER.debug("Starting to read: {}", request);

            final GetDataRequestConfig getConfig = new GetDataRequestConfig()
                    .token(new RequestId().id(request.getToken()))
                    .resource(request.getResource());
            getConfig.setOriginalRequestId(request.getOriginalRequestId());
            LOGGER.debug("Calling palisade service with: {}", getConfig);

            final DataRequestConfig config = getPalisadeService().getDataRequestConfig(getConfig).join();
            LOGGER.info("Palisade service returned: {}", config);

            final DataReaderRequest readerRequest = new DataReaderRequest()
                    .resource(request.getResource())
                    .user(config.getUser())
                    .context(config.getContext())
                    .rules(config.getRules().get(request.getResource()));
            readerRequest.setOriginalRequestId(request.getOriginalRequestId());
            LOGGER.info("Calling dataReader with: {}", readerRequest);

            AtomicLong recordsProcessed = new AtomicLong(0);
            AtomicLong recordsReturned = new AtomicLong(0);
            final DataReaderResponse readerResult = getDataReader().read(readerRequest, recordsProcessed, recordsReturned);
            auditRequestComplete(readerRequest, recordsProcessed, recordsReturned);
            LOGGER.info("Processed {} and returned {} records, reader returned: {}", recordsProcessed.get(), recordsReturned.get(), readerResult);

            return (ReadResponse) new NoInputReadResponse(readerResult);
        }).exceptionally(ex -> {
            LOGGER.warn("Error handling: " + ex.getMessage());
            auditRequestReceivedException(request, ex);
            throw new RuntimeException(ex); //rethrow the exception
        });
    }

    @Override
    public CompletableFuture<Boolean> addSerialiser(final AddSerialiserRequest request) {
        LOGGER.info("Processing AddSerialiserRequest: {}", request);

        //Create map of DataFlavour and Serialiser
        Map<DataFlavour, Serialiser<?>> typeMap = new HashMap<>();
        typeMap.put(request.getDataFlavour(), request.getSerialiser());

        //Create AddCacheRequest
        AddCacheRequest<MapWrap> cacheRequest = new AddCacheRequest<>()
                .service(Service.class)
                .key(SERIALISER_KEY)
                .value(new MapWrap(typeMap));
        cacheService.add(cacheRequest).join();
        LOGGER.debug("Serialiser added: {}", cacheRequest);

        return CompletableFuture.completedFuture(true);
    }

    public PalisadeService getPalisadeService() {
        requireNonNull(palisadeService, "The palisade service has not been set.");
        return palisadeService;
    }

    public void setPalisadeService(final PalisadeService palisadeService) {
        palisadeService(palisadeService);
    }


    public DataReader getDataReader() {
        requireNonNull(dataReader, "The data dataReader has not been set.");
        LOGGER.info("DataReader acquired: {}", dataReader.toString());
        return dataReader;
    }

    public void setDataReader(final DataReader dataReader) {
        reader(dataReader);
    }

    public CacheService getCacheService() {
        requireNonNull(cacheService, "The cacheService has not been set.");
        return cacheService;
    }

    public void setCacheService(final CacheService cacheService) {
        cacheService(cacheService);
    }

    public AuditService getAuditService() {
        requireNonNull(auditService, "The audit service has not been set.");
        return auditService;
    }

    public void setAuditService(final AuditService auditService) {
        auditService(auditService);
    }
}
