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

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import uk.gov.gchq.palisade.service.policy.PolicyTestCommon;
import uk.gov.gchq.palisade.service.policy.common.resource.LeafResource;
import uk.gov.gchq.palisade.service.policy.common.resource.impl.FileResource;
import uk.gov.gchq.palisade.service.policy.common.rule.Rules;
import uk.gov.gchq.palisade.service.policy.common.user.User;
import uk.gov.gchq.palisade.service.policy.exception.NoSuchPolicyException;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PolicyServiceHierarchyProxyTest extends PolicyTestCommon {

    private static final PolicyServiceCachingProxy MOCK_SERVICE = Mockito.spy(new PolicyServiceCachingProxy(new NullPolicyService()));
    private static final PolicyServiceHierarchyProxy HIERARCHY_POLICY = new PolicyServiceHierarchyProxy(MOCK_SERVICE);

    @BeforeAll
    static void setupClass() {
        // Add the system resource to the policy service
        HIERARCHY_POLICY.setResourceRules(TXT_SYSTEM, TXT_POLICY);

        // Add the directory resources to the policy service
        HIERARCHY_POLICY.setResourceRules(JSON_DIRECTORY, JSON_POLICY);
        HIERARCHY_POLICY.setResourceRules(SENSITIVE_DIRECTORY, SENSITIVE_POLICY);
        HIERARCHY_POLICY.setResourceRules(SECRET_DIRECTORY, SECRET_POLICY);

        // Add the file resources to the policy service
        for (FileResource fileResource : Arrays.asList(ACCESSIBLE_JSON_TXT_FILE, INACCESSIBLE_JSON_AVRO_FILE, INACCESSIBLE_PICKLE_TXT_FILE, SENSITIVE_TXT_FILE, SENSITIVE_CSV_FILE, SECRET_TXT_FILE)) {
            HIERARCHY_POLICY.setResourceRules(fileResource, PASS_THROUGH_POLICY);
        }
    }

    @AfterAll
    static void tearDown() {
        Mockito.reset(MOCK_SERVICE);
    }

    @Test
    void testGetRecordLevelRules() {
        // Given - there are record-level rules for the requested resource
        // SECRET_DIRECTORY and (by hierarchy) secretFile

        // When - a record-level policy is requested on a resource
        Mockito.doReturn(Optional.of(PASS_THROUGH_POLICY)).when(MOCK_SERVICE).getRecordRules(SENSITIVE_TXT_FILE.getId());
        Mockito.doReturn(Optional.of(PASS_THROUGH_POLICY)).when(MOCK_SERVICE).getRecordRules(SECRET_TXT_FILE.getId());

        Rules<?> secretDirRules = HIERARCHY_POLICY.getRecordRules(SENSITIVE_TXT_FILE);

        // Then - the record-level rules are returned
        assertThat(secretDirRules)
                .as("The returned rules should not be null")
                .isNotNull()
                .as("Recursively check the returned rules")
                .usingRecursiveComparison()
                .isEqualTo(PASS_THROUGH_POLICY);

        // When - a record-level policy is requested on a resource
        Mockito.doReturn(Optional.of(PASS_THROUGH_POLICY)).when(MOCK_SERVICE).getRecordRules(SECRET_TXT_FILE.getId());
        Rules<?> secretFileRules = HIERARCHY_POLICY.getRecordRules(SECRET_TXT_FILE);

        // Then - the record-level rules are returned (and include all those of the parent directory)
        assertThat(secretFileRules)
                .as("The returned rules should not be null")
                .isNotNull()
                .as("Recursively check the returned rules")
                .usingRecursiveComparison()
                .isEqualTo(PASS_THROUGH_POLICY);
    }

    @Test
    void testShouldReturnNoRulesWhenNotSet() {
        // Given - there are no policies for the requested resource
        // NEW_FILE

        //When - a policy is requested on a resource
        Exception noSuchPolicy = assertThrows(NoSuchPolicyException.class, () -> HIERARCHY_POLICY.getRecordRules(NEW_FILE), "should throw NoSuchPolicyException");

        //Then an error is thrown
        assertThat(noSuchPolicy.getMessage())
                .as("The message of the error should be %s", "No Resource Rules found for the resource")
                .isEqualTo("No Record Rules found for the resource: " + NEW_FILE.getId());
    }

    @Test
    void testCanAccessResources() {
        // Given - there are accessible resources
        // ACCESSIBLE_JSON_TXT_FILE for user, SENSITIVE_TXT_FILE for sensitiveUser, SECRET_TXT_FILE for secretUser
        HIERARCHY_POLICY.setResourceRules(ACCESSIBLE_JSON_TXT_FILE, PASS_THROUGH_POLICY);

        for (User accessingUser : Arrays.asList(USER, SENSITIVE_USER, SECRET_USER)) {
            // When - access to the resource is queried
            Mockito.doReturn(Optional.of(PASS_THROUGH_POLICY)).when(MOCK_SERVICE).getResourceRules(ACCESSIBLE_JSON_TXT_FILE.getId());

            Rules<LeafResource> rules = HIERARCHY_POLICY.getResourceRules(ACCESSIBLE_JSON_TXT_FILE);
            LeafResource resource = PolicyServiceHierarchyProxy.applyRulesToResource(accessingUser, ACCESSIBLE_JSON_TXT_FILE, CONTEXT, rules);
            // Then - the resource is accessible
            assertThat(resource)
                    .as("The returned resource should not be null")
                    .isNotNull();
        }


        for (User accessingUser : Arrays.asList(SENSITIVE_USER, SECRET_USER)) {
            // When - access to the resource is queried
            Mockito.doReturn(Optional.of(PASS_THROUGH_POLICY)).when(MOCK_SERVICE).getResourceRules(SENSITIVE_TXT_FILE.getId());

            Rules<LeafResource> rules = HIERARCHY_POLICY.getResourceRules(SENSITIVE_TXT_FILE);
            LeafResource resource = PolicyServiceHierarchyProxy.applyRulesToResource(accessingUser, SENSITIVE_TXT_FILE, CONTEXT, rules);
            // Then - the resource is accessible
            assertThat(resource)
                    .as("The returned resource should not be null")
                    .isNotNull();
        }

        for (User accessingUser : Collections.singletonList(SECRET_USER)) {
            // When - access to the resource is queried
            Mockito.doReturn(Optional.of(PASS_THROUGH_POLICY)).when(MOCK_SERVICE).getResourceRules(SECRET_TXT_FILE.getId());

            Rules<LeafResource> rules = HIERARCHY_POLICY.getResourceRules(SECRET_TXT_FILE);
            LeafResource resource = PolicyServiceHierarchyProxy.applyRulesToResource(accessingUser, SECRET_TXT_FILE, CONTEXT, rules);
            // Then - the resource is accessible
            assertThat(resource)
                    .as("The returned resource should not be null")
                    .isNotNull();

        }
    }

    @Test
    void testCannotAccessRedactedResources() {
        HashSet<FileResource> files = new HashSet<>(Arrays.asList(ACCESSIBLE_JSON_TXT_FILE, INACCESSIBLE_JSON_AVRO_FILE, INACCESSIBLE_PICKLE_TXT_FILE, SENSITIVE_TXT_FILE, SENSITIVE_CSV_FILE, SECRET_TXT_FILE));

        // Given - there are inaccessible resources
        // everything but ACCESSIBLE_JSON_TXT_FILE for user
        for (FileResource fileResource : files) {
            // When - access to the resource is queried
            Mockito.doReturn(Optional.of(PASS_THROUGH_POLICY)).when(MOCK_SERVICE).getResourceRules(fileResource.getId());

            LeafResource resource = PolicyServiceHierarchyProxy.applyRulesToResource(USER, fileResource, CONTEXT, HIERARCHY_POLICY.getResourceRules(fileResource));
            // Then - the resource is not accessible
            assertThat(resource)
                    .as("The returned resource should not be null")
                    .isNotNull()
                    .as("Recursively check the returned resource")
                    .usingRecursiveComparison()
                    .isEqualTo(fileResource);
        }

        // Given - there are inaccessible resources
        // everything but (accessible/sensitive)TxtFile for sensitiveUser
        files.remove(ACCESSIBLE_JSON_TXT_FILE);
        files.remove(SENSITIVE_TXT_FILE);
        for (FileResource fileResource : files) {
            // When - access to the resource is queried
            Mockito.doReturn(Optional.of(SECRET_POLICY)).when(MOCK_SERVICE).getResourceRules(fileResource.getId());

            Rules<LeafResource> rules = HIERARCHY_POLICY.getResourceRules(fileResource);
            LeafResource resource = PolicyServiceHierarchyProxy.applyRulesToResource(SENSITIVE_USER, fileResource, CONTEXT, rules);
            // Then - the resource is not accessible
            assertThat(resource)
                    .as("The returned resource should be null")
                    .isNull();
        }

        // Given - there are inaccessible resources
        // everything except (accessible/sensitive/secret)TxtFile for secretUser
        files.remove(SECRET_TXT_FILE);
        for (FileResource fileResource : files) {
            // When - access to the resource is queried
            Mockito.doReturn(Optional.of(SECRET_POLICY)).when(MOCK_SERVICE).getResourceRules(fileResource.getId());

            Rules<LeafResource> rules = HIERARCHY_POLICY.getResourceRules(fileResource);
            LeafResource resource = PolicyServiceHierarchyProxy.applyRulesToResource(SENSITIVE_USER, fileResource, CONTEXT, rules);
            // Then - the resource is not accessible
            assertThat(resource)
                    .as("The returned resource should be null")
                    .isNull();
        }
    }
}
