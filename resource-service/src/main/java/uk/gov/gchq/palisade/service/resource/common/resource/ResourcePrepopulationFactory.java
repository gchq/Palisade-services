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

import java.util.Map.Entry;
import java.util.function.Function;

/**
 * This class defines the top level of persistence prepopulation.
 * <p>
 * The only requirement is that there is a build method to construct a LeafResource
 */
public interface ResourcePrepopulationFactory {

    /**
     * Creates a {@link LeafResource} using the data within an implementation of the {@link ResourcePrepopulationFactory}.
     *
     * @param connectionDetailMapper a function mapping the connection detail {@link String}s to proper {@link ConnectionDetail}s
     * @return the {@link LeafResource} that has been created.
     */
    Entry<Resource, LeafResource> build(Function<String, ConnectionDetail> connectionDetailMapper);

}
