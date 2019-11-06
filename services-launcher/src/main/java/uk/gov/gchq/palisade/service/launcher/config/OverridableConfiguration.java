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

public class OverridableConfiguration extends ServiceConfiguration {
    private DefaultsConfiguration defaultConfig;

    // Default constructor
    public OverridableConfiguration() {
    }

    // Copy constructor
    public OverridableConfiguration(final OverridableConfiguration copy) {
        super(copy);
        this.defaultConfig = copy.defaultConfig;
    }

    public OverridableConfiguration defaults(final DefaultsConfiguration defaultsConfiguration) {
        this.defaultConfig = defaultsConfiguration;
        return this;
    }

    @Override
    public String getTarget() {
        return super.getTarget() != null || defaultConfig == null ? super.getTarget() : defaultConfig.name(getName()).getTarget();
    }

    @Override
    public String getConfig() {
        return super.getConfig() != null || defaultConfig == null ? super.getConfig() : defaultConfig.name(getName()).getConfig();
    }

    @Override
    public String getProfiles() {
        return super.getProfiles() != null || defaultConfig == null ? super.getProfiles() : defaultConfig.name(getName()).getProfiles();
    }

    @Override
    public String getLog() {
        return super.getLog() != null || defaultConfig == null ? super.getLog() : defaultConfig.name(getName()).getLog();
    }

}
