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
package uk.gov.gchq.palisade.service.user.service;

import uk.gov.gchq.palisade.service.user.common.User;
import uk.gov.gchq.palisade.service.user.exception.NoSuchUserIdException;

/**
 * A default do-nothing User Service designed to work with the Caching and Hierarchy layers.
 * Within cache TTL and cache size, the addUser method will add to the cache, getUser will return a NoSuchUserIdException
 * as the user would not have been in the cache.
 * After cache TTL timeout, the service will effectively be reset and empty
 */
public class NullUserService implements UserService {

    @Override
    public User getUser(final String userId) {
        throw new NoSuchUserIdException(String.format("No userId matching %s found in cache", userId));
    }

    @Override
    public User addUser(final User user) {
        return user;
    }

}
