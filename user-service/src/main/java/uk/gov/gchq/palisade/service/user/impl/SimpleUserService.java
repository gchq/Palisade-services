/*
 * Copyright 2018 Crown Copyright
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

package uk.gov.gchq.palisade.service.user.impl;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.gchq.palisade.User;
import uk.gov.gchq.palisade.exception.NoConfigException;
import uk.gov.gchq.palisade.jsonserialisation.JSONSerialiser;
import uk.gov.gchq.palisade.service.ServiceState;
import uk.gov.gchq.palisade.service.palisade.request.AddCacheRequest;
import uk.gov.gchq.palisade.service.palisade.request.GetCacheRequest;
import uk.gov.gchq.palisade.service.palisade.service.CacheService;
import uk.gov.gchq.palisade.service.user.UserService;
import uk.gov.gchq.palisade.service.user.exception.NoSuchUserIdException;
import uk.gov.gchq.palisade.service.user.request.AddUserRequest;
import uk.gov.gchq.palisade.service.user.request.GetUserRequest;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

/**
 * A SimpleUserService is a simple implementation of a {@link UserService} that keeps user data in the cache service.
 */
public class SimpleUserService implements UserService {
    public static final String CACHE_IMPL_KEY = "user.svc.hashmap.cache.svc";
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleUserService.class);
    /**
     * Cache service that user service can use.
     */
    private CacheService cacheService;

    public SimpleUserService() {
    }

    @Override
    public void applyConfigFrom(final ServiceState config) throws NoConfigException {
        requireNonNull(config, "config");
        //extract cache
        String serialisedCache = config.getOrDefault(CACHE_IMPL_KEY, null);
        if (nonNull(serialisedCache)) {
            setCacheService(JSONSerialiser.deserialise(serialisedCache.getBytes(StandardCharsets.UTF_8), CacheService.class));
        } else {
            throw new NoConfigException("no cache service specified in configuration");
        }
    }

    @Override
    public void recordCurrentConfigTo(final ServiceState config) {
        requireNonNull(config, "config");
        config.put(UserService.class.getTypeName(), getClass().getTypeName());
        String serialisedCache = new String(JSONSerialiser.serialise(cacheService), StandardCharsets.UTF_8);
        config.put(CACHE_IMPL_KEY, serialisedCache);
    }

    public SimpleUserService cacheService(final CacheService cacheService) {
        requireNonNull(cacheService, "Cache service cannot be set to null.");
        this.cacheService = cacheService;
        LOGGER.debug("Configured to use cache {}", cacheService);
        return this;
    }

    public CacheService getCacheService() {
        requireNonNull(cacheService, "The cache service has not been set.");
        return cacheService;
    }

    public void setCacheService(final CacheService cacheService) {
        cacheService(cacheService);
    }

    @Override
    public CompletableFuture<User> getUser(final GetUserRequest request) {
        Objects.requireNonNull(request);
        User user = null;

        GetCacheRequest<User> temp = new GetCacheRequest<User>()
                .service(this.getClass())
                .key(request.userId.getId());

        Optional<User> cachedUser = getCacheService().get(temp).join();

        if (cachedUser.isPresent()) {
            user = cachedUser.get();
        }
        CompletableFuture<User> userCompletion = CompletableFuture.completedFuture(user);

        if (user == null) {
            userCompletion.obtrudeException(new NoSuchUserIdException(request.userId.getId()));
        }
        return userCompletion;
    }

    @Override
    public CompletableFuture<Boolean> addUser(final AddUserRequest request) {
        Objects.requireNonNull(request);
        Objects.requireNonNull(request.user);
        Objects.requireNonNull(request.user.getUserId());
        Objects.requireNonNull(request.user.getUserId().getId());
        //save to the cache service
        AddCacheRequest<User> addCacheRequest = new AddCacheRequest<User>()
                .service(this.getClass())
                .key(request.user.getUserId().getId())
                .value(request.user);
        getCacheService().add(addCacheRequest).join();
        return CompletableFuture.completedFuture(true);
    }
}
