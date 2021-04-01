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
import uk.gov.gchq.palisade.service.policy.common.resource.Resource;
import uk.gov.gchq.palisade.service.policy.common.rule.Rules;

import java.util.Objects;
import java.util.StringJoiner;

/**
 * The class represents the resource have been modified by applying the rules.  This class is a container for
 * {@link PolicyRequest}, {@link Rules} and {@link AuditErrorMessage} during stream processing.  Under normal
 * conditions {@code PolicyRequest} and {@code Rules} will be non-null, indicating successful process or
 * {@code AuditErrorMessage} when there has been an error in the process.
 */
public final class AuditablePolicyResourceResponse {

    private final PolicyRequest policyRequest;
    private final Rules<LeafResource> rules;
    private final AuditErrorMessage auditErrorMessage;
    private final Resource modifiedResource;


    private AuditablePolicyResourceResponse(
            final PolicyRequest policyRequest,
            final Rules<LeafResource> rules,
            final AuditErrorMessage auditErrorMessage,
            final Resource modifiedResource) {

        this.policyRequest = policyRequest;
        this.rules = rules;
        this.auditErrorMessage = auditErrorMessage;
        this.modifiedResource = modifiedResource;

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

    @Generated
    public Resource getModifiedResource() {
        return modifiedResource;
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
            return request -> rules -> audit -> modifiedResource -> new AuditablePolicyResourceResponse(request, rules, audit, modifiedResource);
        }

        /**
         * Takes the data for the request, rules.  Is a partial constructor and is expecting the modified resources
         * to complete the build of the class.
         *
         * @param auditablePolicyResourceRules holds the data from the request and the related rules
         * @return the composed immutable object
         */
        public static IModifiedResource create(final AuditablePolicyResourceRules auditablePolicyResourceRules) {
            return create().withPolicyRequest(auditablePolicyResourceRules.getPolicyRequest())
                    .withRules(auditablePolicyResourceRules.getRules())
                    .withAuditErrorMessage(auditablePolicyResourceRules.getAuditErrorMessage());
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
            IModifiedResource withAuditErrorMessage(AuditErrorMessage audit);

            /**
             * Without error audit
             *
             * @return the composed immutable object
             */
            default IModifiedResource withNoErrors() {
                return this.withAuditErrorMessage(null);
            }
        }

        /**
         * Compose with {@code AuditErrorMessage}
         */
        public interface IModifiedResource {
            /**
             * Compose value
             *
             * @param modifiedResource or null
             * @return value object
             */
            AuditablePolicyResourceResponse withModifiedResource(Resource modifiedResource);

            /**
             * Without error audit
             *
             * @return the composed immutable object
             */
            default AuditablePolicyResourceResponse withNoModifiedResource() {
                return this.withModifiedResource(null);
            }
        }


    }

    @Override
    @Generated
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AuditablePolicyResourceResponse)) {
            return false;
        }
        AuditablePolicyResourceResponse that = (AuditablePolicyResourceResponse) o;
        return policyRequest.equals(that.policyRequest) &&
                rules.equals(that.rules) &&
                Objects.equals(auditErrorMessage, that.auditErrorMessage) &&
                Objects.equals(modifiedResource, that.modifiedResource);
    }

    @Override
    @Generated
    public int hashCode() {
        return Objects.hash(policyRequest, rules, auditErrorMessage, modifiedResource);
    }

    @Override
    @Generated
    public String toString() {
        return new StringJoiner(", ", AuditablePolicyResourceResponse.class.getSimpleName() + "[", "]")
                .add("policyRequest=" + policyRequest)
                .add("rules=" + rules)
                .add("auditErrorMessage=" + auditErrorMessage)
                .add("modifiedResource=" + modifiedResource)
                .toString();
    }
}

