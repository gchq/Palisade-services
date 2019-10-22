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
package uk.gov.gchq.palisade.service.palisade.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import uk.gov.gchq.palisade.service.palisade.request.GetDataRequestConfig;
import uk.gov.gchq.palisade.service.palisade.request.RegisterDataRequest;
import uk.gov.gchq.palisade.service.palisade.service.PalisadeService;
import uk.gov.gchq.palisade.service.request.DataRequestConfig;
import uk.gov.gchq.palisade.service.request.DataRequestResponse;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping(path = "/")
public class DefaultController {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultController.class);

    private final PalisadeService service;

    public DefaultController(final PalisadeService service) {
        this.service = service;
    }

    @PostMapping(value = "/registerDataRequest", consumes = "application/json", produces = "application/json")
    public DataRequestResponse registerDataRequestSync(@RequestBody final RegisterDataRequest request) {
        LOGGER.debug("Invoking registerDataRequest: {}", request);
        return this.registerDataRequest(request).join();
    }

    public CompletableFuture<DataRequestResponse> registerDataRequest(final RegisterDataRequest request) {
        return service.registerDataRequest(request);
    }

    @PostMapping(value = "/getDataRequestConfig", consumes = "application/json", produces = "application/json")
    public DataRequestConfig getDataRequestConfigSync(@RequestBody final GetDataRequestConfig request) {
        LOGGER.debug("Invoking getDataRequestConfig: {}", request);
        return getDataRequestConfig(request).join();
    }

    public CompletableFuture<DataRequestConfig> getDataRequestConfig(final GetDataRequestConfig request) {
        return this.service.getDataRequestConfig(request);
    }

}
