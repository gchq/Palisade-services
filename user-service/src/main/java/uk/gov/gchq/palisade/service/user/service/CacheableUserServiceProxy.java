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

@CacheConfig(cacheNames = {"users"})
public class CacheableUserServiceProxy implements UserService {
    private static final Logger LOGGER = LoggerFactory.getLogger(CacheableUserServiceProxy.class);
    private final UserService service;

    public CacheableUserServiceProxy(final UserService service) {
        this.service = service;
    }

    @Cacheable(key = "#userId")
    public User getUser(final String user) {
        LOGGER.info("Cache miss for userId {}", user);
        return service.getUser(user);
    }

    @CachePut(key = "#user.userId.id")
    public User addUser(final User user) {
        LOGGER.info("Cache add for userId {}", user.getUserId());
        LOGGER.debug("Added user {} to cache (key=userId)", user);
        return service.addUser(user);
    }

}
