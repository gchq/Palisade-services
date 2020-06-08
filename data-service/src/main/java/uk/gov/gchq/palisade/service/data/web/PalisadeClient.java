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
package uk.gov.gchq.palisade.service.data.web;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import uk.gov.gchq.palisade.service.data.request.GetDataRequestConfig;
import uk.gov.gchq.palisade.service.request.DataRequestConfig;

/**
 * The interface Palisade client which uses Feign to either resolve services called palisade-service or looks up a url specified in the relevant profiles yaml.
 */
@FeignClient(name = "palisade-service", url = "${web.client.palisade-service}")
public interface PalisadeClient {

    /**
     * Gets data request config.
     *
     * @param request the request
     * @return the data request config
     */
    @PostMapping(path = "/getDataRequestConfig", consumes = "application/json", produces = "application/json")
    DataRequestConfig getDataRequestConfig(@RequestBody final GetDataRequestConfig request);

}
