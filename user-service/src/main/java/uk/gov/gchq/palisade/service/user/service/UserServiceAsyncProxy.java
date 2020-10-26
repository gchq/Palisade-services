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

import uk.gov.gchq.palisade.User;
import uk.gov.gchq.palisade.service.user.model.UserRequest;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * An asynchronous service for processing a cacheable method.
 */
public class UserServiceAsyncProxy {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserServiceAsyncProxy.class);
    private final Executor executor;
    private final UserService service;

    /**
     * Constructor for the {@link UserServiceAsyncProxy}
     *
     * @param service            the {@link UserService} implementation
     * @param executor           the {@link Executor} for the service
     */
    public UserServiceAsyncProxy(final UserService service,
                                 final Executor executor) {
        this.service = service;
        this.executor = executor;
    }

    public CompletableFuture<User> getUser(final UserRequest userRequest) {
        LOGGER.info("Getting user '{}' from cache", userRequest.getUserId());
        return CompletableFuture.supplyAsync(() -> service.getUser(userRequest.userId), executor);
    }

    public User addUser(final User user) {
        LOGGER.info("Adding user '{}' to cache", user.getUserId().getId());
        return service.addUser(user);
    }
}
