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
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import uk.gov.gchq.palisade.User;
import uk.gov.gchq.palisade.service.user.request.AddUserRequest;
import uk.gov.gchq.palisade.service.user.request.GetUserRequest;
import uk.gov.gchq.palisade.service.user.service.UserService;

import javax.annotation.PostConstruct;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

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
        return service.getUser(request.userId);
    }

    @PostMapping(value = "/addUser", consumes = "application/json", produces = "application/json")
    public Boolean addUserRequest(@RequestBody final AddUserRequest request) {
        LOGGER.info("Invoking AddUserRequest: {}", request);
        return service.addUser(request.user).equals(request.user);
    }

    @PostConstruct
    public void initPostConstruct() {
        Resource resource = new ClassPathResource("users.txt");
        try {
            InputStream inputStream = resource.getInputStream();
            InputStreamReader isr = new InputStreamReader(inputStream);
            BufferedReader br = new BufferedReader(isr);
            String line;
            while ((line = br.readLine()) != null) {
                User newUser = new User().userId(line);
                service.addUser(newUser);
            }
        } catch (IOException e) {
            LOGGER.error("IOException", e);
        } catch (RuntimeException e) {
            LOGGER.error("Failed to add user to {}: {}", service.getClass(), e);
        }
    }
}
