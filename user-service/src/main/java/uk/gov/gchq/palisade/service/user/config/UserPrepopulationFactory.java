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

package uk.gov.gchq.palisade.service.user.config;

import uk.gov.gchq.palisade.user.User;

/**
 * This class defines the top level of the cache prepopulation.
 * <p>
 * The only requirement is that there is a warm method, used to create the object
 */
public interface UserPrepopulationFactory {

    /**
     * Creates a {@link User} using the data within an implementation of the {@link UserPrepopulationFactory}.
     *
     * @return the {@link User} that has been created.
     */
    User build();

}
