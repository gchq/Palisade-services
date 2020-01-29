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
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;

import uk.gov.gchq.palisade.User;
import uk.gov.gchq.palisade.service.user.exception.NoSuchUserIdException;
import uk.gov.gchq.palisade.service.user.request.AddUserRequest;
import uk.gov.gchq.palisade.service.user.request.GetUserRequest;

import java.util.concurrent.CompletableFuture;

import static java.util.Objects.requireNonNull;

/**
 * A SimpleUserService is a simple implementation of a {@link UserService} that keeps user data in the cache service.
 */
public class SimpleUserService implements UserService {
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleUserService.class);

    public SimpleUserService() {
    }

    @Override
    public CompletableFuture<User> getUser(final GetUserRequest request) {
        LOGGER.info("Getting User : {} ", request);
        requireNonNull(request);
        User user = getUserFromCache(new User().userId(request.getId().toString()));
        CompletableFuture<User> userCompletion = CompletableFuture.completedFuture(user);
        if (user == null) {
            LOGGER.error("User {} not found in cache", request);
            userCompletion.obtrudeException(new NoSuchUserIdException(request.userId.getId()));
        }
        return userCompletion;
    }

    @Override
    public CompletableFuture<Boolean> addUser(final AddUserRequest request) {
        LOGGER.info("Adding User : {}", request);
        requireNonNull(request);
        requireNonNull(request.user);
        requireNonNull(request.user.getUserId());
        requireNonNull(request.user.getUserId().getId());
        addUserToCache(new User().userId(request.getId().toString()));
        return CompletableFuture.completedFuture(true);
    }

    @CachePut("users")
    public void addUserToCache(final User user) {
        requireNonNull(user);
    }

    @Cacheable("users")
    public User getUserFromCache(final User user) {
        requireNonNull(user);
        return null;
    }
}
