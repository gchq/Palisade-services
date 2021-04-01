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

import uk.gov.gchq.palisade.service.policy.common.Generated;
import uk.gov.gchq.palisade.service.policy.common.resource.LeafResource;
import uk.gov.gchq.palisade.service.policy.common.rule.Rules;

import java.util.Objects;
import java.util.StringJoiner;

/**
 * This class is a container for {@link PolicyRequest}, {@link Rules} and {@link AuditErrorMessage} during stream
 * processing.  The class represents the data after making the request for the rules. Under normal conditions
 * {@code PolicyRequest} and {@code Rules} will be non-null, indicating successful process OR {@code AuditErrorMessage}
 * when there has been an error in the process.
 */
public final class AuditablePolicyResourceRules {

    private final PolicyRequest policyRequest;
    private final Rules<LeafResource> rules;
    private final AuditErrorMessage auditErrorMessage;

    private AuditablePolicyResourceRules(
            final PolicyRequest policyRequest,
            final Rules<LeafResource> rules,
            final AuditErrorMessage auditErrorMessage) {

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
     * The static builder
     */
    public static class Builder {

        /**
         * The creator function
         *
         * @return the composed immutable object
         */
        public static IPolicyRequest create() {
            return request -> rules -> audit -> new AuditablePolicyResourceRules(request, rules, audit);
        }

        /**
         * Compose with {@code PolicyRequest}
         */
        public interface IPolicyRequest {
            /**
             * Compose value
             *
             * @param policyRequest or null
             * @return value object
             */
            IRules withPolicyRequest(PolicyRequest policyRequest);
        }

        /**
         * Compose with {@code Rules}
         */
        public interface IRules {
            /**
             * Compose value
             *
             * @param rules or null
             * @return value object
             */
            IAuditErrorMessage withRules(Rules<LeafResource> rules);
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
            AuditablePolicyResourceRules withAuditErrorMessage(AuditErrorMessage audit);

            /**
             * Without error audit
             *
             * @return the composed immutable object
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
