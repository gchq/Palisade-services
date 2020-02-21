/*
 * Copyright 2019 Crown Copyright
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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

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
import uk.gov.gchq.palisade.service.policy.request.Policy;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeThat;

@RunWith(JUnit4.class)
public class PolicyServiceHierarchyProxyTest {
    private final User user = new User().userId("testUser");
    private final User sensitiveUser = new User().userId("sensitiveTestUser").addAuths(Collections.singleton("Sensitive"));
    private final User secretUser = new User().userId("secretTestUser").addAuths(new HashSet<>(Arrays.asList("Sensitive", "Secret")));
    private final Context context = new Context().purpose("testing");

    // A system that only allows text files to be seen
    private final SystemResource txtSystem = new SystemResource().id("/txt");
    private final Policy txtPolicy = new Policy<>()
            .owner(user)
            .resourceLevelRule("Resource serialised format is txt", new IsTextResourceRule());

    // A directory that only allows JSON serialisation
    private final DirectoryResource jsonDirectory = new DirectoryResource().id("/txt/json").parent(txtSystem);
    private final Policy jsonPolicy = new Policy<>()
            .owner(user)
            .resourceLevelRule("Does nothing", (PredicateRule<Resource>) (resource, user, context) -> resource instanceof LeafResource && ((LeafResource) resource).getSerialisedFormat().equals("json"));

    // A text file containing json data - this should be accessible
    private final FileResource accessibleJsonTxtFile = new FileResource().id("/txt/json/json.txt").type("txt").serialisedFormat("json").parent(jsonDirectory);
    // An avro file containing json data - this should be inaccessible due to the system policy
    private final FileResource inaccessibleJsonAvroFile = new FileResource().id("/txt/json/json.avro").type("avro").serialisedFormat("json").parent(jsonDirectory);
    // A text file containing pickle data - this should be inaccessible due to the directory policy
    private final FileResource inaccessiblePickleTxtFile = new FileResource().id("/txt/json/pickled.txt").type("txt").serialisedFormat("pickle").parent(jsonDirectory);

    // A sensitive directory that only allows authorised users to view contents of records
    private final DirectoryResource sensitiveDirectory = new DirectoryResource().id("/txt/sensitive").parent(txtSystem);
    private final Policy sensitivePolicy = new Policy<>()
            .owner(sensitiveUser)
            .resourceLevelRule("Does nothing", new PassThroughRule<>())
            .recordLevelRule("Check user has 'Sensitive' auth", new HasSensitiveAuthRule<>());

    // A sensitive text file containing a report of salary information - the content should be accessible to authorised users only
    private final FileResource sensitiveTxtFile = new FileResource().id("/txt/sensitive/report.txt").type("txt").serialisedFormat("txt").parent(sensitiveDirectory);
    // A sensitive CSV of salary information - this should be inaccessible due to the system policy
    private final FileResource sensitiveCsvFile = new FileResource().id("/txt/sensitive/salary.csv").type("csv").serialisedFormat("txt").parent(sensitiveDirectory);

    // A secret directory that allows authorised users to view resources and secret users to view contents of records
    private final DirectoryResource secretDirectory = new DirectoryResource().id("/txt/secret").parent(txtSystem);
    private final Policy secretPolicy = new Policy<>()
            .owner(secretUser)
            .resourceLevelRule("Check user has 'Sensitive' auth", new HasSensitiveAuthRule<>())
            .recordLevelRule("Check user has 'Secret' auth", (PredicateRule<Object>) (resource, user, context) -> user.getAuths().contains("Secret"));

    // A secret file - accessible only to the secret user
    private final FileResource secretTxtFile = new FileResource().id("/txt/secret/secrets.txt").type("txt").serialisedFormat("txt").parent(secretDirectory);

    private final FileResource newFile = new FileResource().id("/new/file.txt").type("txt").serialisedFormat("txt").parent(new SystemResource().id("/new"));

    // A do-nothing policy to apply to leaf resources
    private final Policy passThroughPolicy = new Policy<>()
            .owner(user)
            .resourceLevelRule("Does nothing", new PassThroughRule<>())
            .recordLevelRule("Does nothing", new PassThroughRule<>());

    private final Set<DirectoryResource> directoryResources = new HashSet<>(Arrays.asList(jsonDirectory, sensitiveDirectory, secretDirectory));
    private final Set<FileResource> fileResources = new HashSet<>(Arrays.asList(accessibleJsonTxtFile, inaccessibleJsonAvroFile, inaccessiblePickleTxtFile, sensitiveTxtFile, sensitiveCsvFile, secretTxtFile));

    private PolicyServiceHierarchyProxy hierarchyProxy;
    private PolicyService serviceMock;

    @Before
    public void setup() {
        serviceMock = new SimplePolicyService();
        hierarchyProxy = new PolicyServiceHierarchyProxy(serviceMock);

        // Add the system resource to the policy service
        assumeThat(hierarchyProxy.setResourcePolicy(txtSystem, txtPolicy), equalTo(txtPolicy));

        // Add the directory resources to the policy service
        assumeThat(hierarchyProxy.setResourcePolicy(jsonDirectory, jsonPolicy), equalTo(jsonPolicy));
        assumeThat(hierarchyProxy.setResourcePolicy(sensitiveDirectory, sensitivePolicy), equalTo(sensitivePolicy));
        assumeThat(hierarchyProxy.setResourcePolicy(secretDirectory, secretPolicy), equalTo(secretPolicy));

        // Add the file resources to the policy service
        for (FileResource fileResource : fileResources) {
            assumeThat(hierarchyProxy.setResourcePolicy(fileResource, passThroughPolicy), equalTo(passThroughPolicy));
        }
    }

    @Test
    public void getRecordLevelRules() {
    }

    @Test
    public void shouldReturnNoPolicyWhenNotSet() {
        // Given - there are no policies for the requested resource
        // newFile

        // When - a policy is requested on a resource
        Optional<Policy> policy = hierarchyProxy.getPolicy(newFile);

        // Then - no such policy was retrieved
        assertFalse(policy.isPresent());
    }

    @Test
    public void canAccessResources() {
        // Given - there are accessible resources
        // accessibleJsonTxtFile for user, sensitiveTxtFile for sensitiveUser, secretTxtFile for secretUser

        // When - access to the resource is queried
        Optional<Resource> resource = hierarchyProxy.canAccess(user, context, accessibleJsonTxtFile);
        // Then - the resource is accessible
        assertTrue(resource.isPresent());

        // When - access to the resource is queried
        resource = hierarchyProxy.canAccess(sensitiveUser, context, sensitiveTxtFile);
        // Then - the resource is accessible
        assertTrue(resource.isPresent());

        // When - access to the resource is queried
        resource = hierarchyProxy.canAccess(secretUser, context, secretTxtFile);
        // Then - the resource is accessible
        assertTrue(resource.isPresent());
    }

    @Test
    public void cannotAccessNonResources() {
        // Given - there are inaccessible resources
        // everything but accessibleJsonTxtFile for user
        HashSet<FileResource> resources = new HashSet<>(fileResources);
        resources.remove(accessibleJsonTxtFile);
        for (FileResource fileResource : resources) {
            // When - access to the resource is queried
            Optional<Resource> resource = hierarchyProxy.canAccess(user, context, fileResource);
            // Then - the resource is accessible
            assertTrue(resource.isPresent());
        }

        // Given - there are inaccessible resources
        // everything but (accessible/sensitive)TxtFile for sensitiveUser
        resources = new HashSet<>(fileResources);
        resources.remove(accessibleJsonTxtFile);
        resources.remove(sensitiveTxtFile);
        for (FileResource fileResource : resources) {
            // When - access to the resource is queried
            Optional<Resource> resource = hierarchyProxy.canAccess(sensitiveUser, context, fileResource);
            // Then - the resource is accessible
            assertTrue(resource.isPresent());
        }

        // Given - there are inaccessible resources
        // everything except (accessible/sensitive/secret)TxtFile for secretUser
        resources = new HashSet<>(fileResources);
        resources.remove(accessibleJsonTxtFile);
        resources.remove(sensitiveTxtFile);
        resources.remove(secretTxtFile);
        for (FileResource fileResource : resources) {
            // When - access to the resource is queried
            Optional<Resource> resource = hierarchyProxy.canAccess(sensitiveUser, context, fileResource);
            // Then - the resource is accessible
            assertTrue(resource.isPresent());
        }
    }
}

