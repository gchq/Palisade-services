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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.User;
import uk.gov.gchq.palisade.Util;
import uk.gov.gchq.palisade.resource.ChildResource;
import uk.gov.gchq.palisade.resource.Resource;
import uk.gov.gchq.palisade.rule.Rules;
import uk.gov.gchq.palisade.service.policy.request.Policy;

import java.util.ArrayList;
import java.util.Optional;
import java.util.function.Function;

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
public class PolicyServiceHierarchyProxy implements PolicyService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PolicyServiceHierarchyProxy.class);

    final PolicyService service;

    public PolicyServiceHierarchyProxy(final PolicyService service) {
        this.service = service;
    }

    @Override
    public <R extends Resource> Optional<R> canAccess(final User user, final Context context, final R resource) {
        LOGGER.debug("Determining access: {} for user {} with these resources {}", context, user, resource);
        Optional<R> serviceCanAccess = service.canAccess(user, context, resource);
        // If the service says we can access the resource naively, check up the resource hierarchy
        // If all parent, grandparent etc. resources say we can access the resource, then it is accessible
        Optional<Rules<Resource>> accessRules = serviceCanAccess.flatMap(res -> getHierarchicalRules(res, Policy::getResourceRules));
        return (Optional<R>) accessRules.map(rule -> Util.applyRulesToItem(resource, user, context, rule));
    }

    /**
     * This method is used to recursively go up the resource hierarchy ending with the original
     * data type to extract and merge the policies at each stage of the hierarchy.
     *
     * @param resource       A {@link Resource} to get the applicable rules for.
     * @param rulesExtractor The rule type to extract from each policy
     * @param <T>            The record tpe for this resource
     * @return An optional {@link Rules} object, which contains the list of rules found
     * that need to be applied to the resource.
     */
    private <T> Optional<Rules<T>> getHierarchicalRules(final Resource resource, final Function<Policy, Rules<T>> rulesExtractor) {
        LOGGER.debug("Getting the applicable rules: {}", resource);
        Optional<Rules<T>> inheritedRules;
        if (resource instanceof ChildResource) {
            // We will also need the policy applied to the parent resource
            LOGGER.debug("resource {} an instance of ChildResource", resource);
            inheritedRules = getHierarchicalRules(((ChildResource) resource).getParent(), rulesExtractor);
            LOGGER.debug("Inherited rules {} for resource {}", inheritedRules, resource);
        } else {
            // We are at top of hierarchy
            LOGGER.debug("resource {} NOT an instance of ChildResource (top of hierarchy)", resource);
            inheritedRules = Optional.empty();
        }

        Optional<Policy> newPolicy = service.getPolicy(resource);
        Optional<Rules<T>> newRules = newPolicy.map(rulesExtractor);

        // If both present, merge both
        // If either present, return present
        // If none present, return Optional.empty()
        return inheritedRules.map(iRules -> newRules.map(nRules -> mergeRules(iRules, nRules)).or(() -> inheritedRules)).orElse(newRules);
    }

    private <T> Rules<T> mergeRules(final Rules<T> inheritedRules, final Rules<T> newRules) {
        LOGGER.debug("inheritedRules and newRules both present\n MessageInherited: {}\n MessageNew: {}\n RulesInherited: {}\n RulesNew: {}",
                inheritedRules.getMessage(), newRules.getMessage(), inheritedRules.getRules(), newRules.getRules());
        Rules<T> mergedRules = new Rules<>();

        // Merge messages
        ArrayList<String> messages = new ArrayList<>();
        String inheritedMessage = inheritedRules.getMessage();
        String newMessage = newRules.getMessage();
        if (!inheritedMessage.equals(Rules.NO_RULES_SET)) {
            messages.add(inheritedMessage);
        }
        if (!newMessage.equals(Rules.NO_RULES_SET)) {
            messages.add(newMessage);
        }
        mergedRules.message(String.join(",", messages));
        LOGGER.debug("Merged messages: {} + {} -> {}", inheritedRules.getMessage(), newRules.getMessage(), mergedRules.getMessage());

        // Merge rules
        mergedRules.addRules(inheritedRules.getRules());
        mergedRules.addRules(newRules.getRules());
        LOGGER.debug("Merged rules: {} + {} -> {}", inheritedRules.getRules(), newRules.getRules(), mergedRules.getRules());

        return mergedRules;
    }

    @Override
    public Optional<Policy> getPolicy(final Resource resource) {
        Optional<Rules<Object>> optionalRules = getHierarchicalRules(resource, Policy::getRecordRules);
        return optionalRules.map(rules -> new Policy<>().recordRules(rules));
    }

    @Override
    public <T> Policy<T> setResourcePolicy(final Resource resource, final Policy<T> policy) {
        requireNonNull(resource, "type cannot be null");
        LOGGER.debug("Setting Resource policy {} to resource {}", policy, resource);
        return service.setResourcePolicy(resource, policy);
    }

    @Override
    public <T> Policy<T> setTypePolicy(final String type, final Policy<T> policy) {
        requireNonNull(type, "type cannot be null");
        LOGGER.debug("Setting Type policy {} to data type {}", policy, type);
        return service.setTypePolicy(type, policy);
    }
}
