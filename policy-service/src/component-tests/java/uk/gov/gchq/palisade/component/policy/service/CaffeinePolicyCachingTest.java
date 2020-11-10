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
package uk.gov.gchq.palisade.component.policy.service;

import com.github.benmanes.caffeine.cache.Cache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.test.context.ActiveProfiles;

import uk.gov.gchq.palisade.contract.policy.common.PolicyTestCommon;
import uk.gov.gchq.palisade.policy.PassThroughRule;
import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.resource.Resource;
import uk.gov.gchq.palisade.resource.StubResource;
import uk.gov.gchq.palisade.resource.impl.FileResource;
import uk.gov.gchq.palisade.resource.impl.SystemResource;
import uk.gov.gchq.palisade.rule.Rules;
import uk.gov.gchq.palisade.service.SimpleConnectionDetail;
import uk.gov.gchq.palisade.service.policy.config.ApplicationConfiguration;
import uk.gov.gchq.palisade.service.policy.service.PolicyServiceCachingProxy;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = {ApplicationConfiguration.class, CacheAutoConfiguration.class},
        webEnvironment = WebEnvironment.NONE,
        properties = {"spring.cache.caffeine.spec=expireAfterWrite=1s, maximumSize=100"}
)
@EnableCaching
@ActiveProfiles({"caffeine"})
class CaffeinePolicyCachingTest extends PolicyTestCommon {

    @Autowired
    private PolicyServiceCachingProxy policyService;

    @Autowired
    private CacheManager cacheManager;

    /**
     * Cleans up the caches used by the Policy Service
     */
    private void forceCleanUp() {
        List<String> caches = Arrays.asList("recordRules", "resourceRules");
        caches.forEach(x -> ((Cache<?, ?>) Objects.requireNonNull(cacheManager.getCache(x)).getNativeCache()).cleanUp());
    }

    /**
     * Before the tests run, add policies to resources in the Policy service
     */
    @BeforeEach
    void setup() {
        // Add the system resource to the policy service
        policyService.setResourceRules(TXT_SYSTEM, TXT_POLICY);

        // Add the directory resources to the policy service
        policyService.setResourceRules(JSON_DIRECTORY, JSON_POLICY);
        policyService.setResourceRules(SECRET_DIRECTORY, SECRET_POLICY);

        // Add the file resources to the policy service
        for (FileResource fileResource : FILE_RESOURCES) {
            policyService.setResourceRules(fileResource, PASS_THROUGH_POLICY);
        }
    }

    /**
     * Tests that the service loads
     */
    @Test
    void testContextLoads() {
        assertThat(policyService).isNotNull();
    }

    /**
     * Tests that the correct policy is retrieved for the resource
     */
    @Test
    void testAddedRuleIsRetrievable() {
        // Given - resources have been added as above
        // Given there is no underlying policy storage (gets must be wholly cache-based)

        for (Resource resource : FILE_RESOURCES) {
            // When
            Optional<Rules<LeafResource>> recordRules = policyService.getResourceRules(resource);

            // Then
            assertThat(recordRules).isPresent();
        }
    }

    /**
     * Tests that if the resource is not added for the policy then nothing is returned
     */
    @Test
    void testNonExistentRuleRetrieveFails() {
        // Given - the requested resource is not added

        // When
        Optional<Rules<LeafResource>> recordRules = policyService.getResourceRules(new FileResource().id("does not exist").type("null").serialisedFormat("null").parent(new SystemResource().id("also does not exist")));

        // Then
        assertThat(recordRules).isEmpty();
    }

    /**
     * Tests that if the cache is full, the first entry is removed
     */
    @Test
    void testCacheMaxSize() {
        /// Given - the cache is overfilled
        Function<Integer, Resource> makeResource = i -> new StubResource(i.toString(), i.toString(), i.toString(), new SimpleConnectionDetail().serviceName(i.toString()));
        Function<Integer, Rules<LeafResource>> makeRule = i -> new Rules<LeafResource>().addRule(i.toString(), new PassThroughRule<>());
        for (int count = 0; count <= 100; ++count) {
            policyService.setResourceRules(makeResource.apply(count), makeRule.apply(count));
        }

        // When - we try to get the first (now-evicted) entry
        forceCleanUp();
        Optional<Rules<LeafResource>> recordRules = policyService.getResourceRules(makeResource.apply(0));

        // Then - it has been evicted
        assertThat(recordRules).isEmpty();
    }

    /**
     * Tests that if the entry ttl expires it is removed
     *
     * @throws InterruptedException in case TimeUnit.sleep throws an exception
     */
    @Test
    void testCacheTtl() throws InterruptedException {
        // Given - the requested resource has policies available
        assertThat(policyService.getResourceRules(ACCESSIBLE_JSON_TXT_FILE)).isPresent();
        // Given - a sufficient amount of time has passed

        TimeUnit.SECONDS.sleep(1);
        forceCleanUp();

        // When - an old entry is requested
        Optional<Rules<LeafResource>> recordRules = policyService.getResourceRules(ACCESSIBLE_JSON_TXT_FILE);

        // Then - it has been evicted
        assertThat(recordRules).isEmpty();
    }
}
