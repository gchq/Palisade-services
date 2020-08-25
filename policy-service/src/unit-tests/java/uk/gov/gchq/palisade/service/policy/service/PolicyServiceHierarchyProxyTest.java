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
import org.springframework.boot.test.context.SpringBootTest;

import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.User;
import uk.gov.gchq.palisade.policy.HasSensitiveAuthRule;
import uk.gov.gchq.palisade.policy.IsTextResourceRule;
import uk.gov.gchq.palisade.policy.PassThroughRule;
import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.resource.Resource;
import uk.gov.gchq.palisade.resource.impl.DirectoryResource;
import uk.gov.gchq.palisade.resource.impl.FileResource;
import uk.gov.gchq.palisade.resource.impl.SystemResource;
import uk.gov.gchq.palisade.rule.PredicateRule;
import uk.gov.gchq.palisade.rule.Rule;
import uk.gov.gchq.palisade.rule.Rules;
import uk.gov.gchq.palisade.service.request.Policy;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class PolicyServiceHierarchyProxyTest {
    private static final User USER = new User().userId("testUser");
    private static final User SENSITIVE_USER = new User().userId("sensitiveTestUser").addAuths(Collections.singleton("Sensitive"));
    private static final User SECRET_USER = new User().userId("secretTestUser").addAuths(new HashSet<>(Arrays.asList("Sensitive", "Secret")));
    private static final Context CONTEXT = new Context().purpose("Testing");

    /**
     * Setup a collection of resources with policies like so:
     * /txt - only txt type files are viewable
     * /txt/json - only json format files are viewable
     * /txt/json/json.txt - an accessible json txt file
     * /txt/json/json.avro - an inaccessible json avro file (breaks /txt rule)
     * /txt/json/pickled.txt - an inaccessible pickle txt file (breaks /txt/json rule)
     * /txt/sensitive - only users with sensitive auth can view
     * /txt/sensitive/report.txt - an accessible (to sensitive auths) txt file
     * /txt/sensitive/salary.csv - an inaccessible csv file (breaks /txt rule)
     * /txt/secret - only users with secret auth can view, a purpose of testing will redact all record-level info
     * /txt/secret/secrets.txt - an accessible (to secret auths) txt file
     * /new - a directory to be added with a pass-thru policy (do nothing)
     * /new/file.exe - an accessible executable (not under /txt policy)
     **/

    // A system that only allows text files to be seen
    private static final SystemResource TXT_SYSTEM = new SystemResource().id("/txt");
    private static final Policy TXT_POLICY = new Policy<>()
            .owner(USER)
            .resourceLevelRule("Resource serialised format is txt", new IsTextResourceRule());

    // A directory that only allows JSON types
    private static final DirectoryResource JSON_DIRECTORY = new DirectoryResource().id("/txt/json").parent(TXT_SYSTEM);
    private static final Policy JSON_POLICY = new Policy<>()
            .owner(USER)
            .resourceLevelRule("Resource type is json", (PredicateRule<Resource>) (resource, user, context) -> resource instanceof LeafResource && ((LeafResource) resource).getType().equals("json"));

    // A text file containing json data - this should be accessible
    private static final FileResource ACCESSIBLE_JSON_TXT_FILE = new FileResource().id("/txt/json/json.txt").serialisedFormat("txt").type("json").parent(JSON_DIRECTORY);
    // An avro file containing json data - this should be inaccessible due to the system policy
    private static final FileResource INACCESSIBLE_JSON_AVRO_FILE = new FileResource().id("/txt/json/json.avro").serialisedFormat("avro").type("json").parent(JSON_DIRECTORY);
    // A text file containing pickle data - this should be inaccessible due to the directory policy
    private static final FileResource INACCESSIBLE_PICKLE_TXT_FILE = new FileResource().id("/txt/json/pickled.txt").serialisedFormat("txt").type("pickle").parent(JSON_DIRECTORY);

    // A sensitive directory that only allows sensitive authorised users
    private static final DirectoryResource SENSITIVE_DIRECTORY = new DirectoryResource().id("/txt/sensitive").parent(TXT_SYSTEM);
    private static final Policy SENSITIVE_POLICY = new Policy<>()
            .owner(SENSITIVE_USER)
            .resourceLevelRule("Check user has 'Sensitive' auth", new HasSensitiveAuthRule<>());

    // A sensitive text file containing a report of salary information - this is accessible to authorised users only
    private static final FileResource SENSITIVE_TXT_FILE = new FileResource().id("/txt/sensitive/report.txt").serialisedFormat("txt").type("txt").parent(SENSITIVE_DIRECTORY);
    // A sensitive CSV of salary information - this should be inaccessible due to the system policy
    private static final FileResource SENSITIVE_CSV_FILE = new FileResource().id("/txt/sensitive/salary.csv").serialisedFormat("csv").type("txt").parent(SENSITIVE_DIRECTORY);

    // A secret directory that allows only secret authorised users
    private static final DirectoryResource SECRET_DIRECTORY = new DirectoryResource().id("/txt/secret").parent(TXT_SYSTEM);
    private static final Policy SECRET_POLICY = new Policy<>()
            .owner(SENSITIVE_USER)
            .resourceLevelRule("Check user has 'Secret' auth", (PredicateRule<Resource>) (resource, user, context) -> user.getAuths().contains("Secret"))
            .recordLevelPredicateRule("Redact all with 'Testing' purpose", (record, user, context) -> context.getPurpose().equals("Testing"));

    // A secret file - accessible only to the secret user
    private static final FileResource SECRET_TXT_FILE = new FileResource().id("/txt/secret/secrets.txt").serialisedFormat("txt").type("txt").parent(SECRET_DIRECTORY);

    private static final FileResource NEW_FILE = new FileResource().id("/new/file.exe").serialisedFormat("exe").type("elf").parent(new SystemResource().id("/new"));

    // A do-nothing policy to apply to leaf resources
    private static final Policy PASS_THROUGH_POLICY = new Policy<>()
            .owner(USER)
            .resourceLevelRule("Does nothing", new PassThroughRule<>())
            .recordLevelRule("Does nothing", new PassThroughRule<>());

    private static final PolicyService SERVICE = new SimplePolicyService();
    private static final PolicyServiceHierarchyProxy HIERARCHY_POLICY = new PolicyServiceHierarchyProxy(SERVICE);

    @BeforeAll
    public static void setupClass() {
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
    public void testGetRecordLevelRules() {
        // Given - there are record-level rules for the requested resource
        // SECRET_DIRECTORY and (by hierarchy) secretFile

        // When - a record-level policy is requested on a resource
        Optional<Policy> secretDirPolicies = HIERARCHY_POLICY.getPolicy(SECRET_DIRECTORY);
        Optional<Map<String, Rule<Object>>> secretDirRules = secretDirPolicies.map(Policy::getRecordRules).map(Rules::getRules);

        // Then - the record-level rules are returned
        assertThat(secretDirRules).isNotEmpty();
        assertThat(secretDirRules.get()).isNotEmpty();

        // When - a record-level policy is requested on a resource
        Optional<Policy> secretFilePolicies = HIERARCHY_POLICY.getPolicy(SECRET_TXT_FILE);
        Optional<Map<String, Rule<Object>>> secretFileRules = secretFilePolicies.map(Policy::getRecordRules).map(Rules::getRules);

        // Then - the record-level rules are returned (and include all those of the parent directory)
        assertThat(secretFileRules).isNotNull();
        assertThat(secretFileRules.get()).isNotEmpty();
    }

    @Test
    public void testShouldReturnNoPolicyWhenNotSet() {
        // Given - there are no policies for the requested resource
        // NEW_FILE

        // When - a policy is requested on a resource
        Optional<Policy> policy = HIERARCHY_POLICY.getPolicy(NEW_FILE);

        // Then - no such policy was retrieved
        assertThat(policy).isEmpty();
    }

    @Test
    public void testCanAccessResources() {
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
            Optional<Resource> resource = HIERARCHY_POLICY.canAccess(SENSITIVE_USER, CONTEXT, SENSITIVE_TXT_FILE);
            // Then - the resource is accessible
            assertThat(resource).isNotEmpty();
        }

        for (User accessingUser : Arrays.asList(SENSITIVE_USER)) {
            // When - access to the resource is queried
            Optional<Resource> resource = HIERARCHY_POLICY.canAccess(SECRET_USER, CONTEXT, SECRET_TXT_FILE);
            // Then - the resource is accessible
            assertThat(resource).isNotEmpty();
        }
    }

    @Test
    public void testCannotAccessRedactedResources() {
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
    public void testCannotAccessResourceWithoutPolicies() {
        // Given - there are resources with no policies
        // NEW_FILE

        // When - access to the resource is queried
        Optional<Resource> resource = HIERARCHY_POLICY.canAccess(USER, CONTEXT, NEW_FILE);

        // Then - the resource is not accessible
        assertThat(resource).isEmpty();
    }
}
