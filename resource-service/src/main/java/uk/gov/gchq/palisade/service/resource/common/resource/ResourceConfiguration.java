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

package uk.gov.gchq.palisade.service.resource.common.resource;

import java.net.URI;
import java.util.List;

/**
 * A configuration class for adding Resources to a backing store within a service as part of pre-population
 */
public interface ResourceConfiguration {

    /**
     * Gets a {@link List} of the {@link ResourcePrepopulationFactory} implemented
     * objects that have been created from a yaml file, paired with the {@link URI} of the topmost parent
     * that resource should be prepopulated up-to.
     *
     * @return a {@link List} of the objects that have implemented {@link ResourcePrepopulationFactory}.
     */
    @SuppressWarnings("java:S1452")
    List<? extends ResourcePrepopulationFactory> getResources();

}
