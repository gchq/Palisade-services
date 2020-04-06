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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.openfeign.FeignClientBuilder;
import org.springframework.context.ApplicationContext;

import uk.gov.gchq.palisade.service.manager.service.ManagedService;
import uk.gov.gchq.palisade.service.manager.web.ManagedClient;

import java.net.URI;
import java.util.Collection;
import java.util.function.Supplier;

public class ManagedServiceFactory {

    private final ManagedClientFactory clientFactory;

    public ManagedServiceFactory() {
        this.clientFactory = new ManagedClientFactory();
    }

    public ManagedService construct(final String serviceName, final Supplier<Collection<URI>> uriSupplier) {
        ManagedClient client = clientFactory.construct(serviceName, serviceName);
        return new ManagedService(client, uriSupplier);
    }

    static class ManagedClientFactory {

        @Autowired
        private ApplicationContext applicationContext;

        public ManagedClient construct(final String name, final String defaultUrl) {
            return new FeignClientBuilder(applicationContext)
                    .forType(ManagedClient.class, name)
                    .url(defaultUrl)
                    .build();
        }

    }

}
