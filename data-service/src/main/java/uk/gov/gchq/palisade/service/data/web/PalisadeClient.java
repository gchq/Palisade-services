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
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import uk.gov.gchq.palisade.service.data.request.GetDataRequestConfig;
import uk.gov.gchq.palisade.service.request.DataRequestConfig;

public interface PalisadeClient {
    @PostMapping(path = "/getDataRequestConfig", consumes = "application/json", produces = "application/json")
    DataRequestConfig getDataRequestConfig(@RequestBody final GetDataRequestConfig request);


    @Profile("eureka")
    @FeignClient(name = "palisade-service")
    interface EurekaAuditClient extends AuditClient { }

    @Profile("!eureka")
    @FeignClient(name = "palisade-service", url = "${web.client.palisade-service}")
    interface SimpleAuditClient extends AuditClient { }
}
