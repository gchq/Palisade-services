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

import uk.gov.gchq.palisade.service.audit.common.Generated;

/**
 * webMVC config properties class used when configuring WebMVC as an executor when launching the service
 */
@ConfigurationProperties(prefix = "async")
public class AsyncConfigProperties {
    private int webMvcTimeout;
    private int corePoolSize;

    /**
     * Returns the web mvc timeout
     *
     * @return the web mvc timeout
     */
    @Generated
    public int getWebMvcTimeout() {
        return webMvcTimeout;
    }

    /**
     * Sets the web mvc timeout
     *
     * @param webMvcTimeout The new value to set
     */
    @Generated
    public void setWebMvcTimeout(final int webMvcTimeout) {
        this.webMvcTimeout = webMvcTimeout;
    }

    /**
     * Returns the core pool size
     *
     * @return the core pool size
     */
    @Generated
    public int getCorePoolSize() {
        return corePoolSize;
    }

    /**
     * Sets the new core pool size
     *
     * @param corePoolSize the new core pool size
     */
    @Generated
    public void setCorePoolSize(final int corePoolSize) {
        this.corePoolSize = corePoolSize;
    }
}
