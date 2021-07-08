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
import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.rule.Rules;

import java.util.Objects;
import java.util.StringJoiner;

/**
 * This class is a container for {@link PolicyRequest}, {@link Rules} and {@link AuditErrorMessage} during stream
 * processing. The class represents the data after making the request for the rules. Under normal conditions,
 * {@link PolicyRequest} and {@link Rules} will be non-null, indicating successful process OR {@link AuditErrorMessage}
 * when there has been an error in the process.
 */
public final class AuditablePolicyResourceRules {

    private final PolicyRequest policyRequest;
    private final Rules<LeafResource> rules;
    private final AuditErrorMessage auditErrorMessage;

    private AuditablePolicyResourceRules(
            final PolicyRequest policyRequest, final Rules<LeafResource> rules, final AuditErrorMessage auditErrorMessage) {
        this.policyRequest = policyRequest;
        this.rules = rules;
        this.auditErrorMessage = auditErrorMessage;
    }

    @Generated
    public PolicyRequest getPolicyRequest() {
        return policyRequest;
    }

    @Generated
    public Rules<LeafResource> getRules() {
        return rules;
    }

    @Generated
    public AuditErrorMessage getAuditErrorMessage() {
        return auditErrorMessage;
    }


    /**
     * Builder class for the creation of instances of the AuditablePolicyRecordResponse.
     * This is a variant of the Fluent Builder.
     */
    public static class Builder {

        /**
         * Starter method for the Builder class. This method is called to start the process of creating the
         * AuditablePolicyResourceRules class.
         *
         * @return public interface {@link IPolicyRequest} for the next step in the build.
         */
        public static IPolicyRequest create() {
            return request -> rules -> audit -> new AuditablePolicyResourceRules(request, rules, audit);
        }

        /**
         * Adds the {@link PolicyRequest} to the message.
         */
        public interface IPolicyRequest {
            /**
             * Adds the {@link PolicyRequest} to the message.
             *
             * @param policyRequest or null if there was an issue processing the request
             * @return interface {@link IRules} for the next step in the build
             */
            IRules withPolicyRequest(PolicyRequest policyRequest);
        }

        /**
         * Adds the {@link Rules} to the message.
         */
        public interface IRules {
            /**
             * Adds the {@link Rules} to the message.
             *
             * @param rules or null if there was an issue finding rules for the resource
             * @return interface {@link IAuditErrorMessage} for the next step in the build
             */
            IAuditErrorMessage withRules(Rules<LeafResource> rules);
        }

        /**
         * Adds the {@link AuditErrorMessage} to the message.
         */
        public interface IAuditErrorMessage {
            /**
             * Adds the {@link AuditErrorMessage} to the message.
             *
             * @param audit or null if the request was processed successfully
             * @return class {@link AuditablePolicyResourceRules} for the completed class from the builder.
             */
            AuditablePolicyResourceRules withAuditErrorMessage(AuditErrorMessage audit);

            /**
             * By default, add a null AuditErrorMessage to the message, implying the resource was processed successfully
             *
             * @return class {@link AuditablePolicyResourceRules} for the completed class from the builder.
             */
            default AuditablePolicyResourceRules withNoErrors() {
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
        if (!(o instanceof AuditablePolicyResourceRules)) {
            return false;
        }
        AuditablePolicyResourceRules that = (AuditablePolicyResourceRules) o;
        return policyRequest.equals(that.policyRequest) &&
                Objects.equals(rules, that.rules) &&
                Objects.equals(auditErrorMessage, that.auditErrorMessage);
    }

    @Override
    @Generated
    public int hashCode() {
        return Objects.hash(policyRequest, rules, auditErrorMessage);
    }

    @Override
    @Generated
    public String toString() {
        return new StringJoiner(", ", AuditablePolicyResourceRules.class.getSimpleName() + "[", "]")
                .add("policyRequest=" + policyRequest)
                .add("rules=" + rules)
                .add("auditErrorMessage=" + auditErrorMessage)
                .toString();
    }
}
