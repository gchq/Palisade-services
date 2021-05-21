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
package uk.gov.gchq.palisade.service.policy.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.rule.Rules;
import uk.gov.gchq.palisade.service.policy.exception.NoSuchPolicyException;
import uk.gov.gchq.palisade.service.policy.model.AuditErrorMessage;
import uk.gov.gchq.palisade.service.policy.model.AuditablePolicyRecordResponse;
import uk.gov.gchq.palisade.service.policy.model.AuditablePolicyResourceResponse;
import uk.gov.gchq.palisade.service.policy.model.PolicyResponse;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.gchq.palisade.service.policy.ApplicationTestData.AUDITABLE_POLICY_RECORD_RESPONSE_NO_ERROR;
import static uk.gov.gchq.palisade.service.policy.ApplicationTestData.AUDITABLE_POLICY_RESOURCE_RESPONSE;
import static uk.gov.gchq.palisade.service.policy.ApplicationTestData.AUDITABLE_POLICY_RESOURCE_RESPONSE_WITH_NO_RULES;
import static uk.gov.gchq.palisade.service.policy.ApplicationTestData.AUDITABLE_POLICY_RESOURCE_RULES_NO_ERROR;
import static uk.gov.gchq.palisade.service.policy.ApplicationTestData.AUDITABLE_POLICY_RESOURCE_RULES_NO_RULES;
import static uk.gov.gchq.palisade.service.policy.ApplicationTestData.AUDITABLE_POLICY_RESOURCE_RULES_NULL;
import static uk.gov.gchq.palisade.service.policy.ApplicationTestData.AUDIT_ERROR_MESSAGE;
import static uk.gov.gchq.palisade.service.policy.ApplicationTestData.REQUEST;
import static uk.gov.gchq.palisade.service.policy.ApplicationTestData.RESOURCE_RULES;
import static uk.gov.gchq.palisade.service.policy.ApplicationTestData.RESPONSE_NO_RULES;
import static uk.gov.gchq.palisade.service.policy.ApplicationTestData.RULES;

class PolicyServiceAsyncProxyTest {

    ExecutorService executorService;
    PolicyServiceHierarchyProxy hierarchyProxy;
    PolicyServiceAsyncProxy asyncProxy;

    @BeforeEach
    void startUp() {
        executorService = Executors.newSingleThreadExecutor();
        hierarchyProxy = Mockito.mock(PolicyServiceHierarchyProxy.class);
        asyncProxy = new PolicyServiceAsyncProxy(hierarchyProxy, executorService);
    }

    /**
     * Test with a PolicyRequest that is populated, and the query finds related rules.
     * The expected results should be a container {@link AuditablePolicyResourceResponse} with a {@code PolicyRequest},
     * {@code Rules} and no {@code AuditErrorMessage}.
     */
    @Test
    void testGetResourceRulesWithAPolicyRequest() {
        // When
        when(hierarchyProxy.getResourceRules(any())).thenReturn(RESOURCE_RULES);
        var response = asyncProxy.getResourceRules(REQUEST).join();

        // Then
        verify(hierarchyProxy, times(1)).getResourceRules(any());
        assertThat(response)
                .as("Recursively check the returned AuditablePolicyResourceRules object")
                .usingRecursiveComparison()
                .isEqualTo(AUDITABLE_POLICY_RESOURCE_RULES_NO_ERROR);
    }

    /**
     * Test for when there is no PolicyRequest in the message. This can occur when the message is either a start or
     * end marker. These conditions should preempt the call to the
     * {@link PolicyServiceHierarchyProxy#getResourceRules(LeafResource)}.
     * The expected results should be the container {@code AuditablePolicyResourceResponse} without a
     * {@code PolicyRequest}, {@code Rules} nor a {@code AuditErrorMessage}.
     */
    @Test
    void testGetResourceRulesWithANullPolicyRequest() {

        // When
        var response = asyncProxy.getResourceRules(null).join();

        // Then
        verify(hierarchyProxy, times(0)).getResourceRules(any());
        assertThat(response)
                .as("Recursively check the returned AuditablePolicyResourceRules object")
                .usingRecursiveComparison()
                .isEqualTo(AUDITABLE_POLICY_RESOURCE_RULES_NULL);
    }


