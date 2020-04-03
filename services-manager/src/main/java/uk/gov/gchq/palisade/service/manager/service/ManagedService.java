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

import feign.FeignException.FeignClientException;
import feign.Request;
import feign.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.gov.gchq.palisade.service.Service;
import uk.gov.gchq.palisade.service.manager.web.ManagedClient;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.function.Supplier;

public class ManagedService implements Service {

    private static final Logger LOGGER = LoggerFactory.getLogger(ManagedService.class);
    private final ManagedClient managedClient;
    private final Supplier<URI> uriSupplier;

    public ManagedService(final ManagedClient managedClient, final Supplier<URI> uriSupplier) {
        this.managedClient = managedClient;
        this.uriSupplier = uriSupplier;
    }

    public boolean isHealthy() {
        URI clientUri = this.uriSupplier.get();
        LOGGER.debug("Using client uri: {}", clientUri);
        return this.managedClient.getHealth(clientUri).status() == 200;
    }

    public void setLoggers(String module, String configuredLevel) throws Exception {
        URI clientUri = this.uriSupplier.get();
        LOGGER.debug("Using client uri: {}", clientUri);
        Response response = this.managedClient.setLoggers(clientUri, module, configuredLevel);
        if (response.status() != 200) {
            throw new Exception(String.format("Expected /actuator/loggers/%s %s -> 200 OK but instead was %s", module, configuredLevel, Arrays.toString(response.body().asInputStream().readAllBytes())));
        }
    }

}
