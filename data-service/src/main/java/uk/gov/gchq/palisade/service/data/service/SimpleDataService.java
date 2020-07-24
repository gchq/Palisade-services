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

import uk.gov.gchq.palisade.Generated;
import uk.gov.gchq.palisade.RequestId;
import uk.gov.gchq.palisade.data.serialise.Serialiser;
import uk.gov.gchq.palisade.reader.common.DataFlavour;
import uk.gov.gchq.palisade.reader.common.DataReader;
import uk.gov.gchq.palisade.reader.request.DataReaderRequest;
import uk.gov.gchq.palisade.reader.request.DataReaderResponse;
import uk.gov.gchq.palisade.service.data.exception.ReadException;
import uk.gov.gchq.palisade.service.data.request.AuditRequest;
import uk.gov.gchq.palisade.service.data.request.GetDataRequestConfig;
import uk.gov.gchq.palisade.service.data.request.NoInputReadResponse;
import uk.gov.gchq.palisade.service.data.request.ReadRequest;
import uk.gov.gchq.palisade.service.data.request.ReadResponse;
import uk.gov.gchq.palisade.service.request.DataRequestConfig;

import java.io.IOException;
import java.io.OutputStream;
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
    private AuditService auditService;
    private PalisadeService palisadeService;
    private DataReader dataReader;

    public SimpleDataService(final AuditService auditService, final PalisadeService palisadeService, final DataReader dataReader) {
        this.auditService = auditService;
        this.palisadeService = palisadeService;
        this.dataReader = dataReader;
    }

    @Generated
    public SimpleDataService auditService(final AuditService auditService) {
        requireNonNull(auditService, "The audit service cannot be set to null");
        this.setAuditService(auditService);
        return this;
    }

    @Generated
    public SimpleDataService palisadeService(final PalisadeService palisadeService) {
        requireNonNull(palisadeService, "The palisade service cannot be set to null.");
        this.setPalisadeService(palisadeService);
        return this;
    }

    @Generated
    public SimpleDataService dataReader(final DataReader dataReader) {
        requireNonNull(dataReader, "The data reader cannot be set to null.");
        this.setDataReader(dataReader);
        return this;
    }

    DataReaderRequest constructReaderRequest(final ReadRequest request) {
        final GetDataRequestConfig getConfig = new GetDataRequestConfig()
                .token(new RequestId().id(request.getToken()))
                .resource(request.getResource());
        getConfig.setOriginalRequestId(request.getOriginalRequestId());
        LOGGER.info("Calling palisade service with: {}", getConfig);

        // If this throws an exception connecting to the palisade-service, then it must be caught and audited
        final DataRequestConfig config = palisadeService.getDataRequestConfig(getConfig).join();
        LOGGER.info("Palisade service returned: {}", config);

        final DataReaderRequest readerRequest = new DataReaderRequest()
                .resource(request.getResource())
                .user(config.getUser())
                .context(config.getContext())
                .rules(config.getRules().get(request.getResource()));
        readerRequest.setOriginalRequestId(request.getOriginalRequestId());

        return readerRequest;
    }

    @Override
    public void read(final ReadRequest dataRequest, final OutputStream out) throws IOException {
        try {
            final AtomicLong recordsProcessed = new AtomicLong(0);
            final AtomicLong recordsReturned = new AtomicLong(0);

            LOGGER.debug("Querying palisade service for token {} and resource {}", dataRequest.getToken(), dataRequest.getResource());
            // This realistically may throw a RuntimeException, ensure that it is caught for auditing
            DataReaderRequest readerRequest = constructReaderRequest(dataRequest);

            LOGGER.debug("Reading from reader with request {}", readerRequest);
            DataReaderResponse readerResponse = getDataReader().read(readerRequest, recordsProcessed, recordsReturned);

            LOGGER.debug("Writing reader response {} to output stream", readerResponse);
            ReadResponse dataResponse = new NoInputReadResponse(readerResponse).message(dataRequest.toString());
            dataResponse.writeTo(out);
            out.close();

            LOGGER.debug("Output stream closed, {} processed and {} returned, auditing success with audit service", recordsProcessed.get(), recordsReturned.get());
            auditRequestComplete(readerRequest, recordsProcessed, recordsReturned);
        } catch (RuntimeException ex) {
            auditRequestReceivedException(dataRequest, ex);
            throw new ReadException(ex);
        } catch (Error err) {
            // Either an auditRequestComplete or auditRequestException MUST be called here, so catch a broader set of Exception classes than might be expected
            // Generally this is a bad idea, but we need guarantees of the audit - ie. malicious attempt at StackOverflowError
            auditRequestReceivedException(dataRequest, err);
            // Rethrow this Error, don't wrap it in the ReadException
            throw err;
        }
    }

    @Override
    public Boolean addSerialiser(final DataFlavour flavour, final Serialiser<?> serialiser) {
        LOGGER.info("Adding serialiser {} for DataFlavour {}", serialiser, flavour);

        getDataReader().addSerialiser(flavour, serialiser);

        return true;
    }

    private void auditRequestReceivedException(final ReadRequest request, final Throwable ex) {
        LOGGER.error("Error while handling request: {}", request);
        LOGGER.error("Exception was", ex);
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

    @Generated
    public AuditService getAuditService() {
        return auditService;
    }

    @Generated
    public void setAuditService(final AuditService auditService) {
        requireNonNull(auditService);
        this.auditService = auditService;
    }

    @Generated
    public PalisadeService getPalisadeService() {
        return palisadeService;
    }

    @Generated
    public void setPalisadeService(final PalisadeService palisadeService) {
        requireNonNull(palisadeService);
        this.palisadeService = palisadeService;
    }

    @Generated
    public DataReader getDataReader() {
        return dataReader;
    }

    @Generated
    public void setDataReader(final DataReader dataReader) {
        requireNonNull(dataReader);
        this.dataReader = dataReader;
    }
}
