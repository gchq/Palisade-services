/*
 * Copyright 2018-2021 Crown Copyright
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


import uk.gov.gchq.palisade.service.manager.common.Generated;

import java.net.URI;
import java.util.List;
import java.util.Map;

import static java.util.Objects.requireNonNull;

/**
 * Client Configuration is used to resolve service URIs from service names for classes such as
 * {@link uk.gov.gchq.palisade.service.manager.service.ManagedService} to monitor application health.
 */
public class ClientConfiguration {
    private Map<String, List<URI>> client;

    @Generated
    public Map<String, List<URI>> getClient() {
        return client;
    }

    @Generated
    public void setClient(final Map<String, List<URI>> client) {
        requireNonNull(client);
        this.client = client;
    }
}
