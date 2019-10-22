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

import uk.gov.gchq.palisade.reader.common.DataReader;
import uk.gov.gchq.palisade.reader.request.DataReaderRequest;
import uk.gov.gchq.palisade.reader.request.DataReaderResponse;
import uk.gov.gchq.palisade.service.data.request.AuditRequest.ReadRequestExceptionAuditRequest;
import uk.gov.gchq.palisade.service.data.request.AuditRequestReceiver;
import uk.gov.gchq.palisade.service.data.request.GetDataRequestConfig;
import uk.gov.gchq.palisade.service.data.request.NoInputReadResponse;
import uk.gov.gchq.palisade.service.data.request.ReadRequest;
import uk.gov.gchq.palisade.service.data.request.ReadResponse;
import uk.gov.gchq.palisade.service.request.DataRequestConfig;

import java.util.concurrent.CompletableFuture;

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

    private PalisadeService palisadeService;
    private DataReader dataReader;
    private CacheService cacheService;
    private AuditService auditService;
    private AuditRequestReceiver auditRequestReceiver;

    public SimpleDataService(CacheService cacheService, AuditService auditService, PalisadeService palisadeService, DataReader dataReader, AuditRequestReceiver auditRequestReceiver) {
        this.cacheService = cacheService;
        this.auditService = auditService;
        this.palisadeService = palisadeService;
        this.dataReader = dataReader;
        this.auditRequestReceiver = auditRequestReceiver;
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
        requireNonNull(reader, "The data dataReader cannot be set to null.");
        this.dataReader = reader;
        return this;
    }

    public SimpleDataService cacheService(final CacheService cacheService) {
        requireNonNull(cacheService, "The cacheService service cannot be set to null.");
        this.cacheService = cacheService;
        //changing cacheService service...
        return this;
    }

    private void auditRequestReceivedException(final ReadRequest request, final Throwable ex) {
        LOGGER.debug("Error handling: " + ex.getMessage());
        auditService.audit(ReadRequestExceptionAuditRequest.create(request.getOriginalRequestId())
                .withToken(request.getToken())
                .withLeafResource(request.getResource())
                .withException(ex));
    }

    @Override
    public CompletableFuture<ReadResponse> read(final ReadRequest request) {
        requireNonNull(request, "The request cannot be null.");
        //check that we have an active heartbeat before serving request

        LOGGER.debug("Creating async read: {}", request);
        return CompletableFuture.supplyAsync(() -> {
            LOGGER.debug("Starting to read: {}", request);
            final GetDataRequestConfig getConfig = new GetDataRequestConfig()
                    .token(request.getToken())
                    .resource(request.getResource());
            getConfig.setOriginalRequestId(request.getOriginalRequestId());
            LOGGER.debug("Calling palisade service with: {}", getConfig);
            final DataRequestConfig config = getPalisadeService().getDataRequestConfig(getConfig).join();
            LOGGER.debug("Palisade service returned: {}", config);

            final DataReaderRequest readerRequest = new DataReaderRequest()
                    .resource(request.getResource())
                    .user(config.getUser())
                    .context(config.getContext())
                    .rules(config.getRules().get(request.getResource()));
            readerRequest.setOriginalRequestId(request.getOriginalRequestId());

            LOGGER.debug("Calling dataReader with: {}", readerRequest);
            final DataReaderResponse readerResult = getDataReader().read(readerRequest,
                    this.getClass(),
                    auditRequestReceiver);
            LOGGER.debug("Reader returned: {}", readerResult);

            final ReadResponse response = new NoInputReadResponse(readerResult);
            LOGGER.debug("Returning from read: {}", response);
            return response;
        })
                .exceptionally(ex -> {
                    LOGGER.debug("Error handling: " + ex.getMessage());
                    auditRequestReceivedException(request, ex);
                    throw new RuntimeException(ex); //rethrow the exception
                });
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
        return dataReader;
    }

    public void setDataReader(final DataReader dataReader) {
        reader(dataReader);
    }

    public CacheService getCacheService() {
        requireNonNull(cacheService, "The cacheService service has not been set.");
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
