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

package uk.gov.gchq.palisade.service.palisade.model;

import com.fasterxml.jackson.annotation.JsonCreator;

import uk.gov.gchq.palisade.service.palisade.common.Generated;

import java.util.Objects;
import java.util.Optional;

/**
 * This class is a container for {@code PalisadeClientRequest} and {@code AuditErrorMessage} during stream processing.
 * Under normal conditions only one of these will be non-null, indicating failed or successful processing.
 */

public final class AuditablePalisadeSystemResponse {

    private final PalisadeSystemResponse palisadeResponse;
    private final AuditErrorMessage auditErrorMessage;

    @JsonCreator
    private AuditablePalisadeSystemResponse(
            final PalisadeSystemResponse palisadeResponse,
            final AuditErrorMessage auditErrorMessage) {
        this.palisadeResponse = palisadeResponse;
        this.auditErrorMessage = auditErrorMessage;
    }

    /**
     * Chain any errors from previous stream elements
     *
     * @param audit the previous audit or null
     * @return a new instance of this object
     */
    public AuditablePalisadeSystemResponse chain(final AuditErrorMessage audit) {
        return Optional.ofNullable(audit).map(message -> Builder.create()
                .withAuditErrorMessage(message))
                .orElse(this);
    }

    @Generated
    public PalisadeSystemResponse getPalisadeResponse() {
        return palisadeResponse;
    }

    @Generated
    public AuditErrorMessage getAuditErrorMessage() {
        return auditErrorMessage;
    }

    /**
     * The static builder
     */
    public static class Builder {

        /**
         * The creator function
         *
         * @return the composed immutable object
         */
        public static IAuditable create() {
            return AuditablePalisadeSystemResponse::new;
        }

        /**
         * Create an {@link AuditablePalisadeSystemResponse} supplying either the {@link PalisadeSystemResponse} or the {@link AuditErrorMessage}.
         * The non-supplied object is internally set to null.
         */
        public interface IAuditable {
            /**
             * Create an {@link AuditablePalisadeSystemResponse} with only an {@link AuditErrorMessage}
             * This is indicative that something went wrong.
             *
             * @param audit non-null value
             * @return value object
             */
            default AuditablePalisadeSystemResponse withAuditErrorMessage(AuditErrorMessage audit) {
                return withResponseAndError(null, audit);
            }

            /**
             * Create an {@link AuditablePalisadeSystemResponse} from  a {@link PalisadeClientRequest}.
             * This will be converted into an {@link PalisadeSystemResponse}.
             *
             * @param request non-null value
             * @return value object
             */
            default AuditablePalisadeSystemResponse withPalisadeRequest(PalisadeClientRequest request) {
                return withPalisadeResponse(PalisadeSystemResponse.Builder.create(request));
            }

            /**
             * Create an {@link AuditablePalisadeSystemResponse} from a {@link PalisadeSystemResponse} and no error.
             * This is indicative of a successful request registered.
             *
             * @param response non-null value
             * @return value object
             */
            default AuditablePalisadeSystemResponse withPalisadeResponse(PalisadeSystemResponse response) {
                return withResponseAndError(response, null);
            }

            /**
             * Create an {@link AuditablePalisadeSystemResponse} supplying both the {@link PalisadeSystemResponse} and the {@link AuditErrorMessage}.
             * It is expected that exactly one of these is null and the other non-null.
             *
             * @param request           value or null
             * @param auditErrorMessage value or null
             * @return value object
             */
            AuditablePalisadeSystemResponse withResponseAndError(PalisadeSystemResponse request, AuditErrorMessage auditErrorMessage);
        }
    }

    @Override
    @Generated
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AuditablePalisadeSystemResponse)) {
            return false;
        }
        final AuditablePalisadeSystemResponse that = (AuditablePalisadeSystemResponse) o;
        return Objects.equals(palisadeResponse, that.palisadeResponse) &&
                Objects.equals(auditErrorMessage, that.auditErrorMessage);
    }

    @Override
    @Generated
    public int hashCode() {
        return Objects.hash(palisadeResponse, auditErrorMessage);
    }
}
