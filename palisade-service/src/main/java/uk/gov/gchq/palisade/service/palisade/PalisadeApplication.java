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

package uk.gov.gchq.palisade.service.palisade;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

@EnableEurekaClient
@EnableFeignClients
@SpringBootApplication
public class PalisadeApplication {
    private static final Logger LOGGER = LoggerFactory.getLogger(PalisadeApplication.class);

    @Autowired
    DiscoveryClient client;

    @RequestMapping("/")
    public String initMeth() {
        List<ServiceInstance> instances = client.getInstances("user-service");
        ServiceInstance selectedInstance = instances
                .get(new Random().nextInt(instances.size()));
        return "Hello World: " + selectedInstance.getServiceId() + ":" + selectedInstance
                .getHost() + ":" + selectedInstance.getPort() + ":" + selectedInstance.getUri();
    }

    public static void main(final String[] args) {
        LOGGER.debug("PalisadeApplication started with: {}", PalisadeApplication.class.toString(), "main", Arrays.toString(args));

        new SpringApplicationBuilder(PalisadeApplication.class).web(WebApplicationType.SERVLET)
                .run(args);
    }

}

