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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.gov.gchq.palisade.service.policy.common.Context;
import uk.gov.gchq.palisade.service.policy.common.User;
import uk.gov.gchq.palisade.service.policy.common.Util;
import uk.gov.gchq.palisade.service.policy.common.resource.ChildResource;
import uk.gov.gchq.palisade.service.policy.common.resource.LeafResource;
import uk.gov.gchq.palisade.service.policy.common.resource.Resource;
import uk.gov.gchq.palisade.service.policy.common.rule.Rule;
import uk.gov.gchq.palisade.service.policy.common.rule.Rules;
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

    private final PolicyServiceCachingProxy service;

    /**
     * Instantiates a new Policy service hierarchy proxy taking a PolicyService as an argument.
     *
     * @param service {@link PolicyService} used to instantiate this class
     */
    public PolicyServiceHierarchyProxy(final PolicyServiceCachingProxy service) {
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
     * This is where any resource level access controls are enforced, taking the resource, user, context and rules.
     *
     * @param <R>      the type of resource (may be a supertype)
     * @param user     the {@link User} requesting the data
     * @param resource the {@link Resource} being queried for access
     * @param context  the query time {@link Context} containing environmental variables such as why they want the data
     * @param rules    the {@link Rule} that will be applied to the resource
     * @return an Optional {@link Resource} which is only present if the resource is accessible
     */
    public static <R extends Resource> R applyRulesToResource(final User user, final R resource, final Context context, final Rules<R> rules) {
        return Util.applyRulesToItem(resource, user, context, rules);
    }

    /**
     * This method is used to recursively go up the resource hierarchy ending with the original
     * data type to extract and merge the policies at each stage of the hierarchy.
     *
     * @param resource       A {@link Resource} to get the applicable rules for.
     * @param <R>            the type of resource (may be a supertype)
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
     * @param <R>            the type of resource (may be a supertype)
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

    /**
     * GetRecordRules is used by the service to get any record rules that could be applied against the resource that the user has requested
     * If no rules are found then a {@link NoSuchPolicyException} will be thrown
     *
     * @param resource a {@link LeafResource} to get rules for
     * @return the record rules that apply to the LeafResource
     */
    public Rules<Serializable> getRecordRules(final LeafResource resource) {
        Optional<Rules<Serializable>> optionalRules = getRecordRules(resource, rulesResource -> service.getRecordRules(rulesResource.getId()));

        return optionalRules
                .filter(rules -> !rules.getRules().isEmpty())
                .orElseThrow(() -> new NoSuchPolicyException("No Record Rules found for the resource: " + resource.getId()));
    }

    /**
     * GetResourceRules is used by the service to get any resource rules that could be applied against the resource.
     * If no rules are applied then a {@link NoSuchPolicyException} will be thrown
     * A resource rule may be applied at any point in the file tree, and could cause the record to be redacted.
     *
     * @param resource {@link Resource} the user wants access to, this could be a Directory, stream, system resource or file
     * @return rules {@link Rules} object, which contains the list of rules found that need to be applied to the resource
     */
    public Rules<LeafResource> getResourceRules(final LeafResource resource) {
        Optional<Rules<LeafResource>> optionalRules = getResourceRules(resource, ruleResource -> service.getResourceRules(ruleResource.getId()));

        return optionalRules
                .filter(rules -> !rules.getRules().isEmpty())
                .orElseThrow(() -> new NoSuchPolicyException("No Resource Rules found for the resource: " + resource.getId()));
    }

    /**
     * This method sets the record rules against the resource for which the user will eventually request
     *
     * @param resource {@link Resource} the resource which the user wants to apply rules against
     * @param rules    {@link Rules} object, which contains the list of rules to be applied to the resource.
     */
    public void setRecordRules(final Resource resource, final Rules<Serializable> rules) {
        this.service.setRecordRules(resource.getId(), rules);
    }

    /**
     * This method sets the resource rules against the resource for which the user will eventually request
     *
     * @param resource {@link Resource} the user wants access to, this could be a Directory, stream, system resource or file
     * @param rules    {@link Rules} object, which contains the list of rules to be applied to the resource.
     */
    public void setResourceRules(final Resource resource, final Rules<LeafResource> rules) {
        this.service.setResourceRules(resource.getId(), rules);
    }
}
