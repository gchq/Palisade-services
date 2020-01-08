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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.User;
import uk.gov.gchq.palisade.Util;
import uk.gov.gchq.palisade.resource.ChildResource;
import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.resource.Resource;
import uk.gov.gchq.palisade.rule.Rules;
import uk.gov.gchq.palisade.service.policy.request.AddCacheRequest;
import uk.gov.gchq.palisade.service.policy.request.CanAccessRequest;
import uk.gov.gchq.palisade.service.policy.request.CanAccessResponse;
import uk.gov.gchq.palisade.service.policy.request.GetCacheRequest;
import uk.gov.gchq.palisade.service.policy.request.GetPolicyRequest;
import uk.gov.gchq.palisade.service.policy.request.MultiPolicy;
import uk.gov.gchq.palisade.service.policy.request.Policy;
import uk.gov.gchq.palisade.service.policy.request.SetResourcePolicyRequest;
import uk.gov.gchq.palisade.service.policy.request.SetTypePolicyRequest;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * By having the policies stored in several key value stores we can attach policies
 * at either the resource or data type level.
 * Each rule needs to be flagged as a resource level filter, or a record level filter/transform.
 * To get the rules for a file/stream resource, you need to get the rules for the given resource
 * followed by the rules of all its parents. Then you get the rules of the given resources data type.
 * If there are any negation rules then all rules inherited from up the
 * chain should be checked to see if any rules need removing due to the negation rule.
 */
public class HierarchicalPolicyService implements PolicyService {
    private static final Logger LOGGER = LoggerFactory.getLogger(HierarchicalPolicyService.class);

    private static final String DATA_TYPE_POLICIES_PREFIX = "dataTypePolicy.";
    private static final String RESOURCE_POLICIES_PREFIX = "resourcePolicy.";

    public static final String CACHE_IMPL_KEY = "policy.svc.cache.svc";

    private CacheService cacheService;

    public HierarchicalPolicyService(final CacheService cacheService) {
        this.cacheService = cacheService;
    }

    public HierarchicalPolicyService cacheService(final CacheService cacheService) {
        requireNonNull(cacheService, "Cache service cannot be set to null.");
        this.cacheService = cacheService;
        return this;
    }

    public void setCacheService(final CacheService cacheService) {
        cacheService(cacheService);
    }

    public CacheService getCacheService() {
        requireNonNull(cacheService, "The cache service has not been set.");
        return cacheService;
    }

    @Override
    public CompletableFuture<CanAccessResponse> canAccess(final CanAccessRequest request) {
        LOGGER.debug("Determining access: {}", request);
        Context context = request.getContext();
        User user = request.getUser();
        Collection<LeafResource> resources = request.getResources();
        CanAccessResponse response = new CanAccessResponse().canAccessResources(canAccess(context, user, resources));
        return CompletableFuture.completedFuture(response);
    }

