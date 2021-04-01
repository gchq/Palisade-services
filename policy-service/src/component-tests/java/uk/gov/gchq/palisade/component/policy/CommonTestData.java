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

package uk.gov.gchq.palisade.component.policy;

import uk.gov.gchq.palisade.service.policy.ApplicationTestData.PassThroughRule;
import uk.gov.gchq.palisade.service.policy.common.Context;
import uk.gov.gchq.palisade.service.policy.common.User;
import uk.gov.gchq.palisade.service.policy.common.resource.LeafResource;
import uk.gov.gchq.palisade.service.policy.common.resource.impl.FileResource;
import uk.gov.gchq.palisade.service.policy.common.resource.impl.SystemResource;
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

/**
 * Common test data for all classes
 * This cements the expected JSON input and output, providing an external contract for the service
 */
public class CommonTestData {

    public static final String REQUEST_TOKEN = "test-request-token";
    public static final String USER_ID = "testUserId";
    public static final String RESOURCE_ID = "/test/resourceId";
    public static final Context CONTEXT = new Context().purpose("testContext");
    public static final User USER = new User().userId(USER_ID);
    public static final LeafResource LEAF_RESOURCE = new FileResource().id("/test/file.format")
            .type("java.lang.String")
            .serialisedFormat("format")
            .connectionDetail(new SimpleConnectionDetail().serviceName("test-service"))
            .parent(new SystemResource().id("/test"));
    public static final Rules<Serializable> RULES = new Rules<>().addRule("test-rule", new PassThroughRule<>());
    public static final Rules<LeafResource> RESOURCE_RULES = new Rules<LeafResource>().addRule("test-rule", new PassThroughRule<>());

    public static final AuditErrorMessage AUDIT_ERROR_MESSAGE = AuditErrorMessage.Builder.create()
            .withUserId(USER_ID)
            .withResourceId(RESOURCE_ID)
            .withContext(CONTEXT)
            .withAttributes((new HashMap<>()))
            .withError(new NoSuchPolicyException("No rules found for the resource"));

    public static final PolicyRequest POLICY_REQUEST = PolicyRequest.Builder.create()
            .withUserId(USER_ID)
            .withResourceId(RESOURCE_ID)
            .withContext(CONTEXT)
            .withUser(USER)
            .withResource(LEAF_RESOURCE);

    public static final PolicyResponse POLICY_RESPONSE = PolicyResponse.Builder.create()
            .withUserId(USER_ID)
            .withResourceId(RESOURCE_ID)
            .withContext(CONTEXT)
            .withUser(USER)
            .withResource(LEAF_RESOURCE)
            .withRules(RULES);

    public static final AuditablePolicyRecordResponse POLICY_RECORD_RESPONSE = AuditablePolicyRecordResponse.Builder.create()
            .withPolicyResponse(POLICY_RESPONSE)
            .withNoErrors();

    public static final AuditablePolicyRecordResponse POLICY_RECORD_RESPONSE_ERROR = AuditablePolicyRecordResponse.Builder.create()
            .withPolicyResponse(null)
            .withAuditErrorMessage(AUDIT_ERROR_MESSAGE);

    public static final AuditablePolicyResourceResponse POLICY_RESOURCE_RESPONSE = AuditablePolicyResourceResponse.Builder.create()
            .withPolicyRequest(POLICY_REQUEST)
            .withRules(RESOURCE_RULES)
            .withNoErrors()
            .withModifiedResource(LEAF_RESOURCE);

    public static final AuditablePolicyResourceResponse POLICY_RESOURCE_RESPONSE_ERROR = AuditablePolicyResourceResponse.Builder.create()
            .withPolicyRequest(POLICY_REQUEST)
            .withRules(RESOURCE_RULES)
            .withAuditErrorMessage(AUDIT_ERROR_MESSAGE)
            .withModifiedResource(LEAF_RESOURCE);

    public static final AuditablePolicyResourceRules POLICY_RESOURCE_RULES = AuditablePolicyResourceRules.Builder.create()
            .withPolicyRequest(POLICY_REQUEST)
            .withRules(RESOURCE_RULES)
            .withNoErrors();

    public static final AuditablePolicyResourceRules POLICY_RESOURCE_RULES_ERROR = AuditablePolicyResourceRules.Builder.create()
            .withPolicyRequest(POLICY_REQUEST)
            .withRules(RESOURCE_RULES)
            .withAuditErrorMessage(AUDIT_ERROR_MESSAGE);
}
