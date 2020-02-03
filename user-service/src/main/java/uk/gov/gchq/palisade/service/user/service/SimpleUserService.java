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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static java.util.Objects.requireNonNull;

/**
 * A SimpleUserService is a simple implementation of a {@link UserService} that keeps user data in the cache service.
 */
public class SimpleUserService implements UserService {
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleUserService.class);
    private Map<UserId, User> mapmap = new SlowMap<>();

    public SimpleUserService() {
        Resource resource = new ClassPathResource("users.txt");
        try {
            InputStream inputStream = resource.getInputStream();
            InputStreamReader isr = new InputStreamReader(inputStream);
            BufferedReader br = new BufferedReader(isr);
            String line;
            while ((line = br.readLine()) != null) {
                User newUser = new User().userId(line);
                mapmap.put(newUser.getUserId(), newUser);
                LOGGER.info("Users {} added to cache", newUser);
            }
        } catch (IOException e) {
            LOGGER.error("IOException", e);
        }
    }

    @Override
    public CompletableFuture<User> getUser(final GetUserRequest request) {
        LOGGER.info("Getting User : {} ", request);
        requireNonNull(request);
        User user = getUserFromCache(request.userId);
        CompletableFuture<User> userCompletion = CompletableFuture.completedFuture(user);
        if (user == null) {
            LOGGER.error("User {} not found in cache", request.userId.getId());
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
        addUserToCache(new User().userId(request.user.getUserId()));
        return CompletableFuture.completedFuture(true);
    }

    @CachePut(value = "users", key = "#user.userId")
    public User addUserToCache(final User user) {
        LOGGER.info("addUserToCache : {}", user.getUserId().getId());
        requireNonNull(user);
        return mapmap.put(user.getUserId(), user);
    }


    @Cacheable("users")
    public User getUserFromCache(final UserId key) {
        LOGGER.info("getUserFromCache : {}", key.getId());
        requireNonNull(key);
        return mapmap.get(key);
    }
}

class SlowMap<K, V> extends HashMap<K, V> {
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleUserService.class);

    @Cacheable(value = "users", key = "#key")
    public V get(final Object key) {
        try {
            Thread.sleep(1000);
            LOGGER.info("Stuff did things slowly {} ", key);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return super.get(key);
    }

    @CachePut(value = "users", key = "#key")
    public V put(final K key, final V value) {
        try {
            Thread.sleep(1000);
            LOGGER.info("Stuff did things slowly in this put {}, {} ", key, value);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return super.put(key, value);
    }
}
