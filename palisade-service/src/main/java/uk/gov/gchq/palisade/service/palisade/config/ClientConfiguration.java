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

package uk.gov.gchq.palisade.service.palisade.config;

import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.shared.Application;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.netflix.eureka.EurekaServiceInstance;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public class ClientConfiguration {
    private Map<String, URI> availableServices;

    @Autowired(required = false)
    private EurekaClient eurekaClient;

    public ClientConfiguration() {
    }

    public Map<String, URI> getAvailableServices() {
        return availableServices;
    }

    public void setAvailableServices(final Map<String, URI> availableServices) {
        this.availableServices = availableServices;
    }

    public Optional<URI> getClientUri(final String serviceName) {
        Optional<URI> configuredUri = Optional.ofNullable(availableServices.get(serviceName));
        return configuredUri.or(() -> eurekaResolve(serviceName).findAny());
    }

    private Stream<URI> eurekaResolve(final String serviceName) {
        return Optional.ofNullable(eurekaClient).map(client -> client.getApplications().getRegisteredApplications().stream()
                .map(Application::getInstances)
                .flatMap(List::stream)
                .filter(instance -> instance.getAppName().equalsIgnoreCase(serviceName))
                .map(EurekaServiceInstance::new)
                .map(EurekaServiceInstance::getUri))
                .orElse(Stream.empty());
    }
}
