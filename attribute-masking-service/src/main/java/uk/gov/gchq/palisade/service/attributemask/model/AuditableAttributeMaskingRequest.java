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
     * The static builder
     */
    public static class Builder {

        /**
         * Compose with {@code AttributeMaskingRequest}
         */
        public interface IAttributeMaskingRequest {
            /**
             * Compose value
             *
             * @param request or null
             * @return value object
             */
            IAuditErrorMessage withAttributeMaskingRequest(AttributeMaskingRequest request);
        }

        /**
         * Compose with {@code AuditErrorMessage}
         */
        public interface IAuditErrorMessage {
            /**
             * Compose value
             *
             * @param audit or null
             * @return value object
             */
            AuditableAttributeMaskingRequest withAuditErrorMessage(AuditErrorMessage audit);

            /**
             * Without error audit
             *
             * @return the composed immutable object
             */
            default AuditableAttributeMaskingRequest withNoError() {
                return this.withAuditErrorMessage(null);
            }
        }

        /**
         * The creator function
         *
         * @return the composed immutable object
         */
        public static IAttributeMaskingRequest create() {
            return request -> audit -> new AuditableAttributeMaskingRequest(request, audit);
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
