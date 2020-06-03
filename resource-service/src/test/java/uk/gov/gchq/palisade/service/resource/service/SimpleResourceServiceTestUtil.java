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
package uk.gov.gchq.palisade.service.resource.service;

import org.springframework.cloud.client.ServiceInstance;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * Set of methods and classes used in construction of integration tests in this package.
 */
public class SimpleResourceServiceTestUtil {

    static List<ServiceInstance> listTestServiceInstance(final String[] services) {
        List<ServiceInstance> listServiceInstance = new ArrayList<>();
        for (String service : services) {
            listServiceInstance.add(new TestServiceInstance(service));
        }
        return listServiceInstance;
    }

    static class TestServiceInstance implements ServiceInstance {

        private String serviceId = "Empty string";

        // Empty constructor for JSON instantiation
        TestServiceInstance() {
        }

        TestServiceInstance(final String serviceId) {
            this.serviceId = serviceId;
        }

        @Override
        public String getServiceId() {
            return serviceId;
        }

        public void setServiceId(final String serviceID) {
            this.serviceId = serviceID;
        }

        @Override
        public String getHost() {
            return null;
        }

        @Override
        public int getPort() {
            return 0;
        }

        @Override
        public boolean isSecure() {
            return false;
        }

        @Override
        public URI getUri() {
            return URI.create(serviceId);
        }

        @Override
        public Map<String, String> getMetadata() {
            return null;
        }
    }

}
