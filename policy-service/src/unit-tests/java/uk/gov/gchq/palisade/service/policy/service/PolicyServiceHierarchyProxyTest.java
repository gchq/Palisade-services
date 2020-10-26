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
package uk.gov.gchq.palisade.service.policy.service;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import uk.gov.gchq.palisade.User;
import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.resource.impl.FileResource;
import uk.gov.gchq.palisade.rule.Rules;
import uk.gov.gchq.palisade.service.policy.PolicyTestCommon;
import uk.gov.gchq.palisade.service.policy.exception.NoSuchPolicyException;
import uk.gov.gchq.palisade.service.request.Policy;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PolicyServiceHierarchyProxyTest extends PolicyTestCommon {

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

    @Test
    void testGetRecordLevelRules() {
        // Given - there are record-level rules for the requested resource
        // SECRET_DIRECTORY and (by hierarchy) secretFile

        // When - a record-level policy is requested on a resource
        Optional<Rules<?>> secretDirPolicies = HIERARCHY_POLICY.getRecordRules(SECRET_DIRECTORY);
        Optional<?> secretDirRules = secretDirPolicies.map(Policy::getRecordRules).map(Rules::getRules);

        // Then - the record-level rules are returned
        assertThat(secretDirRules).isNotNull();
        assertThat(secretDirRules).isNotEmpty();

        // When - a record-level policy is requested on a resource
        Optional<Rules<?>> secretFilePolicies = Optional.ofNullable(HIERARCHY_POLICY.getRecordRules(SECRET_TXT_FILE));
        Optional<?> secretFileRules = secretFilePolicies.map(Policy::getRecordRules).map(Rules::getRules);

        // Then - the record-level rules are returned (and include all those of the parent directory)
        assertThat(secretFileRules).isNotNull();
        assertThat(secretFileRules).isNotEmpty();
    }

    @Test
    void testShouldReturnNoPolicyWhenNotSet() {
        // Given - there are no policies for the requested resource
        // NEW_FILE

        //When - a policy is requested on a resource
        Exception NoSuchPolicy = assertThrows(NoSuchPolicyException.class, () -> HIERARCHY_POLICY.getRecordRules(NEW_FILE), "should throw NoSuchPolicyException");

        //Then an error is thrown
        assertThat("No Policy Found").isEqualTo(NoSuchPolicy.getMessage());
    }

    @Test
    void testCanAccessResources() {
        // Given - there are accessible resources
        // ACCESSIBLE_JSON_TXT_FILE for user, SENSITIVE_TXT_FILE for sensitiveUser, SECRET_TXT_FILE for secretUser

        // When - access to the resource is queried
        for (User accessingUser : Arrays.asList(USER, SENSITIVE_USER, SECRET_USER)) {
            Optional<FileResource> resource = HIERARCHY_POLICY.getResourceRules(ACCESSIBLE_JSON_TXT_FILE);
            // Then - the resource is accessible
            assertThat(resource).isNotEmpty();
        }

        // When - access to the resource is queried
        for (User accessingUser : Arrays.asList(SENSITIVE_USER, SECRET_USER)) {
            Optional<FileResource> resource = HIERARCHY_POLICY.getResourceRules(SENSITIVE_TXT_FILE);
            // Then - the resource is accessible
            assertThat(resource).isNotEmpty();
        }

        for (User accessingUser : Collections.singletonList(SECRET_USER)) {
            // When - access to the resource is queried
            Optional<FileResource> resource = HIERARCHY_POLICY.getResourceRules(SECRET_TXT_FILE);
            // Then - the resource is accessible
            assertThat(resource).isNotEmpty();
        }
    }

    @Test
    void testCannotAccessRedactedResources() {
        HashSet<FileResource> files = new HashSet<>(Arrays.asList(ACCESSIBLE_JSON_TXT_FILE, INACCESSIBLE_JSON_AVRO_FILE, INACCESSIBLE_PICKLE_TXT_FILE, SENSITIVE_TXT_FILE, SENSITIVE_CSV_FILE, SECRET_TXT_FILE));

        // Given - there are inaccessible resources
        // everything but ACCESSIBLE_JSON_TXT_FILE for user
        HashSet<FileResource> resources = new HashSet<>(files);
        resources.remove(ACCESSIBLE_JSON_TXT_FILE);
        for (FileResource fileResource : resources) {
            // When - access to the resource is queried
            Optional<FileResource> resource = HIERARCHY_POLICY.getResourceRules(fileResource);
            // Then - the resource is not accessible
            assertThat(resource).isEmpty();
        }

        // Given - there are inaccessible resources
        // everything but (accessible/sensitive)TxtFile for sensitiveUser
        resources = new HashSet<>(files);
        resources.remove(ACCESSIBLE_JSON_TXT_FILE);
        resources.remove(SENSITIVE_TXT_FILE);
        for (FileResource fileResource : resources) {
            // When - access to the resource is queried
            Optional<FileResource> resource = HIERARCHY_POLICY.getResourceRules(fileResource);
            // Then - the resource is not accessible
            assertThat(resource).isEmpty();
        }

        // Given - there are inaccessible resources
        // everything except (accessible/sensitive/secret)TxtFile for secretUser
        resources = new HashSet<>(files);
        resources.remove(ACCESSIBLE_JSON_TXT_FILE);
        resources.remove(SENSITIVE_TXT_FILE);
        resources.remove(SECRET_TXT_FILE);
        for (FileResource fileResource : resources) {
            // When - access to the resource is queried
            Optional<FileResource> resource = HIERARCHY_POLICY.getResourceRules(fileResource);
            // Then - the resource is not accessible
            assertThat(resource).isEmpty();
        }
    }

    @Test
    void testCannotAccessResourceWithoutPolicies() {
        // Given - there are resources with no policies
        // NEW_FILE

        // When - access to the resource is queried
        Rules<LeafResource> rules = HIERARCHY_POLICY.getResourceRules(NEW_FILE);
        LeafResource resource = HIERARCHY_POLICY.applyRulesToResource(USER, NEW_FILE, CONTEXT, rules);

        // Then - the resource is not accessible
        assertThat(resource).isNull();
    }
}