    private Collection<LeafResource> canAccess(final Context context, final User user, final Collection<LeafResource> resources) {
        LOGGER.debug("Determining access: {} for user {} with these resources {}", context, user, Arrays.toString(resources.toArray()));
        return resources.stream()
                .map(resource -> {
                    CompletableFuture<Optional<Rules<LeafResource>>> futureRules = getApplicableRules(resource, true, resource.getType());
                    Optional<Rules<LeafResource>> rules = futureRules.join();
                    if (rules.isPresent()) {
                        LOGGER.debug("resource {}, has the following policy {}", resource, rules);
                        return Util.applyRulesToItem(resource, user, context, rules.get(), new AtomicLong(0), new AtomicLong(0));
                    } else {
                        LOGGER.debug("No policy for {}, removing resource from list...", resource);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * This method is used to recursively go up the resource hierarchy ending with the original
     * data type to extract and merge the policies at each stage of the hierarchy.
     *
     * @param resource         A {@link Resource} to get the applicable rules for.
     * @param canAccessRequest A boolean that is true if you want the resource level.
     *                         rules and therefore this is called from the canAccess method
     * @param originalDataType This is the data type that you want to be at the top of the
     *                         Resource hierarchy tree, which will be the data type of the
     *                         first resource in the recursive calls to this method.
     * @param <T>              The type of the returned {@link Rules}.
     * @return A completable future of {@link Rules} object of type T, which contains the list of rules
     * that need to be applied to the resource.
     */
    protected <T> CompletableFuture<Optional<Rules<T>>> getApplicableRules(final Resource resource, final boolean canAccessRequest, final String originalDataType) {
        LOGGER.debug("Getting the applicable rules: {} {} {}", resource, canAccessRequest, originalDataType);
        CompletableFuture<Optional<Rules<T>>> inheritedRules;
        if (resource instanceof ChildResource) {
            LOGGER.debug("resource is an instance of ChildResource");
            inheritedRules = getApplicableRules(((ChildResource) resource).getParent(), canAccessRequest, originalDataType);
        } else {
            //we are at top of hierarchy
            LOGGER.debug("resource is NOT an instance of ChildResource (top of hierachy)");
            CompletableFuture<Optional<Policy>> inheritedPolicy = (CompletableFuture<Optional<Policy>>) getCacheService().get(
                    new GetCacheRequest<Policy>()
                            .service(this.getClass())
                            .key(DATA_TYPE_POLICIES_PREFIX + originalDataType));

            inheritedRules = inheritedPolicy.thenApply(policy -> extractRules(canAccessRequest, policy));
        }

        CompletableFuture<Optional<Policy>> newPolicy = (CompletableFuture<Optional<Policy>>) getCacheService().get(
                new GetCacheRequest<Policy>()
                        .service(this.getClass())
                        .key(RESOURCE_POLICIES_PREFIX + resource.getId()));

        return inheritedRules.thenCombine(newPolicy, (oldRules, policy) -> {
            Optional<Rules<T>> newRules = extractRules(canAccessRequest, policy);
            return mergeRules(oldRules, newRules);
        });
    }

    private <T> Optional<Rules<T>> extractRules(final boolean canAccessRequest, final Optional<Policy> policy) {
        if (canAccessRequest) {
            return policy.map(p -> {
                        Rules rules = p.getResourceRules();
                        LOGGER.debug("getting RESOURCE rules for Policy: {}", p.getMessage());
                        return rules;
                    }
            );
        } else {
            return policy.map(p -> {
                        Rules rules = p.getRecordRules();
                        LOGGER.debug("getting RECORD rules for Policy: {}", p.getMessage());
                        return rules;
                    }
            );
        }
    }

    private <T> Optional<Rules<T>> mergeRules(final Optional<Rules<T>> inheritedRules, final Optional<Rules<T>> newRules) {

        if (inheritedRules.isPresent() && newRules.isPresent()) {
            LOGGER.debug("inheritedRules and newRules both present MessageInherited:{} MessageNew:{} RulesInherited:{} RulesNew:{}", inheritedRules.get().getMessage(), newRules.get().getMessage(),
                    inheritedRules.get().getRules(), newRules.get().getRules());
            //both present --> merge
            String inheritedMessage = inheritedRules.get().getMessage();
            String newMessage = newRules.get().getMessage();
            if (!inheritedMessage.equals(Rules.NO_RULES_SET) && !newMessage.equals(Rules.NO_RULES_SET)) {
                inheritedRules.get().message(inheritedMessage + ", " + newMessage);
            } else if (!newMessage.equals(Rules.NO_RULES_SET)) {
                inheritedRules.get().message(newMessage);
            }
            //don't test for inheritedRules != Rules.NO_RULES_SET as that is the default case, there is nothing to do
            inheritedRules.get().addRules(newRules.get().getRules());
            LOGGER.debug("mergeRules -  Message:{} Rules:{}", inheritedRules.get().getMessage(), inheritedRules.get().getRules());
            return inheritedRules;
        } else if (inheritedRules.isPresent()) {
            //only inherited present
            LOGGER.debug("inherited only Message:{} Rules:{}", inheritedRules.get().getMessage(), inheritedRules.get().getRules());
            return inheritedRules;
        } else if (newRules.isPresent()) {
            LOGGER.debug("new only Message:{} Rules:{}", newRules.get().getMessage(), newRules.get().getRules());
            return newRules;
        } else {
            LOGGER.debug("no rules present");
            return Optional.empty();
        }
    }

    @Override
    public CompletableFuture<MultiPolicy> getPolicy(final GetPolicyRequest request) {
        LOGGER.debug("getPolicy - {}", request);
        Context context = request.getContext();
        User user = request.getUser();
        Collection<LeafResource> resources = request.getResources();
        Collection<LeafResource> canAccessResources = canAccess(context, user, resources);
        /* Having filtered out any resources the user doesn't have access to in the line above, we now build the map
         * of resource to record level rule policies. If there are resource level rules for a record then there SHOULD
         * be record level rules. Either list may be empty, but they should at least be present!
         */
        HashMap<LeafResource, Policy> map = new HashMap<>();
        canAccessResources.forEach(resource -> {
            CompletableFuture<Optional<Rules<Object>>> rules = getApplicableRules(resource, false, resource.getType());
            Optional<Rules<Object>> optionalRecordRules = rules.join();
            if (optionalRecordRules.isPresent()) {
                Policy<Object> policy = new Policy<>().recordRules(optionalRecordRules.get());
                LOGGER.debug("adding resource: {} with the following policy: {}", resource, policy);
                map.put(resource, policy);
            } else {
                LOGGER.warn("Couldn't find any record level rules for {}. This shouldn't be the case, since we found resource level rules for it!", user.getUserId().getId());
            }
        });
        return CompletableFuture.completedFuture(new MultiPolicy().policies(map));
    }

    @Override
    public CompletableFuture<Boolean> setResourcePolicy(final SetResourcePolicyRequest request) {
        requireNonNull(request);
        Resource resource = request.getResource();
        Policy policy = request.getPolicy();
        LOGGER.debug("Setting resource policy {} to resource {}", policy, resource);
        return getCacheService().add(
                new AddCacheRequest<Policy>()
                        .service(this.getClass())
                        .key(RESOURCE_POLICIES_PREFIX + resource.getId())
                        .value(policy));
    }

    @Override
    public CompletableFuture<Boolean> setTypePolicy(final SetTypePolicyRequest request) {
        requireNonNull(request);
        final String type = request.getType();
        final Policy policy = request.getPolicy();
        LOGGER.debug("Setting Type policy {} to data type {}", policy, type);
        return getCacheService().add(
                new AddCacheRequest<Policy>()
                        .service(this.getClass())
                        .key(DATA_TYPE_POLICIES_PREFIX + type)
                        .value(policy));
    }
}
