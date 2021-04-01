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

import uk.gov.gchq.palisade.service.policy.common.resource.LeafResource;
import uk.gov.gchq.palisade.service.policy.common.resource.Resource;
import uk.gov.gchq.palisade.service.policy.common.rule.Rules;
import uk.gov.gchq.palisade.service.policy.common.service.Service;

import java.io.Serializable;
import java.util.Optional;

/**
 * The core API for the policy service.
 * The responsibilities of the policy service is to provide the set of rules
 * (filters or transformations) that need to be applied to each resource that
 * has been requested, based the user and context.
 * Note that a resource could be a file, stream, directory or even the system
 * resource (policies added to the system resource would be applied globally).
 */
public interface PolicyService extends Service {

    /**
     * GetResourceRules is used by the service to get any resource rules that could be applied against the resource.
     * If no rules are applied then an exception will be thrown
     * A resource rule may be applied at any point in the file tree, and could cause the record to be redacted.
     *
     * @param resourceId the id of the {@link Resource} the user wants access to, this could be a Directory, stream, system resource or file
     * @return An optional {@link Rules} object, which contains the list of rules found that need to be applied to the resource.
     */
    Optional<Rules<LeafResource>> getResourceRules(final String resourceId);

    /**
     * GetRecordRules is used by the service to get any record rules that could be applied against the resource that the user has requested
     *
     * @param resourceId the id of the {@link Resource} to get rules for
     * @return An optional {@link Rules} object, which contains the list of rules found that need to be applied to the resource.
     */
    Optional<Rules<Serializable>> getRecordRules(final String resourceId);

    /**
     * This method sets the resource rules against the resource for which the user will eventually request
     *
     * @param resourceId the id of the {@link Resource} the user wants access to, this could be a Directory, stream, system resource or file
     * @param rules      {@link Rules} object, which contains the list of rules to be applied to the resource.
     * @return an Optional Rules for LeafResource object that contains the returned map of resource rules for each resource
     */
    Optional<Rules<LeafResource>> setResourceRules(final String resourceId, final Rules<LeafResource> rules);

    /**
     * This method sets the record rules against the resource for which the user will eventually request
     *
     * @param resourceId the id of the {@link Resource} the user wants to apply rules against
     * @param rules      {@link Rules} object, which contains the list of rules to be applied to the resource.
     * @return an Optional Serializable rules object that contains the returned map of record rules for each resource
     */
    Optional<Rules<Serializable>> setRecordRules(final String resourceId, final Rules<Serializable> rules);
}
