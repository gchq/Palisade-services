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
import uk.gov.gchq.palisade.service.policy.exception.NoSuchPolicyException;
import uk.gov.gchq.palisade.service.policy.model.AuditablePolicyRecordResponse;
import uk.gov.gchq.palisade.service.policy.model.AuditablePolicyResourceResponse;
import uk.gov.gchq.palisade.service.policy.model.AuditablePolicyResourceRules;
import uk.gov.gchq.palisade.service.policy.model.PolicyResponse;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.gchq.palisade.service.policy.ApplicationTestData.AUDITABLE_POLICY_RESOURCE_RESPONSE;
import static uk.gov.gchq.palisade.service.policy.ApplicationTestData.AUDITABLE_POLICY_RESOURCE_RESPONSE_WITH_NO_RULES;
import static uk.gov.gchq.palisade.service.policy.ApplicationTestData.REQUEST;
import static uk.gov.gchq.palisade.service.policy.ApplicationTestData.RESOURCE_RULES;
import static uk.gov.gchq.palisade.service.policy.ApplicationTestData.RULES;

/**
 * Set of unit tests for  {@code PolicyServiceAsyncProxy}.
 */
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
     * Test with a PolicyRequest that is populated and the query finds related rules.
     * The expected results should be a container {@link AuditablePolicyResourceResponse} with a {@code PolicyRequest},
     * {@code Rules} and no {@code AuditErrorMessage}.
     *
     * @throws Exception if the test fails
     */
    @Test
    void testGetResourceRulesWithAPolicyRequest() throws Exception {
        // When
        when(hierarchyProxy.getResourceRules(any())).thenReturn(RESOURCE_RULES);
        AuditablePolicyResourceRules response = asyncProxy.getResourceRules(REQUEST).get();

        // Then
        verify(hierarchyProxy, times(1)).getResourceRules(any());
        assertAll("Check the values of the returned AuditablePolicyResourceRules object",
                () -> assertThat(response.getPolicyRequest())
                        .as("Check the PolicyRequest value is not null")
                        .isNotNull(),
                () -> assertThat(response.getRules())
                        .as("Check the Rules value is not null")
                        .isNotNull(),
                () -> assertThat(response.getAuditErrorMessage())
                        .as("Check the AuditErrorMessage value is null")
                        .isNull()
        );
    }

    /**
     * Test for when there is no PolicyRequest in the message.  This can occur when the message is either a start or
     * end marker.  These conditions should preempt the call to the
     * {@link PolicyServiceHierarchyProxy#getResourceRules(LeafResource)}.
     * The expected results should be the container {@code AuditablePolicyResourceResponse} without a
     * {@code PolicyRequest}, {@code Rules} nor a {@code AuditErrorMessage}.
     *
     * @throws Exception if the test fails
     */
    @Test
    void testGetResourceRulesWithANullPolicyRequest() throws Exception {

        // When
        AuditablePolicyResourceRules response = asyncProxy.getResourceRules(null).get();

        // Then
        verify(hierarchyProxy, times(0)).getResourceRules(any());
        assertAll("Check the values of the returned AuditablePolicyResourceRules object",
                () -> assertThat(response.getPolicyRequest())
                        .as("Check the PolicyRequest value is null")
                        .isNull(),
                () -> assertThat(response.getRules())
                        .as("Check the Rules value is null")
                        .isNull(),
                () -> assertThat(response.getAuditErrorMessage())
                        .as("Check the AuditErrorMessage value is null")
                        .isNull()
        );
    }


    /**
     * Test for when there is Rules are not found for the resource.  This should produce a container
     * {@link AuditablePolicyResourceResponse} with a {@code AuditErrorMessage} holding the exception that was thrown
     *
     * @throws Exception if the test fails
     */
    @Test
    void testGetResourceRulesWhenItThrowsAnException() throws Exception {
        // When
        when(hierarchyProxy.getResourceRules(any())).thenThrow(new NoSuchPolicyException("Test"));
        AuditablePolicyResourceRules response = asyncProxy.getResourceRules(REQUEST).get();

        // Then
        assertAll("Check the values of the returned AuditablePolicyResourceRules object",
                () -> assertThat(response.getPolicyRequest())
                        .as("Check the PolicyRequest value is not null")
                        .isNotNull(),
                () -> assertThat(response.getRules())
                        .as("Check the Rules value is null")
                        .isNull(),
                () -> assertThat(response.getAuditErrorMessage())
                        .as("Check the AuditErrorMessage value is not null")
                        .isNotNull(),
                () -> assertThat(response.getAuditErrorMessage().getError().getCause())
                        .as("Check the cause of the thrown error")
                        .isInstanceOf(NoSuchPolicyException.class)
        );
    }


    /**
     * Test for when Rules are found for the record.  This should produce an {@link AuditablePolicyRecordResponse}
     * with an {@code PolicyResponse} and no {@code AuditErrorMessage}
     *
     * @throws Exception if the test fails
     */
    @Test
    void testGetRecordRulesWhichFindsRules() throws Exception {
        // When
        when(hierarchyProxy.getRecordRules(any())).thenReturn(RULES);
        AuditablePolicyRecordResponse response = asyncProxy.getRecordRules(AUDITABLE_POLICY_RESOURCE_RESPONSE).get();

        // Then
        assertAll("Check the values of the returned AuditablePolicyRecordResponse object",
                () -> assertThat(response.getPolicyResponse())
                        .as("Check the PolicyRequest value is not null")
                        .isNotNull(),
                () -> assertThat(response.getAuditErrorMessage())
                        .as("Check the AuditErrorMessage value is null")
                        .isNull()
        );
    }

    /**
     * Test for when Rules are not found for the record. This should produce an {@link AuditablePolicyRecordResponse}
     * with a {@code PolicyResponse} that has an empty {@code Rules} set and with {@code AuditErrorMessage}.
     *
     * @throws Exception if the test fails
     */
    @Test
    void testGetRecordRulesWithNoPolicyRecord() throws Exception {
        // When
        when(hierarchyProxy.getRecordRules(any())).thenThrow(new NoSuchPolicyException("Test"));
        AuditablePolicyRecordResponse response = asyncProxy.getRecordRules(AUDITABLE_POLICY_RESOURCE_RESPONSE_WITH_NO_RULES).get();
        PolicyResponse policyResponse = response.getPolicyResponse();

        // Then
        assertAll(
                () -> assertThat(policyResponse)
                        .as("Check the PolicyRequest value is not null")
                        .isNotNull(),
                () -> assertThat(policyResponse.getRules())
                        .as("Check the PolicyRequest contains a Rules object")
                        .isNotNull(),
                () -> assertThat(policyResponse.getRules().containsRules())
                        .as("Check the Rules object does not contain any rules")
                        .isEqualTo(Boolean.FALSE),
                () -> assertThat(response.getAuditErrorMessage())
                        .as("Check the AuditErrorMessage value is null")
                        .isNotNull()
        );
    }
}
