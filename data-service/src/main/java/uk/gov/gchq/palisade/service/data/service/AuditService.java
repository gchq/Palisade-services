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

package uk.gov.gchq.palisade.service.data.service;

import uk.gov.gchq.palisade.reader.request.DataReaderRequest;
import uk.gov.gchq.palisade.service.data.model.AuditSuccessMessage;
import uk.gov.gchq.palisade.service.data.model.DataRequest;

public interface AuditService {

    void auditSuccess(final String token, final AuditSuccessMessage successMessage);

    default AuditSuccessMessage createSuccessMessage(final DataRequest dataRequest, final DataReaderRequest readerRequest, final long recordsProcessed, final long recordsReturned) {
        return AuditSuccessMessage.Builder.create(dataRequest, readerRequest)
                .withRecordsProcessedAndReturned(recordsProcessed, recordsReturned);
    }

}
