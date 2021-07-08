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
package uk.gov.gchq.palisade.service.policy.model;

import uk.gov.gchq.palisade.Generated;

import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;

/**
 * This class is a container for {@link PolicyResponse} and {@link AuditErrorMessage} during stream processing.
 * Under normal conditions only one of these will be non-null, indicating failed or successful processing.
 */
public final class AuditablePolicyRecordResponse {

    private final PolicyResponse policyResponse;
    private final AuditErrorMessage auditErrorMessage;

    private AuditablePolicyRecordResponse(
            final PolicyResponse policyResponse,
            final AuditErrorMessage auditErrorMessage) {
        this.policyResponse = policyResponse;
        this.auditErrorMessage = auditErrorMessage;
    }

    /**
     * Chain any errors from previous stream elements
     *
     * @param audit the previous audit or null
     * @return a new instance of this object
     */
    public AuditablePolicyRecordResponse chain(final AuditErrorMessage audit) {
        return Optional.ofNullable(audit).map(message -> AuditablePolicyRecordResponse.Builder.create()
                .withPolicyResponse(this.policyResponse)
                .withAuditErrorMessage(message))
                .orElse(this);
    }

    @Generated
    public PolicyResponse getPolicyResponse() {
        return policyResponse;
    }

    @Generated
    public AuditErrorMessage getAuditErrorMessage() {
        return auditErrorMessage;
    }

    /**
     * Builder class for the creation of instances of the AuditablePolicyRecordResponse.
     * This is a variant of the Fluent Builder
     */
    public static class Builder {

        /**
         * Starter method for the Builder class. This method is called to start the process of creating the
         * AuditablePolicyRecordResponse class.
         *
         * @return public interface {@link IPolicyResponse} for the next step in the build.
         */
        public static IPolicyResponse create() {
            return request -> audit -> new AuditablePolicyRecordResponse(request, audit);
        }

        /**
         * Adds the PolicyResponse to the message.
         */
        public interface IPolicyResponse {
            /**
             * Adds the PolicyResponse to the message
             *
             * @param response a PolicyResponse or null if there was an issue retrieving the rules, or no rules existed
             * @return interface {@link IAuditErrorMessage} for the next step in the build
             */
            IAuditErrorMessage withPolicyResponse(PolicyResponse response);
        }

        /**
         * Adds the AuditErrorMessage to the message.
         */
        public interface IAuditErrorMessage {
            /**
             * Adds the AuditErrorMessage to the message.
             *
             * @param audit an AuditErrorMessage if there was an issue retrieving rules, or no rules existed for this resource, or null if a PolicyResponse was created
             * @return class {@link AuditablePolicyRecordResponse} for the completed class from the builder.
             */
            AuditablePolicyRecordResponse withAuditErrorMessage(AuditErrorMessage audit);

            /**
             * By default, add no AuditErrorMessage to the builder
             *
             * @return class {@link AuditablePolicyRecordResponse} for the completed class from the builder.
             */
            default AuditablePolicyRecordResponse withNoErrors() {
                return this.withAuditErrorMessage(null);

            }
        }
    }

    @Override
    @Generated
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AuditablePolicyRecordResponse)) {
            return false;
        }
        AuditablePolicyRecordResponse that = (AuditablePolicyRecordResponse) o;
        return Objects.equals(policyResponse, that.policyResponse) &&
                Objects.equals(auditErrorMessage, that.auditErrorMessage);
    }

    @Override
    @Generated
    public int hashCode() {
        return Objects.hash(policyResponse, auditErrorMessage);
    }

    @Override
    @Generated
    public String toString() {
        return new StringJoiner(", ", AuditablePolicyRecordResponse.class.getSimpleName() + "[", "]")
                .add("policyResponse=" + policyResponse)
                .add("auditErrorMessage=" + auditErrorMessage)
                .toString();
    }
}
