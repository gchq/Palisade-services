/*
 * Copyright 2019 Crown Copyright
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
package uk.gov.gchq.palisade.service.user.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import uk.gov.gchq.palisade.User;
import uk.gov.gchq.palisade.service.user.request.AddUserRequest;
import uk.gov.gchq.palisade.service.user.request.GetUserRequest;
import uk.gov.gchq.palisade.service.user.service.UserService;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping(path = "/")
public class UserController {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserController.class);
    private UserService service;

    public UserController(final UserService service) {
        this.service = service;
    }

    @PostMapping(value = "/getUser", consumes = "application/json", produces = "application/json")
    public User getUserRequest(@RequestBody final GetUserRequest request) {
        LOGGER.info("Invoking GetUserRequest: {}", request);
        return this.getUser(request).join();
    }

    public CompletableFuture<User> getUser(final GetUserRequest request) {
        LOGGER.debug("Getting User: {}", request);
        return service.getUser(request);
    }

    @PostMapping(value = "/addUser", consumes = "application/json", produces = "application/json")
    public Boolean addUserRequest(@RequestBody final AddUserRequest request) {
        LOGGER.info("Invoking AddUserRequest: {}", request);
        return this.addUser(request).join();
    }

    public CompletableFuture<Boolean> addUser(final AddUserRequest request) {
        LOGGER.info("Adding User: {}", request);
        return service.addUser(request);
    }

}
