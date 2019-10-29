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
    private DefaultsConfiguration defaultConfig;

    // Default constructor
    public OverriddenConfiguration() {
    }

    // Copy constructor
    public OverriddenConfiguration(OverriddenConfiguration copy) {
        super(copy);
        this.defaultConfig = copy.defaultConfig;
    }

    public OverriddenConfiguration defaults(final DefaultsConfiguration defaultsConfiguration) {
        this.defaultConfig = defaultsConfiguration;
        return this;
    }

    @Override
    public String getTarget() {
        requireNonNull(defaultConfig);
        return super.getTarget() != null ? super.getTarget() : defaultConfig.name(getName()).getTarget();
    }

    @Override
    public String getConfig() {
        requireNonNull(defaultConfig);
        return super.getConfig() != null ? super.getConfig() : defaultConfig.name(getName()).getConfig();
    }

    @Override
    public String getLog() {
        requireNonNull(defaultConfig);
        return super.getLog() != null ? super.getLog() : defaultConfig.name(getName()).getLog();
    }
}
