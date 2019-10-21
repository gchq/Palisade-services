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
package uk.gov.gchq.palisade.service.resource.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration properties for cache selection and configuration
 */
@ConfigurationProperties(prefix = "cache")
public class CacheConfiguration {

    private String implementation;
    private String props;
    private List<String> etcd = new ArrayList<>();

    /**
     * The backing store implementation type
     *
     * @return implementation
     */
    public String getImplementation() {
        return implementation;
    }

    public void setImplementation(final String implementation) {
        this.implementation = implementation;
    }

    public String getProps() {
        return props;
    }

    public void setProps(final String props) {
        this.props = props;
    }

    public List<String> getEtcd() {
        return etcd;
    }

    public void setEtcd(final List<String> etcd) {
        this.etcd = etcd;
    }
}
