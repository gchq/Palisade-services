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
 * This class is a container for {@code PalisadeClientResponse} and {@code AuditErrorMessage} during stream processing.
 * Under normal conditions only one of these will be non-null, if a PalisadeSystemResponse is attached the the request has been successful,
 * otherwise an AuditErrorMessage will be attached
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
     * Builder class for the creation of instances of the AuditablePalisadeSystemResponse.
     * This is a variant of the Fluent Builder
     */
    public static class Builder {

        /**
         * Creates a new instance of the AuditablePalisadeSystemResponse
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
             * @param auditErrorMessage an error to be attached to the response object
             * @return a link to the interface required to add an {@link AuditErrorMessage} and a null {@link PalisadeSystemResponse} to a now completed {@link AuditablePalisadeSystemResponse}.
             */
            default AuditablePalisadeSystemResponse withAuditErrorMessage(AuditErrorMessage auditErrorMessage) {
                return withResponseAndError(null, auditErrorMessage);
            }

            /**
             * Create an {@link AuditablePalisadeSystemResponse} from a {@link PalisadeClientRequest}.
             * This will be converted into an {@link PalisadeSystemResponse}.
             *
             * @param clientRequest a request from the client to be attached to the response object
             * @return a link to the interface required to add a {@link PalisadeSystemResponse} and null {@link AuditErrorMessage} to the now completed {@link AuditablePalisadeSystemResponse}
             */
            default AuditablePalisadeSystemResponse withPalisadeRequest(PalisadeClientRequest clientRequest) {
                return withPalisadeResponse(PalisadeSystemResponse.Builder.create(clientRequest));
            }

            /**
             * Create an {@link AuditablePalisadeSystemResponse} from a {@link PalisadeSystemResponse} and no error.
             * This is indicative of a successful request registered.
             *
             * @param response a non-null {@link PalisadeSystemResponse}
             * @return a link to the interface required to add a {@link PalisadeSystemResponse} and a null {@link AuditErrorMessage} to a now completed {@link AuditablePalisadeSystemResponse}.
             */
            default AuditablePalisadeSystemResponse withPalisadeResponse(PalisadeSystemResponse response) {
                return withResponseAndError(response, null);
            }

            /**
             * Create an {@link AuditablePalisadeSystemResponse} supplying both the {@link PalisadeSystemResponse} and the {@link AuditErrorMessage}.
             * It is expected that exactly one of these is null and the other non-null.
             *
             * @param response          either a {@link PalisadeSystemResponse} or null if the request was unsuccessful
             * @param auditErrorMessage either null, if the request was successful, or an error message in a {@link AuditErrorMessage}
             * @return a now completed {@link AuditablePalisadeSystemResponse} with either a response or an auditErrorMessage
             */
            AuditablePalisadeSystemResponse withResponseAndError(PalisadeSystemResponse response, AuditErrorMessage auditErrorMessage);
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
