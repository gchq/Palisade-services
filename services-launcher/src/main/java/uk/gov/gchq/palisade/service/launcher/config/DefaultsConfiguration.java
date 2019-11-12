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

@ConfigurationProperties(prefix = "launcher.default-service")
public class DefaultsConfiguration extends ServiceConfiguration {
    private String root;

    public DefaultsConfiguration() {
    }

    public DefaultsConfiguration(final ServiceConfiguration copy) {
        super(copy);
    }

    public DefaultsConfiguration name(final String name) {
        setName(name);
        return this;
    }

    public String getRoot() {
        return root;
    }

    public void setRoot(final String root) {
        this.root = root;
    }

    @Override
    public String getTarget() {
        return getName() == null || super.getTarget() == null ? super.getTarget() : super.getTarget().replace("SERVICE", getName());
    }

    @Override
    public String getConfig() {
        return getName() == null || super.getConfig() == null ? super.getConfig() : super.getConfig().replace("SERVICE", getName());
    }

    @Override
    public String getProfiles() {
        return getName() == null || super.getProfiles() == null ? super.getProfiles() : super.getProfiles().replace("SERVICE", getName());
    }

    @Override
    public String getLog() {
        return  getName() == null || super.getLog() == null ? super.getLog() : super.getLog().replace("SERVICE", getName());
    }

}
