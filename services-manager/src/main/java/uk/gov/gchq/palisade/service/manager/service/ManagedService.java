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

package uk.gov.gchq.palisade.service.manager.service;

import feign.Response;
import feign.RetryableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.gov.gchq.palisade.service.Service;
import uk.gov.gchq.palisade.service.manager.web.ManagedClient;

import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Supplier;

public class ManagedService implements Service {

    private static final Logger LOGGER = LoggerFactory.getLogger(ManagedService.class);
    private final ManagedClient managedClient;
    private final Supplier<Collection<URI>> uriSupplier;

    public ManagedService(final ManagedClient managedClient, final Supplier<Collection<URI>> uriSupplier) {
        this.managedClient = managedClient;
        this.uriSupplier = uriSupplier;
    }

    public boolean isHealthy() {
        Collection<URI> clientUris = this.uriSupplier.get();
        return clientUris.stream()
                .map(clientUri -> {
                    int status = 404;
                    try {
                        status = this.managedClient.getHealth(clientUri).status();
                    } catch (RetryableException ex) {
                        // Not up yet
                    }
                    LOGGER.debug("Client uri {} has status {}", clientUri, status);
                    return status;
                })
                // Could be anyMatch, as only one healthy service is needed to perform requests
                // Could be allMatch, as it should be expected that all services are healthy
                // Note that in the case of an empty list, this should always return false
                .anyMatch(x -> x == 200);
    }

    public void setLoggers(final String module, final String configuredLevel) throws Exception {
        Collection<URI> clientUris = this.uriSupplier.get();
        Optional<Response> failures = clientUris.stream()
                .map(clientUri -> {
                    Response response = this.managedClient.setLoggers(clientUri, module, configuredLevel);
                    LOGGER.debug("Client uri {} responded with {}", clientUri, response);
                    return response;
                })
                .filter(x -> x.status() != 200)
                .findAny();
        // Need to throw an error, so can't wrap inside an Optional.ifPresent
        if (failures.isPresent()) {
            Response response = failures.get();
            LOGGER.error("An error occurred while setting logging levels: {}", response);
            String responseBody = Arrays.toString(response.body().asInputStream().readAllBytes());
            throw new Exception(String.format("Expected /actuator/loggers/%s %s -> 200 OK but instead was %s", module, configuredLevel, responseBody));
        }
    }

    public void shutdown() {
        Collection<URI> clientUris = this.uriSupplier.get();
        clientUris.forEach(this.managedClient::shutdown);
    }

}
