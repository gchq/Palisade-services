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

import uk.gov.gchq.palisade.reader.request.DataReaderRequest;
import uk.gov.gchq.palisade.service.data.model.AuditSuccessMessage;
import uk.gov.gchq.palisade.service.data.model.DataRequest;

/**
 * Interface to the audit-service for auditing successful data-reads along with some required metadata,
 * such as records processed and records returned (processed - redacted).
 */
public interface AuditService {

    /**
     * Audit a successful read to the audit-service
     *
     * @param token          the client's (unique) request token
     * @param successMessage the constructed message detailing the resource read, the rules applied and other metadata
     * @implNote Any implementation of this should ensure it has confirmation that the message has been persisted downstream.
     * This provides assurances that the audit logs won't go missing due to processing failures.
     * This is probably implemented as blocking until the persistence-write (kafka/redis/etc.) completes and throwing a
     * {@link RuntimeException} if processing fails.
     */
    void auditSuccess(final String token, final AuditSuccessMessage successMessage);

    /**
     * Convenience method for converting the pair of {@link DataRequest} and {@link DataReaderRequest} objects from the data-service
     * into the {@link AuditSuccessMessage} required for the audit-service, attaching any additional required metadata.
     *
     * @param dataRequest      the client's {@link DataRequest} sent to the data-service
     * @param readerRequest    the persisted-and-retrieved rules for the data access,
     *                         passed as a request to the {@link uk.gov.gchq.palisade.reader.common.DataReader}
     * @param recordsProcessed the total number of records processed by the data-reader (number of records in the resource)
     * @param recordsReturned  the number of records returned to the client after applying record-level redaction (processed - redacted)
     * @return an {@link AuditSuccessMessage} to be used by this class
     */
    default AuditSuccessMessage createSuccessMessage(final DataRequest dataRequest, final DataReaderRequest readerRequest, final long recordsProcessed, final long recordsReturned) {
        return AuditSuccessMessage.Builder.create(dataRequest, readerRequest)
                .withRecordsProcessedAndReturned(recordsProcessed, recordsReturned);
    }

}
