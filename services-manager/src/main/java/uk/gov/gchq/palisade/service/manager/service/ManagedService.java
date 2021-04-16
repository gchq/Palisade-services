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

package uk.gov.gchq.palisade.service.manager.service;

import feign.Response;
import feign.RetryableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

import uk.gov.gchq.palisade.service.manager.web.ManagedClient;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Wrapper around a Feign client to call out to a collection of URIs rather than a single REST service
 * Allows multiple instances of a service to be running and all of them to be effected by shutdown, logging changes, etc.
 */
public class ManagedService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ManagedService.class);
    private final ManagedClient managedClient;
    private final Supplier<Collection<URI>> uriSupplier;

    public ManagedService(final ManagedClient managedClient, final Supplier<Collection<URI>> uriSupplier) {
        this.managedClient = managedClient;
        this.uriSupplier = uriSupplier;
    }

    /**
     * Get whether a single service in the possible collection is healthy
     *
     * @return whether there exists a healthy service
     */
    public boolean isHealthy() {
        Collection<URI> clientUris = this.uriSupplier.get();
        return clientUris.stream()
                .map((URI clientUri) -> {
                    int status = HttpStatus.NOT_FOUND.value();
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
                .anyMatch(x -> x == HttpStatus.OK.value());
    }

    /**
     * Set the logging level for a given java package
     *
     * @param packageName     the name of the package (eg. uk.gov, root, java.util)
     * @param configuredLevel the level to log to stdout for the named package (TRACE, DEBUG, INFO, WARN, ERROR)
     * @throws IOException if any service did not report 200-OK after the REST POST request
     */
    public void setLoggers(final String packageName, final String configuredLevel) throws IOException {
        Collection<URI> clientUris = this.uriSupplier.get();
        Optional<Response> failures = clientUris.stream()
                .map((URI clientUri) -> {
                    Response response = this.managedClient.setLoggers(clientUri, packageName, configuredLevel);
                    LOGGER.debug("Client uri {} responded with {}", clientUri, response);
                    return response;
                })
                .filter(x -> x.status() != HttpStatus.OK.value())
                .findAny();
        // Need to throw an error, so can't wrap inside an Optional.ifPresent
        // Could be avoided by throwing a RuntimeException
        if (failures.isPresent()) {
            Response response = failures.get();
            LOGGER.error("An error occurred while setting logging levels: {}", response);
            String responseBody = Arrays.toString(response.body().asInputStream().readAllBytes());
            throw new IOException(String.format("Expected /actuator/loggers/%s %s -> 200 OK but instead was %s", packageName, configuredLevel, responseBody));
        }
    }

    /**
     * Requests a shutdown all running instances of the service
     * Does not verify that services have actually stopped
     */
    public void shutdown() {
        Collection<URI> clientUris = this.uriSupplier.get();
        clientUris.forEach(this.managedClient::shutdown);
    }

}
