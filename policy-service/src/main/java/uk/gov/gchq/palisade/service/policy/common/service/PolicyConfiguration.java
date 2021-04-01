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

package uk.gov.gchq.palisade.service.policy.common.service;

import java.util.List;

public interface PolicyConfiguration {

    /**
     * Gets a {@link List} of the {@link PolicyPrepopulationFactory} implemented
     * objects that have been created from a yaml file.
     *
     * @return a {@link List} of the objects that have implemented {@link PolicyPrepopulationFactory}.
     */
    List<? extends PolicyPrepopulationFactory> getPolicies();

}
