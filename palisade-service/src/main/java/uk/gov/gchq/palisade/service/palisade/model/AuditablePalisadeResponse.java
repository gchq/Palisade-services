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

package uk.gov.gchq.palisade.service.palisade.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import uk.gov.gchq.palisade.Generated;

import java.util.Objects;
import java.util.Optional;

/**
 * This class is a container for {@code PalisadeRequest} and {@code AuditErrorMessage} during stream processing.
 * Under normal conditions only one of these will be non-null, indicating failed or successful processing.
 */

public final class AuditablePalisadeResponse {

    @JsonProperty("palisadeRequest")
    private final PalisadeSystemResponse palisadeResponse;
    @JsonProperty("auditErrorMessage")
    private final AuditErrorMessage auditErrorMessage;

    @JsonCreator
    private AuditablePalisadeResponse(
            final @JsonProperty("palisadeRequest") PalisadeSystemResponse palisadeResponse,
            final @JsonProperty("auditErrorMessage") AuditErrorMessage auditErrorMessage) {
        this.palisadeResponse = palisadeResponse;
        this.auditErrorMessage = auditErrorMessage;
    }

    /**
     * Chain any errors from previous stream elements
     *
     * @param audit the previous audit or null
     * @return a new instance of this object
     */
    public AuditablePalisadeResponse chain(final AuditErrorMessage audit) {
        return Optional.ofNullable(audit).map(message -> Builder.create()
                .withAuditErrorMessage(message))
                .orElse(this);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final AuditablePalisadeResponse that = (AuditablePalisadeResponse) o;
        return Objects.equals(palisadeResponse, that.palisadeResponse) &&
                Objects.equals(auditErrorMessage, that.auditErrorMessage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(palisadeResponse, auditErrorMessage);
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
        public static IPalisadeRequest create() {
            return AuditablePalisadeResponse::new;
        }

        /**
         * Compose with {@code UserResponse}
         */
        public interface IPalisadeRequest {
            /**
             * Compose value
             *
             * @param audit value or null
             * @return value object
             */
            default AuditablePalisadeResponse withAuditErrorMessage(AuditErrorMessage audit) {
                return withResponseAndError(null, audit);
            }


            /**
             * Compose value
             *
             * @param request value or null
             * @return value object
             */
            default AuditablePalisadeResponse withPalisadeRequest(PalisadeRequest request) {
                return withResponseAndError(PalisadeSystemResponse.Builder.create(request), null);
            }

            /**
             * Compose value
             *
             * @param request           value or null
             * @param auditErrorMessage value or null
             * @return value object
             */
            AuditablePalisadeResponse withResponseAndError(PalisadeSystemResponse request, AuditErrorMessage auditErrorMessage);
        }

    }
}
