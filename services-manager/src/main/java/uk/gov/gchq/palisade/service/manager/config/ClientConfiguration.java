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

package uk.gov.gchq.palisade.service.manager.config;

import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.shared.Application;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.netflix.eureka.EurekaServiceInstance;

import uk.gov.gchq.palisade.Generated;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

public class ClientConfiguration {
    private Map<String, List<URI>> client;

    @Autowired(required = false)
    private EurekaClient eurekaClient;

    @Generated
    public Map<String, List<URI>> getClient() {
        return client;
    }

    @Generated
    public void setClient(final Map<String, List<URI>> client) {
        requireNonNull(client);
        this.client = client;
    }

    public Collection<URI> getClientUri(final String serviceName) {
        requireNonNull(serviceName);
        // If possible, use eureka
        // Otherwise, fall back to config yaml
        return eurekaResolve(serviceName)
                .orElseGet(() -> configResolve(serviceName));
    }

    private Collection<URI> configResolve(final String serviceName) {
        return client.get(serviceName);
    }

    private Optional<Collection<URI>> eurekaResolve(final String serviceName) {
        // If eureka is available
        return Optional.ofNullable(eurekaClient)
                // Get all registered applications
                .map(eureka -> eureka.getApplications().getRegisteredApplications().stream()
                        .map(Application::getInstances)
                        .flatMap(List::stream)
                        // If any config values match a service's appName (spring.application.name)
                        .filter(instance -> client.get(serviceName).stream().anyMatch(uri -> uri.toString().equalsIgnoreCase(instance.getAppName())))
                        .map(EurekaServiceInstance::new)
                        // Get the URI for this service
                        .map(EurekaServiceInstance::getUri)
                        // Return a collection of URIs that match the serviceName
                        .collect(Collectors.toList()));
    }
}
