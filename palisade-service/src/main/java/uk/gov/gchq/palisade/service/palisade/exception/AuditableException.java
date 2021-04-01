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
package uk.gov.gchq.palisade.service.palisade.exception;

import org.apache.kafka.clients.consumer.ConsumerRecord;

import uk.gov.gchq.palisade.service.palisade.common.Generated;
import uk.gov.gchq.palisade.service.palisade.model.PalisadeClientRequest;

/**
 * Capture a thrown exception, and a kafka consumer record, wrapping them in a message to be caught by the supervisor.
 * This allows exceptions to be audited along with their original request.
 * These exceptions are elevated to {@link RuntimeException}s.
 */
public class AuditableException extends RuntimeException {
    private final transient ConsumerRecord<String, PalisadeClientRequest> request;

    /**
     * Capture a thrown exception, and a kafka consumer record, wrapping them in a message to be caught by the supervisor.
     * This allows exceptions to be audited along with their original request.
     *
     * @param request the original request to the service that caused the exception
     * @param cause   the exception that was thrown
     */
    public AuditableException(final ConsumerRecord<String, PalisadeClientRequest> request, final Exception cause) {
        super(cause);
        this.request = request;
    }

    @Generated
    public ConsumerRecord<String, PalisadeClientRequest> getRequest() {
        return request;
    }
}
