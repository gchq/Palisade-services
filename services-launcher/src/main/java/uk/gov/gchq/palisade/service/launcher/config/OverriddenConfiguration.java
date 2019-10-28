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

import static java.util.Objects.requireNonNull;

public class OverriddenConfiguration extends ServiceConfiguration {
    private DefaultsConfiguration configuration;

    public OverriddenConfiguration defaults(final DefaultsConfiguration defaultsConfiguration) {
        this.configuration = defaultsConfiguration;
        return this;
    }

    @Override
    public String getService() {
        requireNonNull(configuration);
        return super.getService() != null ? super.getService() : configuration.name(getName()).getService();
    }

    @Override
    public String getTarget() {
        requireNonNull(configuration);
        return super.getTarget() != null ? super.getTarget() : configuration.name(getName()).getTarget();
    }

    @Override
    public String getConfig() {
        requireNonNull(configuration);
        return super.getConfig() != null ? super.getConfig() : configuration.name(getName()).getConfig();
    }

    @Override
    public String getLog() {
        requireNonNull(configuration);
        return super.getLog() != null ? super.getLog() : configuration.name(getName()).getLog();
    }
}
