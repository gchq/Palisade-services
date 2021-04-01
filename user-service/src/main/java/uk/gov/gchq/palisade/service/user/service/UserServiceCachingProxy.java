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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;

import uk.gov.gchq.palisade.service.user.common.User;

/**
 * This acts as a caching layer on top of an implementation of the user-service.
 */
@CacheConfig(cacheNames = {"users"})
public class UserServiceCachingProxy {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserServiceCachingProxy.class);
    private final UserService service;

    /**
     * Default constructor used to create the UserServiceCachingProxy
     *
     * @param service an implementation of the {@link UserService}
     */
    public UserServiceCachingProxy(final UserService service) {
        this.service = service;
    }

    /**
     * Using the userId as the key, retrieves the {@link User} from the cache. If the User doesn't exist
     * then a message is Logged, and the request is passed to the service implementation.
     * This will either return a user or an error if there was an issue.
     *
     * @param userId of the user wants to retrieve from the cache
     * @return the user returned from the cache
     */
    @Cacheable(key = "#userId")
    public User getUser(final String userId) {
        LOGGER.info("Cache miss for userId {}", userId);
        return service.getUser(userId);
    }

    /**
     * Using the userId from the User object as they key, adds the User, and any attributes about the user,
     * such as roles and auths, to the cache.
     *
     * @param user the user that the client wants to request resources with
     * @return the user that was successfully added to the cache
     */
    @CachePut(key = "#user.userId.id")
    public User addUser(final User user) {
        LOGGER.info("Cache add for userId {}", user.getUserId().getId());
        LOGGER.debug("Added user {} to cache (key=userId)", user);
        return service.addUser(user);
    }

}
