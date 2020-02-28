/*
 * Copyright 2020 Crown Copyright
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

package uk.gov.gchq.palisade.service.manager.runner;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.shared.Application;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

class EurekaUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(EurekaUtils.class);

    @Autowired(required = false)
    private EurekaClient eurekaClient;

    protected EurekaClient getEurekaClient() {
        return this.eurekaClient;
    }

    protected List<InstanceInfo> getRunningServices() {
        if (Objects.nonNull(eurekaClient)) {
            LOGGER.debug("Getting InstanceInfo from EurekaClient");
            return eurekaClient.getApplications().getRegisteredApplications().stream()
                    .map(Application::getInstances)
                    .flatMap(List::stream)
                    .peek(instance -> LOGGER.debug("Discovered {} :: {}:{}/{} ({})", instance.getAppName(), instance.getIPAddr(), instance.getPort(), instance.getSecurePort(), instance.getStatus()))
                    .collect(Collectors.toList());
        } else {
            LOGGER.debug("EurekaClient is null - is the discovery service running?");
            return Collections.emptyList();
        }
    }
}
