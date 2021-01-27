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

package uk.gov.gchq.palisade.service.audit.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import uk.gov.gchq.palisade.Generated;

import java.util.Collections;
import java.util.List;

/**
 * Configuration class for the Audit Service. Used to define the directory for any error files
 */
@ConfigurationProperties("audit")
public final class AuditServiceConfigProperties {

    private List<String> implementations;
    private String errorDirectory;

    private AuditServiceConfigProperties() {
    }

    @Generated
    public String getErrorDirectory() {
        return errorDirectory;
    }

    @Generated
    public void setErrorDirectory(final String errorDirectory) {
        this.errorDirectory = errorDirectory;
    }

    @Generated
    public List<String> getImplementations() {
        return Collections.unmodifiableList(implementations);
    }

    @Generated
    public void setImplementations(final List<String> implementations) {
        this.implementations = Collections.unmodifiableList(implementations);
    }
}
