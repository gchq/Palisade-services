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
package uk.gov.gchq.palisade.service.manager.service;

import feign.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.gov.gchq.palisade.service.Service;
import uk.gov.gchq.palisade.service.manager.web.UserClient;

import java.net.URI;
import java.util.function.Supplier;

public class UserService implements Service {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserService.class);
    private final UserClient userClient;
    private final Supplier<URI> uriSupplier;

    public UserService(final UserClient userClient, final Supplier<URI> uriSupplier) {
        this.userClient = userClient;
        this.uriSupplier = uriSupplier;
    }

    public Response getHealth() {
        try {
            URI clientUri = this.uriSupplier.get();
            LOGGER.debug("Using client uri: {}", clientUri);
            return this.userClient.getHealth(clientUri);
        } catch (Exception ex) {
            LOGGER.error("Failed to get health: {}", ex.getMessage());
            throw new RuntimeException(ex); //rethrow the exception
        }
    }

}
