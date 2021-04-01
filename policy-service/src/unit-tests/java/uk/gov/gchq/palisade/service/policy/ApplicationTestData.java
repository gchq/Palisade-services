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
package uk.gov.gchq.palisade.service.policy;

import uk.gov.gchq.palisade.service.policy.common.Context;
import uk.gov.gchq.palisade.service.policy.common.User;
import uk.gov.gchq.palisade.service.policy.common.UserId;
import uk.gov.gchq.palisade.service.policy.common.resource.LeafResource;
import uk.gov.gchq.palisade.service.policy.common.resource.impl.FileResource;
import uk.gov.gchq.palisade.service.policy.common.resource.impl.SystemResource;
import uk.gov.gchq.palisade.service.policy.common.rule.Rule;
import uk.gov.gchq.palisade.service.policy.common.rule.Rules;
import uk.gov.gchq.palisade.service.policy.common.service.SimpleConnectionDetail;
import uk.gov.gchq.palisade.service.policy.exception.NoSuchPolicyException;
import uk.gov.gchq.palisade.service.policy.model.AuditErrorMessage;
import uk.gov.gchq.palisade.service.policy.model.AuditablePolicyRecordResponse;
import uk.gov.gchq.palisade.service.policy.model.AuditablePolicyResourceResponse;
import uk.gov.gchq.palisade.service.policy.model.AuditablePolicyResourceRules;
import uk.gov.gchq.palisade.service.policy.model.PolicyRequest;
import uk.gov.gchq.palisade.service.policy.model.PolicyResponse;

import java.io.Serializable;
import java.util.HashMap;

public class ApplicationTestData {
    public static final UserId USER_ID = new UserId().id("test-user-id");
    public static final User USER = new User().userId(USER_ID);
    public static final String RESOURCE_ID = "/test/resourceId";
    public static final String RESOURCE_TYPE = "uk.gov.gchq.palisade.test.TestType";
    public static final String RESOURCE_FORMAT = "avro";
    public static final String DATA_SERVICE_NAME = "test-data-service";
    public static final String RESOURCE_PARENT = "/test";

    public static final LeafResource LEAF_RESOURCE = new FileResource()
            .id(RESOURCE_ID)
            .type(RESOURCE_TYPE)
            .serialisedFormat(RESOURCE_FORMAT)
            .connectionDetail(new SimpleConnectionDetail().serviceName(DATA_SERVICE_NAME))
            .parent(new SystemResource().id(RESOURCE_PARENT));

    public static final String PURPOSE = "test-purpose";
    public static final Context CONTEXT = new Context().purpose(PURPOSE);
    public static final String RULE_MESSAGE = "test-rule";

    public static final Rules<LeafResource> RESOURCE_RULES = new Rules<LeafResource>().addRule(RULE_MESSAGE, new PassThroughRule<>());
    public static final Rules<Serializable> RULES = new Rules<>().addRule(RULE_MESSAGE, new PassThroughRule<>());

    public static final PolicyRequest REQUEST = PolicyRequest.Builder.create()
            .withUserId(USER_ID.getId())
            .withResourceId(RESOURCE_ID)
            .withContext(CONTEXT)
            .withUser(USER)
            .withResource(LEAF_RESOURCE);

    public static final PolicyResponse RESPONSE = PolicyResponse.Builder.create(REQUEST)
            .withRules(RULES);
    public static final PolicyResponse RESPONSE_NO_RULES = PolicyResponse.Builder.create(REQUEST)
            .withRules(new Rules<>());

    public static final AuditErrorMessage AUDIT_ERROR_MESSAGE = AuditErrorMessage.Builder.create()
            .withUserId(USER_ID.getId())
            .withResourceId(RESOURCE_ID)
            .withContext(CONTEXT)
            .withAttributes((new HashMap<>()))
            .withError(new NoSuchPolicyException("No rules found for the resource"));

    public static final AuditablePolicyResourceRules AUDITABLE_POLICY_RESOURCE_RULES_NULL = AuditablePolicyResourceRules.Builder.create()
            .withPolicyRequest(null)
            .withRules(null)
            .withNoErrors();
    public static final AuditablePolicyResourceRules AUDITABLE_POLICY_RESOURCE_RULES_NO_ERROR = AuditablePolicyResourceRules.Builder.create()
            .withPolicyRequest(REQUEST)
            .withRules(RESOURCE_RULES)
            .withNoErrors();
    public static final AuditablePolicyResourceRules AUDITABLE_POLICY_RESOURCE_RULES_NO_RULES = AuditablePolicyResourceRules.Builder.create()
            .withPolicyRequest(REQUEST)
            .withRules(null)
            .withAuditErrorMessage(AUDIT_ERROR_MESSAGE);

    public static final AuditablePolicyRecordResponse AUDITABLE_POLICY_RECORD_RESPONSE_NO_ERROR = AuditablePolicyRecordResponse.Builder.create()
            .withPolicyResponse(RESPONSE)
            .withAuditErrorMessage(null);

    public static final AuditablePolicyResourceResponse AUDITABLE_POLICY_RESOURCE_RESPONSE = AuditablePolicyResourceResponse.Builder.create()
            .withPolicyRequest(REQUEST).withRules(RESOURCE_RULES).withNoErrors().withNoModifiedResource();
    public static final AuditablePolicyResourceResponse AUDITABLE_POLICY_RESOURCE_RESPONSE_WITH_NO_RULES = AuditablePolicyResourceResponse.Builder.create()
            .withPolicyRequest(REQUEST).withRules(null).withNoErrors().withNoModifiedResource();


    /**
     * Common test data for all classes
     */

    private ApplicationTestData() {
        // hide the constructor, this is just a collection of static objects
    }

    public static class PassThroughRule<T extends Serializable> implements Rule<T> {
        @Override
        public T apply(final T record, final User user, final Context context) {
            return record;
        }
    }
}
