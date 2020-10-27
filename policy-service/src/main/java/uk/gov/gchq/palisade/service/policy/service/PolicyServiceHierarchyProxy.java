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
import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.resource.Resource;
import uk.gov.gchq.palisade.rule.Rules;
import uk.gov.gchq.palisade.service.policy.exception.NoSuchPolicyException;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Optional;
import java.util.function.Function;

/**
 * By having the policies stored in several key value stores we can attach policies
 * at either the resource or data type level.
 * Each rule needs to be flagged as a resource level filter, or a record level filter/transform.
 * To get the rules for a file/stream resource, you need to get the rules for the given resource
 * followed by the rules of all its parents. Then you get the rules of the given resources data type.
 * If there are any negation rules then all rules inherited from up the
 * chain should be checked to see if any rules need removing due to the negation rule.
 */
public class PolicyServiceHierarchyProxy {
    private static final Logger LOGGER = LoggerFactory.getLogger(PolicyServiceHierarchyProxy.class);

    private final PolicyService service;

    public PolicyServiceHierarchyProxy(final PolicyService service) {
        this.service = service;
    }

    private static <T extends Serializable> Rules<T> mergeRules(final Rules<T> inheritedRules, final Rules<T> newRules) {
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

    /**
     * This method is used to find out if the given user is allowed to access
     * the resource given their purpose. This is where any resource level
     * access controls are enforced.
     *
     * @param user     the {@link User} requesting the data
     * @param context  the query time {@link Context} containing environmental variables
     *                 such as why they want the data
     * @param resource the {@link Resource} being queried for access
     * @param rules    the {@link uk.gov.gchq.palisade.rule.Rule} that will be applied to the resource
     * @param <R>      the type of resource (may be a supertype)
     * @return an Optional {@link Resource} which is only present if the resource
     * is accessible
     */
    public static <R extends Resource> R applyRulesToResource(final User user, final R resource, final Context context, final Rules<R> rules) {
        return Util.applyRulesToItem(resource, user, context, rules);
    }

    /**
     * This method is used to recursively go up the resource hierarchy ending with the original
     * data type to extract and merge the policies at each stage of the hierarchy.
     *
     * @param resource       A {@link Resource} to get the applicable rules for.
     * @param <R> the type of resource (may be a supertype)
     * @param rulesExtractor The rule type to extract from each policy
     * @return An optional {@link Rules} object, which contains the list of rules found
     * that need to be applied to the resource.
     */
    private <R extends Resource> Optional<Rules<Serializable>> getRecordRules(final R resource, final Function<Resource, Optional<Rules<Serializable>>> rulesExtractor) {
        LOGGER.debug("Getting the applicable rules: {}", resource);
        Optional<Rules<Serializable>> inheritedRules;
        if (resource instanceof ChildResource) {
            // We will also need the policy applied to the parent resource
            LOGGER.debug("resource {} an instance of ChildResource", resource);
            inheritedRules = getRecordRules(((ChildResource) resource).getParent(), rulesExtractor);
            LOGGER.debug("Inherited rules {} for resource {}", inheritedRules, resource);
        } else {
            // We are at top of hierarchy
            LOGGER.debug("resource {} NOT an instance of ChildResource (top of hierarchy)", resource);
            inheritedRules = Optional.empty();
        }

        Optional<Rules<Serializable>> newRules = rulesExtractor.apply(resource);

        // If both present, merge both
        // If either present, return present
        // If none present, return Optional.empty()
        return inheritedRules.map(iRules -> newRules.map(nRules -> mergeRules(iRules, nRules)).or(() -> inheritedRules)).orElse(newRules);
    }

    /**
     * This method is used to recursively go up the resource hierarchy ending with the original
     * data type to extract and merge the policies at each stage of the hierarchy.
     *
     * @param resource       A {@link Resource} to get the applicable rules for.
     * @param rulesExtractor The rule type to extract from each policy
     * @param <R> the type of resource (may be a supertype)
     * @return An optional {@link Rules} object, which contains the list of rules found
     * that need to be applied to the resource.
     */
    private <R extends Resource> Optional<Rules<LeafResource>> getResourceRules(final R resource, final Function<Resource, Optional<Rules<LeafResource>>> rulesExtractor) {
        LOGGER.debug("Getting the applicable rules: {}", resource);
        Optional<Rules<LeafResource>> inheritedRules;
        if (resource instanceof ChildResource) {
            // We will also need the policy applied to the parent resource
            LOGGER.debug("resource {} an instance of ChildResource", resource);
            inheritedRules = getResourceRules(((ChildResource) resource).getParent(), rulesExtractor);
            LOGGER.debug("Inherited rules {} for resource {}", inheritedRules, resource);
        } else {
            // We are at top of hierarchy
            LOGGER.debug("resource {} NOT an instance of ChildResource (top of hierarchy)", resource);
            inheritedRules = Optional.empty();
        }

        Optional<Rules<LeafResource>> newRules = rulesExtractor.apply(resource);

        // If both present, merge both
        // If either present, return present
        // If none present, return Optional.empty()
        return inheritedRules.map(iRules -> newRules.map(nRules -> mergeRules(iRules, nRules)).or(() -> inheritedRules)).orElse(newRules);
    }

    public Rules<Serializable> getRecordRules(final LeafResource resource) {
        var optionalRules = getRecordRules(resource, service::getRecordRules);

        return optionalRules
                .filter(rules -> !rules.getRules().isEmpty())
                .orElseThrow(() -> new NoSuchPolicyException("No Policy Found"));
    }

    public Rules<LeafResource> getResourceRules(final LeafResource resource) {
        Optional<Rules<LeafResource>> optionalRules = getResourceRules(resource, service::getResourceRules);

        return optionalRules
                .filter(rules -> !rules.getRules().isEmpty())
                .orElseThrow(() -> new NoSuchPolicyException("No Policy Found"));
    }

    public void setRecordRules(final Resource resource, final Rules<Serializable> rules) {
        this.service.setRecordRules(resource, rules);
    }

    public void setResourceRules(final Resource resource, final Rules<LeafResource> rules) {
        this.service.setResourceRules(resource, rules);
    }
}
