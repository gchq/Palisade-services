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

package uk.gov.gchq.palisade.service.attributemask.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;
import java.util.Optional;

/**
 * This class is a container for {@code AttributeMaskingResponse} and {@code AuditErrorMessage} during stream processing.
 * Under normal conditions only one of these will be non-null, indicating failed or successful processing.
 */
public final class AuditableAttributeMaskingResponse {

    @JsonProperty("attributeMaskingResponse")
    private final AttributeMaskingResponse attributeMaskingResponse;
    @JsonProperty("auditErrorMessage")
    private final AuditErrorMessage auditErrorMessage;

    @JsonCreator
    private AuditableAttributeMaskingResponse(
            final @JsonProperty("attributeMaskingResponse") AttributeMaskingResponse attributeMaskingResponse,
            final @JsonProperty("auditErrorMessage") AuditErrorMessage auditErrorMessage) {
        this.attributeMaskingResponse = attributeMaskingResponse;
        this.auditErrorMessage = auditErrorMessage;
    }

    /**
     * Chain any errors from previous stream elements
     *
     * @param audit the previous audit or null
     * @return a new instance of this object
     */
    public AuditableAttributeMaskingResponse chain(final AuditErrorMessage audit) {
        return Optional.ofNullable(audit).map(message -> AuditableAttributeMaskingResponse.Builder.create()
                .withAttributeMaskingResponse(this.attributeMaskingResponse)
                .withAuditErrorMessage(message))
                .orElse(this);
    }

    /**
     * Builder class for the creation of instances of the {@link AttributeMaskingResponse}.
     * This is a variant of the Fluent Builder which will use Java Objects or JsonNodes equivalents for the components in the build.
     */
    public static class Builder {

        /**
         * Starter method for the Builder class.
         * This method is called to start the process of creating the AuditableAttributeMaskingResponse class.
         *
         * @return interface {@link IAttributeMaskingResponse} for the next step in the build.
         */
        public static IAttributeMaskingResponse create() {
            return request -> audit -> new AuditableAttributeMaskingResponse(request, audit);
        }

        /**
         * Creates an AuditableAttributeMaskingResponse with the addition of a AttributeMaskingResponse
         */
        public interface IAttributeMaskingResponse {
            /**
             * Adds a AttributeMaskingResponse to the message
             *
             * @param response a AttributeMaskingResponse or a null value
             * @return interface {@link IAuditErrorMessage} for the next step in the build.
             */
            IAuditErrorMessage withAttributeMaskingResponse(AttributeMaskingResponse response);
        }

        /**
         * Adds a {@link AuditErrorMessage} to the message if an error was thrown in this service
         */
        public interface IAuditErrorMessage {
            /**
             * Adds a {@link AuditErrorMessage} to the message if an error was thrown in this service
             *
             * @param audit a AuditErrorMessage if one was thrown in the service or null if one was not thrown
             * @return class {@link AuditableAttributeMaskingResponse} for the completed class from the builder.
             */
            AuditableAttributeMaskingResponse withAuditErrorMessage(AuditErrorMessage audit);
        }
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AuditableAttributeMaskingResponse that = (AuditableAttributeMaskingResponse) o;
        return Objects.equals(attributeMaskingResponse, that.attributeMaskingResponse) &&
                Objects.equals(auditErrorMessage, that.auditErrorMessage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(attributeMaskingResponse, auditErrorMessage);
    }


    public AttributeMaskingResponse getAttributeMaskingResponse() {
        return attributeMaskingResponse;
    }

    public AuditErrorMessage getAuditErrorMessage() {
        return auditErrorMessage;
    }

}
