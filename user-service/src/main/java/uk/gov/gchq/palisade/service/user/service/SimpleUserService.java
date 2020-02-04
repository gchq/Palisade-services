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
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import uk.gov.gchq.palisade.User;
import uk.gov.gchq.palisade.UserId;
import uk.gov.gchq.palisade.service.user.exception.NoSuchUserIdException;
import uk.gov.gchq.palisade.service.user.request.AddUserRequest;
import uk.gov.gchq.palisade.service.user.request.GetUserRequest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import static java.util.Objects.requireNonNull;

/**
 * A SimpleUserService is a simple implementation of a {@link UserService} that keeps user data in the cache service.
 */
@CacheConfig(cacheNames = {"users"})
public class SimpleUserService implements UserService {
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleUserService.class);

    public SimpleUserService() {
        Resource resource = new ClassPathResource("users.txt");
        try {
            InputStream inputStream = resource.getInputStream();
            InputStreamReader isr = new InputStreamReader(inputStream);
            BufferedReader br = new BufferedReader(isr);
            String line;
            while ((line = br.readLine()) != null) {
                User newUser = new User().userId(line);
                addUser(newUser);
            }
        } catch (IOException e) {
            LOGGER.error("IOException", e);
        }
    }

    @Override
    public CompletableFuture<User> getUser(final GetUserRequest request) {
        requireNonNull(request);
        User user = getUser(request.userId);
        CompletableFuture<User> userCompletion = CompletableFuture.completedFuture(user);
        if (user == null) {
            LOGGER.error("User {} not found in cache", request.userId.getId());
            userCompletion.obtrudeException(new NoSuchUserIdException(request.userId.getId()));
        }
        return userCompletion;
    }

    @Cacheable(key = "#userId")
    public User getUser(final UserId userId) {
        LOGGER.warn("Cache miss for userId {}", userId);
        return null;
    }

    @Override
    public CompletableFuture<Boolean> addUser(final AddUserRequest request) {
        requireNonNull(request);
        addUser(request.user);
        User newUser = getUser(request.user.getUserId());
        return CompletableFuture.completedFuture(Objects.nonNull(newUser));
    }

    @CachePut(key = "#user.userId")
    public User addUser(final User user) {
        LOGGER.warn("Cache add for userId {}", user.getUserId());
        LOGGER.debug("Added user {} to cache (key=userId)", user);
        return user;
    }
}
