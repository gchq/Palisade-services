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

public class ServiceConfiguration {

    private String classpath;
    private String path;
    private String log;
    private String profiles;

    public String getClasspath() {
        return classpath;
    }

    public void setClasspath(final String classpath) {
        this.classpath = classpath;
    }

    public String getProfiles() {
        return profiles;
    }

    public void setProfiles(final String profiles) {
        this.profiles = profiles;
    }

    public String getLog() {
        return log;
    }

    public void setLog(final String log) {
        this.log = log;
    }

    public String getPath() {
        return path;
    }

    public void setPath(final String path) {
        this.path = path;
    }
}
