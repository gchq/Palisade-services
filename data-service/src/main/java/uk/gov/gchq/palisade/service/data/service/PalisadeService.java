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
package uk.gov.gchq.palisade.service.data.service;

import feign.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.gov.gchq.palisade.service.Service;
import uk.gov.gchq.palisade.service.data.request.GetDataRequestConfig;
import uk.gov.gchq.palisade.service.data.web.PalisadeClient;
import uk.gov.gchq.palisade.service.request.DataRequestConfig;

import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

public class PalisadeService implements Service {

    private static final Logger LOGGER = LoggerFactory.getLogger(PalisadeService.class);
    private final PalisadeClient client;
    private final Supplier<URI> uriSupplier;
    private final Executor executor;

    public PalisadeService(final PalisadeClient palisadeClient, final Supplier<URI> uriSupplier, final Executor executor) {
        this.client = palisadeClient;
        this.uriSupplier = uriSupplier;
        this.executor = executor;
    }

    CompletableFuture<DataRequestConfig> getDataRequestConfig(final GetDataRequestConfig request) {
        LOGGER.info("Getting config from palisade service for data request: {}", request);

        CompletionStage<DataRequestConfig> config;
        try {
            LOGGER.info("User request: {}", request);
            config = CompletableFuture.supplyAsync(() -> {
                URI clientUri = this.uriSupplier.get();
                LOGGER.debug("Using client uri: {}", clientUri);
                DataRequestConfig requestConfig = this.client.getDataRequestConfig(clientUri, request);
                LOGGER.info("Got config from palisade service: {}", requestConfig);
                return requestConfig;
            }, this.executor);
        } catch (Exception ex) {
            LOGGER.error("Failed to get data request config: {}", ex.getMessage());
            throw new RuntimeException(ex); //rethrow the exception
        }

        return config.toCompletableFuture();
    }

    public Response getHealth() {
        try {
            URI clientUri = this.uriSupplier.get();
            LOGGER.debug("Using client uri: {}", clientUri);
            return this.client.getHealth(clientUri);
        } catch (Exception ex) {
            LOGGER.error("Failed to get health: {}", ex.getMessage());
            throw new RuntimeException(ex); //rethrow the exception
        }
    }

}
