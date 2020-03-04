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

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import uk.gov.gchq.palisade.service.policy.request.Policy;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.junit.Assert.assertTrue;

@RunWith(JUnit4.class)
public class PolicyServiceHierarchyProxyTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(PolicyServiceHierarchyProxyTest.class);

    private static final User user = new User().userId("testUser");
    private static final User sensitiveUser = new User().userId("sensitiveTestUser").addAuths(Collections.singleton("Sensitive"));
    private static final User secretUser = new User().userId("secretTestUser").addAuths(new HashSet<>(Arrays.asList("Sensitive", "Secret")));
    private static final Context context = new Context().purpose("Testing");

    /**
     * Setup a collection of resources with policies like so:
     * /txt - only txt type files are viewable
     *   /txt/json - only json format files are viewable
     *     /txt/json/json.txt - an accessible json txt file
     *     /txt/json/json.avro - an inaccessible json avro file (breaks /txt rule)
     *     /txt/json/pickled.txt - an inaccessible pickle txt file (breaks /txt/json rule)
     *   /txt/sensitive - only users with sensitive auth can view
     *     /txt/sensitive/report.txt - an accessible (to sensitive auths) txt file
     *     /txt/sensitive/salary.csv - an inaccessible csv file (breaks /txt rule)
     *   /txt/secret - only users with secret auth can view, a purpose of testing will redact all record-level info
     *     /txt/secret/secrets.txt - an accessible (to secret auths) txt file
     * /new - a directory to be added with a pass-thru policy (do nothing)
     *   /new/file.exe - an accessible executable (not under /txt policy)
     **/

    // A system that only allows text files to be seen
    private static final SystemResource txtSystem = new SystemResource().id("/txt");
    private static final Policy txtPolicy = new Policy<>()
            .owner(user)
            .resourceLevelRule("Resource serialised format is txt", new IsTextResourceRule());

    // A directory that only allows JSON types
    private static final DirectoryResource jsonDirectory = new DirectoryResource().id("/txt/json").parent(txtSystem);
    private static final Policy jsonPolicy = new Policy<>()
            .owner(user)
            .resourceLevelRule("Resource type is json", (PredicateRule<Resource>) (resource, user, context) -> resource instanceof LeafResource && ((LeafResource) resource).getType().equals("json"));

    // A text file containing json data - this should be accessible
    private static final FileResource accessibleJsonTxtFile = new FileResource().id("/txt/json/json.txt").serialisedFormat("txt").type("json").parent(jsonDirectory);
    // An avro file containing json data - this should be inaccessible due to the system policy
    private static final FileResource inaccessibleJsonAvroFile = new FileResource().id("/txt/json/json.avro").serialisedFormat("avro").type("json").parent(jsonDirectory);
    // A text file containing pickle data - this should be inaccessible due to the directory policy
    private static final FileResource inaccessiblePickleTxtFile = new FileResource().id("/txt/json/pickled.txt").serialisedFormat("txt").type("pickle").parent(jsonDirectory);

    // A sensitive directory that only allows sensitive authorised users
    private static final DirectoryResource sensitiveDirectory = new DirectoryResource().id("/txt/sensitive").parent(txtSystem);
    private static final Policy sensitivePolicy = new Policy<>()
            .owner(sensitiveUser)
            .resourceLevelRule("Check user has 'Sensitive' auth", new HasSensitiveAuthRule<>());

    // A sensitive text file containing a report of salary information - this is accessible to authorised users only
    private static final FileResource sensitiveTxtFile = new FileResource().id("/txt/sensitive/report.txt").serialisedFormat("txt").type("txt").parent(sensitiveDirectory);
    // A sensitive CSV of salary information - this should be inaccessible due to the system policy
    private static final FileResource sensitiveCsvFile = new FileResource().id("/txt/sensitive/salary.csv").serialisedFormat("csv").type("txt").parent(sensitiveDirectory);

    // A secret directory that allows only secret authorised users
    private static final DirectoryResource secretDirectory = new DirectoryResource().id("/txt/secret").parent(txtSystem);
    private static final Policy secretPolicy = new Policy<>()
            .owner(secretUser)
            .resourceLevelRule("Check user has 'Secret' auth", (PredicateRule<Resource>) (resource, user, context) -> user.getAuths().contains("Secret"))
            .recordLevelPredicateRule("Redact all with 'Testing' purpose", (record, user, context) -> context.getPurpose().equals("Testing"));

    // A secret file - accessible only to the secret user
    private static final FileResource secretTxtFile = new FileResource().id("/txt/secret/secrets.txt").serialisedFormat("txt").type("txt").parent(secretDirectory);

    private static final FileResource newFile = new FileResource().id("/new/file.exe").serialisedFormat("exe").type("elf").parent(new SystemResource().id("/new"));

    // A do-nothing policy to apply to leaf resources
    private static final Policy passThroughPolicy = new Policy<>()
            .owner(user)
            .resourceLevelRule("Does nothing", new PassThroughRule<>())
            .recordLevelRule("Does nothing", new PassThroughRule<>());

    private static final PolicyService service = new SimplePolicyService();
    private static final PolicyServiceHierarchyProxy hierarchyProxy = new PolicyServiceHierarchyProxy(service);

    @BeforeClass
    public static void setupClass() {
        // Add the system resource to the policy service
        assertThat(hierarchyProxy.setResourcePolicy(txtSystem, txtPolicy), equalTo(txtPolicy));

        // Add the directory resources to the policy service
        assertThat(hierarchyProxy.setResourcePolicy(jsonDirectory, jsonPolicy), equalTo(jsonPolicy));
        assertThat(hierarchyProxy.setResourcePolicy(sensitiveDirectory, sensitivePolicy), equalTo(sensitivePolicy));
        assertThat(hierarchyProxy.setResourcePolicy(secretDirectory, secretPolicy), equalTo(secretPolicy));

        // Add the file resources to the policy service
        for (FileResource fileResource : Arrays.asList(accessibleJsonTxtFile, inaccessibleJsonAvroFile, inaccessiblePickleTxtFile, sensitiveTxtFile, sensitiveCsvFile, secretTxtFile)) {
            assertThat(hierarchyProxy.setResourcePolicy(fileResource, passThroughPolicy), equalTo(passThroughPolicy));
        }
    }

    @Test
    public void getRecordLevelRules() {
        // Given - there are record-level rules for the requested resource
        // secretDirectory and (by hierarchy) secretFile

        // When - a record-level policy is requested on a resource
        Optional<Policy> secretDirPolicies = hierarchyProxy.getPolicy(secretDirectory);
        Optional<Map<String, Rule<Object>>> secretDirRules = secretDirPolicies.map(Policy::getRecordRules).map(Rules::getRules);

        // Then - the record-level rules are returned
        assertTrue(secretDirRules.isPresent());
        assertThat(secretDirRules.get(), not(Collections.emptyMap()));

        // When - a record-level policy is requested on a resource
        Optional<Policy> secretFilePolicies = hierarchyProxy.getPolicy(secretTxtFile);
        Optional<Map<String, Rule<Object>>> secretFileRules = secretFilePolicies.map(Policy::getRecordRules).map(Rules::getRules);

        // Then - the record-level rules are returned (and include all those of the parent directory)
        assertTrue(secretFileRules.isPresent());
        assertThat(secretFileRules.get(), not(Collections.emptyMap()));
        for (Entry<String, Rule<Object>> entry : secretDirRules.get().entrySet()) {
            assertThat(secretFileRules.get(), hasEntry(entry.getKey(), entry.getValue()));
        }
    }

    @Test
    public void shouldReturnNoPolicyWhenNotSet() {
        // Given - there are no policies for the requested resource
        // newFile

        // When - a policy is requested on a resource
        Optional<Policy> policy = hierarchyProxy.getPolicy(newFile);

        // Then - no such policy was retrieved
        assertTrue(policy.isEmpty());
    }

    @Test
    public void canAccessResources() {
        // Given - there are accessible resources
        // accessibleJsonTxtFile for user, sensitiveTxtFile for sensitiveUser, secretTxtFile for secretUser

        // When - access to the resource is queried
        for (User accessingUser : Arrays.asList(user, sensitiveUser, secretUser)) {
            Optional<Resource> resource = hierarchyProxy.canAccess(accessingUser, context, accessibleJsonTxtFile);
            // Then - the resource is accessible
            assertTrue(resource.isPresent());
        }

        // When - access to the resource is queried
        for (User accessingUser : Arrays.asList(sensitiveUser, secretUser)) {
            Optional<Resource> resource = hierarchyProxy.canAccess(sensitiveUser, context, sensitiveTxtFile);
            // Then - the resource is accessible
            assertTrue(resource.isPresent());
        }

        for (User accessingUser : Arrays.asList(secretUser)) {
            // When - access to the resource is queried
            Optional<Resource> resource = hierarchyProxy.canAccess(secretUser, context, secretTxtFile);
            // Then - the resource is accessible
            assertTrue(resource.isPresent());
        }
    }

    @Test
    public void cannotAccessRedactedResources() {
        HashSet<FileResource> files = new HashSet<>(Arrays.asList(accessibleJsonTxtFile, inaccessibleJsonAvroFile, inaccessiblePickleTxtFile, sensitiveTxtFile, sensitiveCsvFile, secretTxtFile));

        // Given - there are inaccessible resources
        // everything but accessibleJsonTxtFile for user
        HashSet<FileResource> resources = new HashSet<>(files);
        resources.remove(accessibleJsonTxtFile);
        for (FileResource fileResource : resources) {
            // When - access to the resource is queried
            Optional<Resource> resource = hierarchyProxy.canAccess(user, context, fileResource);
            // Then - the resource is not accessible
            assertTrue(resource.isEmpty());
        }

        // Given - there are inaccessible resources
        // everything but (accessible/sensitive)TxtFile for sensitiveUser
        resources = new HashSet<>(files);
        resources.remove(accessibleJsonTxtFile);
        resources.remove(sensitiveTxtFile);
        for (FileResource fileResource : resources) {
            // When - access to the resource is queried
            Optional<Resource> resource = hierarchyProxy.canAccess(sensitiveUser, context, fileResource);
            // Then - the resource is not accessible
            assertTrue(resource.isEmpty());
        }

        // Given - there are inaccessible resources
        // everything except (accessible/sensitive/secret)TxtFile for secretUser
        resources = new HashSet<>(files);
        resources.remove(accessibleJsonTxtFile);
        resources.remove(sensitiveTxtFile);
        resources.remove(secretTxtFile);
        for (FileResource fileResource : resources) {
            // When - access to the resource is queried
            Optional<Resource> resource = hierarchyProxy.canAccess(sensitiveUser, context, fileResource);
            // Then - the resource is not accessible
            assertTrue(resource.isEmpty());
        }
    }

    @Test
    public void cannotAccessResourceWithoutPolicies() {
        // Given - there are resources with no policies
        // newFile

        // When - access to the resource is queried
        Optional<Resource> resource = hierarchyProxy.canAccess(user, context, newFile);

        // Then - the resource is not accessible
        assertTrue(resource.isEmpty());
    }
}
