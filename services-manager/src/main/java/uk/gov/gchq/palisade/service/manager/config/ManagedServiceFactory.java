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

package uk.gov.gchq.palisade.service.manager.service;

import uk.gov.gchq.palisade.service.manager.web.ManagedClient;
import uk.gov.gchq.palisade.service.manager.web.ManagedClientFactory;

import java.net.URI;
import java.util.function.Supplier;

public class ManagedServiceFactory {

    private final ManagedClientFactory clientFactory;

    public ManagedServiceFactory(final ManagedClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    public ManagedService construct(final String serviceName, final Supplier<URI> uriSupplier) {
        ManagedClient client = clientFactory.construct(serviceName, serviceName);
        return new ManagedService( client, uriSupplier);
    }

}
