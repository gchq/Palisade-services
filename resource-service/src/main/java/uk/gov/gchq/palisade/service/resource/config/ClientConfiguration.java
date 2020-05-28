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

package uk.gov.gchq.palisade.service.resource.config;

import org.springframework.cloud.client.discovery.DiscoveryClient;

import uk.gov.gchq.palisade.Generated;

import java.net.URI;
import java.util.Map;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * A generic resolver from service names to {@link URI}s
 * Uses Eureka if available, otherwise uses the Spring yaml configuration value directly as a URI (useful for k8s)
 */
public class ClientConfiguration {
    private Map<String, URI> client;

    private final Optional<DiscoveryClient> discoveryClient;

    /**
     * Default constructor with an {@link Optional} {@link DiscoveryClient} depending on whether this is
     * an eureka-enabled environment or not.
     *
     * @param discoveryClient the discovery client to (maybe) use to resolve {@link URI}s for service names
     */
    public ClientConfiguration(final DiscoveryClient discoveryClient) {
        this.discoveryClient = Optional.ofNullable(discoveryClient);
    }

    @Generated
    public Map<String, URI> getClient() {
        return client;
    }

    @Generated
    public void setClient(final Map<String, URI> client) {
        requireNonNull(client);
        this.client = client;
    }

    public Optional<URI> getClientUri(final String serviceName) {
        requireNonNull(serviceName);
        return discoveryClient
                // If possible, use eureka
                .map(x -> eurekaResolve(serviceName))
                // Otherwise, fall back to config yaml
                .orElseGet(() -> configResolve(serviceName));
    }

    private Optional<URI> configResolve(final String serviceName) {
        return Optional.ofNullable(client.get(serviceName));
    }

    private Optional<URI> eurekaResolve(final String serviceName) {
        return Optional.of(URI.create(serviceName));
    }
}
