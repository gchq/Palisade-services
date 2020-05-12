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

package uk.gov.gchq.palisade.service.data.config;

import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.shared.Application;
import org.springframework.cloud.netflix.eureka.EurekaServiceInstance;

import uk.gov.gchq.palisade.Generated;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
* A generic resolver from service names to {@link URI}s
* Uses Eureka if available, otherwise uses the Spring yaml configuration value directly as a URI (useful for k8s)
 */
public class ClientConfiguration {
    private Map<String, URI> client;

    private final Optional<EurekaClient> eurekaClient;

    /**
     * Default constructor with an {@link Optional} {@link EurekaClient} depending on whether this is
     * an eureka-enabled environment or not.
     *
     * @param eurekaClient the eureka client to (maybe) use to resolve {@link URI}s for service names
     */
    public ClientConfiguration(final Optional<EurekaClient> eurekaClient) {
        this.eurekaClient = eurekaClient;
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
        return eurekaClient
                // If possible, use eureka
                .map(x -> eurekaResolve(serviceName))
                // Otherwise, fall back to config yaml
                .orElseGet(() -> configResolve(serviceName));
    }

    private Optional<URI> configResolve(final String serviceName) {
        return Optional.ofNullable(client.get(serviceName));
    }

    private Optional<URI> eurekaResolve(final String serviceName) {
        return eurekaClient
                .flatMap(eureka -> eureka.getApplications().getRegisteredApplications().stream()
                        .map(Application::getInstances)
                        .flatMap(List::stream)
                        .filter(instance -> instance.getAppName().equalsIgnoreCase(client.get(serviceName).toString()))
                        .map(EurekaServiceInstance::new)
                        .map(EurekaServiceInstance::getUri)
                        .findAny());
    }
}
