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

package uk.gov.gchq.palisade.service.attributemask.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;
import java.util.Optional;

public class AuditableAttributeMaskingResponse implements AuditableResult {

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

    @Override
    public Class<?> getType() {
        return this.attributeMaskingResponse.getClass();
    }

    public AuditableAttributeMaskingResponse chain(final AuditErrorMessage audit) {
        return Optional.ofNullable(audit).map(message -> AuditableAttributeMaskingResponse.Builder.create()
                .withAttributeMaskingResponse(this.attributeMaskingResponse)
                .withAuditErrorMessage(message))
                    .orElse(this);
    }

    public static class Builder {

        public interface IAttributeMaskingResponse {
            AuditableAttributeMaskingResponse.Builder.IAuditErrorMessage withAttributeMaskingResponse(AttributeMaskingResponse response);
        }

        public interface IAuditErrorMessage {
            AuditableAttributeMaskingResponse withAuditErrorMessage(AuditErrorMessage audit);
        }

        public static AuditableAttributeMaskingResponse.Builder.IAttributeMaskingResponse create() {
            return request -> audit -> new AuditableAttributeMaskingResponse(request, audit);
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
