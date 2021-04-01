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

package uk.gov.gchq.palisade.service.resource.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import uk.gov.gchq.palisade.service.resource.common.Generated;

import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;

/**
 * ResourceService class containing default config properties about the resource service which is used when launching the service
 */
@ConfigurationProperties(prefix = "resource")
public class ResourceServiceConfigProperties {
    private String implementation;
    private String defaultType;

    @Generated
    public String getImplementation() {
        return implementation;
    }

    @Generated
    public void setImplementation(final String implementation) {
        this.implementation = Optional.ofNullable(implementation).orElse("simple");
    }

    @Generated
    public String getDefaultType() {
        return defaultType;
    }

    @Generated
    public void setDefaultType(final String defaultType) {
        this.defaultType = Optional.ofNullable(defaultType).orElse(String.class.getName());
    }

    @Override
    @Generated
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ResourceServiceConfigProperties)) {
            return false;
        }
        final ResourceServiceConfigProperties that = (ResourceServiceConfigProperties) o;
        return Objects.equals(implementation, that.implementation) &&
                Objects.equals(defaultType, that.defaultType);
    }

    @Override
    @Generated
    public int hashCode() {
        return Objects.hash(implementation, defaultType);
    }

    @Override
    @Generated
    public String toString() {
        return new StringJoiner(", ", ResourceServiceConfigProperties.class.getSimpleName() + "[", "]")
                .add("implementation='" + implementation + "'")
                .add("defaultType='" + defaultType + "'")
                .add(super.toString())
                .toString();
    }
}
