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

package uk.gov.gchq.palisade.service.launcher.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import static java.util.Objects.requireNonNull;

@ConfigurationProperties(prefix = "launcher.defaults")
public class DefaultsConfiguration extends ServiceConfiguration {

    public DefaultsConfiguration() {
    }

    public DefaultsConfiguration(final ServiceConfiguration copy) {
        super(copy);
    }

    public DefaultsConfiguration name(final String name) {
        setName(name);
        return this;
    }

    @Override
    public String getTarget() {
        requireNonNull(getName());
        return super.getTarget().replace("SERVICE", getName());
    }

    @Override
    public String getConfig() {
        requireNonNull(getName());
        return super.getConfig().replace("SERVICE", getName());
    }

    @Override
    public String getLog() {
        requireNonNull(getName());
        return super.getLog().replace("SERVICE", getName());
    }
}
