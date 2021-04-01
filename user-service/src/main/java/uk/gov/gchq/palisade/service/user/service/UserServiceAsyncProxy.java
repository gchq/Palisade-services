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

import uk.gov.gchq.palisade.service.user.common.User;
import uk.gov.gchq.palisade.service.user.model.AuditErrorMessage;
import uk.gov.gchq.palisade.service.user.model.AuditableUserResponse;
import uk.gov.gchq.palisade.service.user.model.UserRequest;
import uk.gov.gchq.palisade.service.user.model.UserResponse;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * An asynchronous service for processing a cacheable method.
 */
public class UserServiceAsyncProxy {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserServiceAsyncProxy.class);
    private final Executor executor;
    private final UserServiceCachingProxy service;

    /**
     * Constructor for the {@link UserServiceAsyncProxy}
     *
     * @param service  the {@link UserService} implementation
     * @param executor the {@link Executor} for the service
     */
    public UserServiceAsyncProxy(final UserServiceCachingProxy service,
                                 final Executor executor) {
        this.service = service;
        this.executor = executor;
    }

    /**
     * Takes the {@link String} value of the userId from the {@link UserRequest} and attempts to get the User from the caching layer.
     * If the user is successfully retrieved then a {@link UserResponse} is created and added to the {@link AuditableUserResponse}
     * class with no error attached.
     * If any errors are thrown in the service, then the CompletableFuture.exceptionally will return a new completable future,
     * calling the {@link AuditableUserResponse}, but this time with no UserResponse, just with a {@link AuditErrorMessage} that contains
     * the original UserRequest that was passed into this method, an empty Map or attributes, and the exception that was thrown.
     * In most cases, the exception thrown will be a {@link uk.gov.gchq.palisade.service.user.exception.NoSuchUserIdException}
     *
     * @param userRequest the {@link UserRequest} containing the user ID
     * @return a {@link CompletableFuture} of the {@link User} obtained from the {@link UserService}
     */
    public CompletableFuture<AuditableUserResponse> getUser(final UserRequest userRequest) {
        LOGGER.info("Getting user '{}' from cache", userRequest.getUserId());
        return CompletableFuture.supplyAsync(() -> service.getUser(userRequest.getUserId()), executor)
                .thenApply(user -> AuditableUserResponse.Builder.create()
                        .withUserResponse(UserResponse.Builder.create(userRequest).withUser(user)))
                .exceptionally(ex -> AuditableUserResponse.Builder.create()
                        .withAuditErrorMessage(AuditErrorMessage.Builder.create(userRequest, Collections.emptyMap()).withError(ex)));
    }

    /**
     * Adds a {@link User} to the cache, via the {@link UserServiceCachingProxy}
     *
     * @param user the {@link User} to be added to the cache of the service
     * @return the {@link User} that was added to the service cache
     */
    public User addUser(final User user) {
        LOGGER.info("Adding user '{}' to cache", user.getUserId().getId());
        return service.addUser(user);
    }
}
