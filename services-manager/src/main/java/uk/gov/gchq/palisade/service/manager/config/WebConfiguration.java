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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import uk.gov.gchq.palisade.Generated;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

public class WebConfiguration {
    private static final Logger LOGGER = LoggerFactory.getLogger(WebConfiguration.class);

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
        return Optional.ofNullable(client.get(serviceName)).orElse(Collections.emptyList());
    }

    private Optional<Collection<URI>> eurekaResolve(final String serviceName) {
        // If eureka is available
        return Optional.ofNullable(eurekaClient)
                // Get all registered applications
                .map(eureka -> eureka.getApplications().getRegisteredApplications().stream()
                        .map(Application::getInstances)
                        .flatMap(List::stream)
                        .peek(instance -> LOGGER.debug("Found instance: {}", instance.getAppName()))
                        // If any config values match a service's appName (spring.application.name)
                        .filter(instance -> Optional.ofNullable(client.get(serviceName)).stream().flatMap(List::stream)
                                .anyMatch(uri -> uri.toString().equalsIgnoreCase(instance.getAppName())))
                        .map(instance -> {
                            try {
                                return new URI(String.format("http://%s:%s", instance.getHostName(), instance.getPort()));
                            } catch (URISyntaxException e) {
                                LOGGER.error("There was an error ", e);
                                return null;
                            }
                        })
                        // Return a collection of URIs that match the serviceName
                        .collect(Collectors.toList()));
    }
}
