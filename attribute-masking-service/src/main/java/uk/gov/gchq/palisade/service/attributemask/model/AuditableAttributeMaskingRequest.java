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

/**
 * This class is a container for {@code AttributeMaskingRequest} and {@code AuditErrorMessage} during stream processing.
 * Under normal conditions only one of these will be non-null, indicating failed or successful processing.
 */
public final class AuditableAttributeMaskingRequest {

    @JsonProperty("attributeMaskingRequest")
    private final AttributeMaskingRequest attributeMaskingRequest;
    @JsonProperty("auditErrorMessage")
    private final AuditErrorMessage auditErrorMessage;

    @JsonCreator
    private AuditableAttributeMaskingRequest(
            final @JsonProperty("attributeMaskingRequest") AttributeMaskingRequest attributeMaskingRequest,
            final @JsonProperty("auditErrorMessage") AuditErrorMessage auditErrorMessage) {
        this.attributeMaskingRequest = attributeMaskingRequest;
        this.auditErrorMessage = auditErrorMessage;
    }

    /**
     * Builder class for the creation of instances of the {@link AttributeMaskingRequest}.
     * This is a variant of the Fluent Builder which will use Java Objects or JsonNodes equivalents for the components in the build.
     */
    public static class Builder {

        /**
         * Starter method for the Builder class.
         * This method is called to start the process of creating the AuditableAttributeMaskingRequest class.
         *
         * @return interface {@link IAttributeMaskingRequest} for the next step in the build.
         */
        public static IAttributeMaskingRequest create() {
            return request -> audit -> new AuditableAttributeMaskingRequest(request, audit);
        }

        /**
         * Creates an AuditableAttributeMaskingRequest with the addition of a AttributeMaskingRequest
         */
        public interface IAttributeMaskingRequest {
            /**
             * Adds a AttributeMaskingRequest to the message
             *
             * @param request a AttributeMaskingRequest or a null value
             * @return interface {@link IAuditErrorMessage} for the next step in the build.
             */
            IAuditErrorMessage withAttributeMaskingRequest(AttributeMaskingRequest request);
        }

        /**
         * Adds a {@link AuditErrorMessage} to the message if an error was thrown in this service
         */
        public interface IAuditErrorMessage {
            /**
             * Adds a {@link AuditErrorMessage} to the message if an error was thrown in this service
             *
             * @param audit a AuditErrorMessage if one was thrown in the service or null if one was not thrown
             * @return a default interface {@link AuditableAttributeMaskingRequest} to attach a null AuditErrorMessage
             */
            AuditableAttributeMaskingRequest withAuditErrorMessage(AuditErrorMessage audit);

            /**
             * By default, add a null AuditErrorMessage to indicate that no error was thrown in the service
             *
             * @return class {@link AuditableAttributeMaskingRequest} for the completed class from the builder.
             */
            default AuditableAttributeMaskingRequest withNoError() {
                return this.withAuditErrorMessage(null);
            }
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
        AuditableAttributeMaskingRequest that = (AuditableAttributeMaskingRequest) o;
        return Objects.equals(attributeMaskingRequest, that.attributeMaskingRequest) &&
                Objects.equals(auditErrorMessage, that.auditErrorMessage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(attributeMaskingRequest, auditErrorMessage);
    }


    public AttributeMaskingRequest getAttributeMaskingRequest() {
        return attributeMaskingRequest;
    }

    public AuditErrorMessage getAuditErrorMessage() {
        return auditErrorMessage;
    }

}
