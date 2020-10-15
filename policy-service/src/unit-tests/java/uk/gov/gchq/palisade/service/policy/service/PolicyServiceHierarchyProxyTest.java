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
import uk.gov.gchq.palisade.resource.Resource;
import uk.gov.gchq.palisade.resource.impl.FileResource;
import uk.gov.gchq.palisade.rule.Rule;
import uk.gov.gchq.palisade.rule.Rules;
import uk.gov.gchq.palisade.service.policy.PolicyTestCommon;
import uk.gov.gchq.palisade.service.request.Policy;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class PolicyServiceHierarchyProxyTest extends PolicyTestCommon {

    @BeforeAll
    static void setupClass() {
        // Add the system resource to the policy service
        assertThat(HIERARCHY_POLICY.setResourcePolicy(TXT_SYSTEM, TXT_POLICY)).isEqualTo(TXT_POLICY);

        // Add the directory resources to the policy service
        assertThat(HIERARCHY_POLICY.setResourcePolicy(JSON_DIRECTORY, JSON_POLICY)).isEqualTo(JSON_POLICY);
        assertThat(HIERARCHY_POLICY.setResourcePolicy(SENSITIVE_DIRECTORY, SENSITIVE_POLICY)).isEqualTo(SENSITIVE_POLICY);
        assertThat(HIERARCHY_POLICY.setResourcePolicy(SECRET_DIRECTORY, SECRET_POLICY)).isEqualTo(SECRET_POLICY);

        // Add the file resources to the policy service
        for (FileResource fileResource : Arrays.asList(ACCESSIBLE_JSON_TXT_FILE, INACCESSIBLE_JSON_AVRO_FILE, INACCESSIBLE_PICKLE_TXT_FILE, SENSITIVE_TXT_FILE, SENSITIVE_CSV_FILE, SECRET_TXT_FILE)) {
            assertThat(HIERARCHY_POLICY.setResourcePolicy(fileResource, PASS_THROUGH_POLICY)).isEqualTo(PASS_THROUGH_POLICY);
        }
    }

    @Test
    void testGetRecordLevelRules() {
        // Given - there are record-level rules for the requested resource
        // SECRET_DIRECTORY and (by hierarchy) secretFile

        // When - a record-level policy is requested on a resource
        Optional<Policy> secretDirPolicies = HIERARCHY_POLICY.getPolicy(SECRET_DIRECTORY);
        Optional<Map<String, Rule<Serializable>>> secretDirRules = secretDirPolicies.map(Policy::getRecordRules).map(Rules::getRules);

        // Then - the record-level rules are returned
        assertThat(secretDirRules).isNotEmpty();
        assertThat(secretDirRules.get()).isNotEmpty();

        // When - a record-level policy is requested on a resource
        Optional<Policy> secretFilePolicies = HIERARCHY_POLICY.getPolicy(SECRET_TXT_FILE);
        Optional<Map<String, Rule<Serializable>>> secretFileRules = secretFilePolicies.map(Policy::getRecordRules).map(Rules::getRules);

        // Then - the record-level rules are returned (and include all those of the parent directory)
        assertThat(secretFileRules).isNotNull().isPresent();
        assertThat(secretFileRules.get()).isNotEmpty();
    }

    @Test
    void testShouldReturnNoPolicyWhenNotSet() {
        // Given - there are no policies for the requested resource
        // NEW_FILE

        // When - a policy is requested on a resource
        Optional<Policy> policy = HIERARCHY_POLICY.getPolicy(NEW_FILE);

        // Then - no such policy was retrieved
        assertThat(policy).isEmpty();
    }

    @Test
    void testCanAccessResources() {
        // Given - there are accessible resources
        // ACCESSIBLE_JSON_TXT_FILE for user, SENSITIVE_TXT_FILE for sensitiveUser, SECRET_TXT_FILE for secretUser

        // When - access to the resource is queried
        for (User accessingUser : Arrays.asList(USER, SENSITIVE_USER, SECRET_USER)) {
            Optional<Resource> resource = HIERARCHY_POLICY.canAccess(accessingUser, CONTEXT, ACCESSIBLE_JSON_TXT_FILE);
            // Then - the resource is accessible
            assertThat(resource).isNotEmpty();
        }

        // When - access to the resource is queried
        for (User accessingUser : Arrays.asList(SENSITIVE_USER, SECRET_USER)) {
            Optional<Resource> resource = HIERARCHY_POLICY.canAccess(accessingUser, CONTEXT, SENSITIVE_TXT_FILE);
            // Then - the resource is accessible
            assertThat(resource).isNotEmpty();
        }

        for (User accessingUser : Collections.singletonList(SECRET_USER)) {
            // When - access to the resource is queried
            Optional<Resource> resource = HIERARCHY_POLICY.canAccess(accessingUser, CONTEXT, SECRET_TXT_FILE);
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
            Optional<Resource> resource = HIERARCHY_POLICY.canAccess(USER, CONTEXT, fileResource);
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
            Optional<Resource> resource = HIERARCHY_POLICY.canAccess(SENSITIVE_USER, CONTEXT, fileResource);
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
            Optional<Resource> resource = HIERARCHY_POLICY.canAccess(SENSITIVE_USER, CONTEXT, fileResource);
            // Then - the resource is not accessible
            assertThat(resource).isEmpty();
        }
    }

    @Test
    void testCannotAccessResourceWithoutPolicies() {
        // Given - there are resources with no policies
        // NEW_FILE

        // When - access to the resource is queried
        Optional<Resource> resource = HIERARCHY_POLICY.canAccess(USER, CONTEXT, NEW_FILE);

        // Then - the resource is not accessible
        assertThat(resource).isEmpty();
    }
}
