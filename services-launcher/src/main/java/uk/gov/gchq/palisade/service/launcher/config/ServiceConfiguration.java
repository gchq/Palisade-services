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

import java.util.Objects;

public class ServiceConfiguration {
    private String name;
    private String target;
    private String config;
    private String log;

    // Default constructor
    public ServiceConfiguration() {
    }

    // Copy constructor
    public ServiceConfiguration(final ServiceConfiguration copy) {
        this.name = copy.name;
        this.target = copy.target;
        this.config = copy.config;
        this.log = copy.log;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(final String target) {
        this.target = target;
    }

    public String getConfig() {
        return config;
    }

    public void setConfig(final String config) {
        this.config = config;
    }

    public String getLog() {
        return log;
    }

    public void setLog(final String log) {
        this.log = log;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ServiceConfiguration)) {
            return false;
        }
        final ServiceConfiguration that = (ServiceConfiguration) o;
        return Objects.equals(getName(), that.getName()) &&
                Objects.equals(getTarget(), that.getTarget());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName(), getTarget());
    }
}
