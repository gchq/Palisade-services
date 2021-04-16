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

package uk.gov.gchq.palisade.service.policy.common.policy;

import uk.gov.gchq.palisade.service.policy.common.resource.LeafResource;
import uk.gov.gchq.palisade.service.policy.common.rule.Rules;

import java.io.Serializable;
import java.util.Map.Entry;

/**
 * This class defines the top level of the cache prepopulation.
 * <p>
 * The only requirement is that there is a build method, used to create the object
 */
public interface PolicyPrepopulationFactory {

    /**
     * Creates a {@link Rules} of type {@link LeafResource} that is associated to a resourceId using the
     * data within an implementation of the PolicyPrepopulationFactory
     *
     * @return an {@link Entry} value that consists of a resourceId and the created {@link Rules} of type {@link LeafResource}.
     */
    Entry<String, Rules<LeafResource>> buildResourceRules();

    /**
     * Creates a {@link Rules} of type {@link Serializable} that is associated to a resourceId using the
     * data within an implementation of the PolicyPrepopulationFactory.
     *
     * @return an {@link Entry} value that consists of a resourceId and the created {@link Rules} of type {@link Serializable}.
     */
    Entry<String, Rules<Serializable>> buildRecordRules();

}
