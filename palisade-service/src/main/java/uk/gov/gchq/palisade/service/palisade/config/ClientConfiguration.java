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

public class ClientConfiguration {
    private Map<String, URI> client;

    @Autowired(required = false)
    private EurekaClient eurekaClient;

    public Map<String, URI> getClient() {
        return client;
    }

    public void setClient(final Map<String, URI> client) {
        this.client = client;
    }

    public Optional<URI> getClientUri(final String serviceName) {
        // If possible, use eureka
        // Otherwise, fall back to config yaml
        return eurekaResolve(serviceName)
                .or(() -> configResolve(serviceName));
    }

    private Optional<URI> configResolve(final String serviceName) {
        return Optional.ofNullable(client.get(serviceName));
    }

    private Optional<URI> eurekaResolve(final String serviceName) {
        return Optional.ofNullable(eurekaClient).flatMap(eureka -> eureka.getApplications().getRegisteredApplications().stream()
                .map(Application::getInstances)
                .flatMap(List::stream)
                .filter(instance -> instance.getAppName().equalsIgnoreCase(serviceName))
                .map(EurekaServiceInstance::new)
                .map(EurekaServiceInstance::getUri)
                .findAny());
    }
}
