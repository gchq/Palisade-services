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

package uk.gov.gchq.palisade.service.user.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;

import uk.gov.gchq.palisade.User;
import uk.gov.gchq.palisade.UserId;
import uk.gov.gchq.palisade.service.user.exception.NoSuchUserIdException;

@CacheConfig(cacheNames = {"users"})
public class CachedUserService implements UserService {
    private static final Logger LOGGER = LoggerFactory.getLogger(CachedUserService.class);

    @Cacheable(key = "#userId")
    public User getUser(final UserId userId) {
        LOGGER.info("Cache miss for userId {}", userId);
        throw new NoSuchUserIdException(String.format("No userId matching %s found in cache", userId));
    }

    @CachePut(key = "#user.userId")
    public User addUser(final User user) {
        LOGGER.info("Cache add for userId {}", user.getUserId());
        LOGGER.debug("Added user {} to cache (key=userId)", user);
        return user;
    }

}