    /**
     * Test for when there Rules aren't found for the resource. This should produce a container
     * {@link AuditablePolicyResourceResponse} with a {@link AuditErrorMessage} holding the exception that was thrown.
     */
    @Test
    void testGetResourceRulesWhenItThrowsAnException() {
        // When
        when(hierarchyProxy.getResourceRules(any())).thenThrow(new NoSuchPolicyException("No rules found for the resource"));
        var response = asyncProxy.getResourceRules(REQUEST).join();

        // Then
        assertThat(response)
                .as("Recursively check the returned AuditablePolicyResourceRules object")
                .usingRecursiveComparison()
                .ignoringFieldsOfTypes(AuditErrorMessage.class)
                .isEqualTo(AUDITABLE_POLICY_RESOURCE_RULES_NO_RULES);

        assertThat(response.getAuditErrorMessage())
                .as("Recursively check the AuditErrorMessage object, ignoring the timestamp field")
                .usingRecursiveComparison()
                .ignoringFields("timestamp")
                .isEqualTo(AUDIT_ERROR_MESSAGE);

        assertThat(response.getAuditErrorMessage().getError())
                .extracting(Throwable::getCause)
                .as("The exception cause should be 'NoSuchPolicyException'")
                .isInstanceOf(NoSuchPolicyException.class)
                .as("The exception should contain the message 'No rules found for the resource'")
                .extracting(Throwable::getMessage)
                .isEqualTo("No rules found for the resource");
    }


    /**
     * Test for when Rules are found for the record. This should produce an {@link AuditablePolicyRecordResponse}
     * with an {@link PolicyResponse} and no {@link AuditErrorMessage}.
     */
    @Test
    void testGetRecordRulesWhichFindsRules() {
        // When
        when(hierarchyProxy.getRecordRules(any())).thenReturn(RULES);
        var response = asyncProxy.getRecordRules(AUDITABLE_POLICY_RESOURCE_RESPONSE).join();

        // Then
        assertThat(response)
                .as("Recursively check the returned AuditablePolicyRecordResponse object")
                .usingRecursiveComparison()
                .isEqualTo(AUDITABLE_POLICY_RECORD_RESPONSE_NO_ERROR);
    }

    /**
     * Test for when Rules are not found for the record. This should produce an {@link AuditablePolicyRecordResponse}
     * with a {@link PolicyResponse} that has an empty {@link Rules} set and with {@link AuditErrorMessage}.
     */
    @Test
    void testGetRecordRulesWithNoPolicyRecord() {
        // When
        when(hierarchyProxy.getRecordRules(any())).thenThrow(new NoSuchPolicyException("No rules found for the resource"));
        var response = asyncProxy.getRecordRules(AUDITABLE_POLICY_RESOURCE_RESPONSE_WITH_NO_RULES).join();
        var policyResponse = response.getPolicyResponse();

        // Then
        assertThat(policyResponse)
                .as("Recursively check the PolicyResponse objet")
                .usingRecursiveComparison()
                .ignoringFieldsOfTypes(AuditErrorMessage.class)
                .isEqualTo(RESPONSE_NO_RULES);

        assertThat(response.getAuditErrorMessage())
                .as("Recursively check the AuditErrorMessage object")
                .usingRecursiveComparison()
                .ignoringFields("timestamp")
                .isEqualTo(AUDIT_ERROR_MESSAGE);

        assertThat(response.getAuditErrorMessage().getError())
                .extracting(Throwable::getCause)
                .as("The exception cause should be 'NoSuchPolicyException'")
                .isInstanceOf(NoSuchPolicyException.class)
                .as("The exception should contain the message 'No rules found for the resource'")
                .extracting(Throwable::getMessage)
                .isEqualTo("No rules found for the resource");
    }
}
