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

public class AuditableAttributeMaskingRequest implements AuditableResult {

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

    @Override
    public Class<?> getType() {
        return this.attributeMaskingRequest.getClass();
    }

    public static class Builder {

        public interface IAttributeMaskingRequest {
            IAuditErrorMessage withAttributeMaskingRequest(AttributeMaskingRequest request);
        }

        public interface IAuditErrorMessage {
            AuditableAttributeMaskingRequest withAuditErrorMessage(AuditErrorMessage audit);

            default AuditableAttributeMaskingRequest withNoError() {
                return this.withAuditErrorMessage(null);
            }
        }

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
