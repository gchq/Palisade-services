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
package uk.gov.gchq.palisade.service.palisade.web;

import feign.Response;
import feign.Response.Body;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;

@EnableFeignClients
@Component
public class PalisadeHealthIndicator extends AbstractHealthIndicator {
    private static final Logger LOGGER = LoggerFactory.getLogger(PalisadeHealthIndicator.class);
    private final PolicyClient policyClient;
    private final ResourceClient resourceClient;
    private final UserClient userClient;

    public PalisadeHealthIndicator(final PolicyClient policyClient, final ResourceClient resourceClient, final UserClient userClient) {
        this.policyClient = policyClient;
        this.resourceClient = resourceClient;
        this.userClient = userClient;
    }

    @Override
    protected void doHealthCheck(final Health.Builder builder) throws Exception {
        // Use the builder to build the health status details that should be reported.
        // If you throw an exception, the status will be DOWN with the exception message.
        Response policyHealth = policyClient.getHealth();
        Response resourceHealth = resourceClient.getHealth();
        Response userHealth = userClient.getHealth();
        if (policyHealth.status() != 200) {
            throw new Exception("Policy service down");
        }
        if (resourceHealth.status() != 200) {
            throw new Exception("Resource service down");
        }
        if (userHealth.status() != 200) {
            throw new Exception("User service down");
        }
        builder.up()
                .withDetail("Policy Service", readBody(policyHealth.body()))
                .withDetail("Resource Service", readBody(resourceHealth.body()))
                .withDetail("User Service", readBody(userHealth.body()));
    }

    private String readBody(final Body body) {
        try {
            InputStream is = body.asInputStream();
            Scanner sc = new Scanner(is);
            StringBuffer sb = new StringBuffer();
            while (sc.hasNext()) {
                sb.append(sc.nextLine());
            }
            return sb.toString();
        } catch (IOException e) {
            return e.getMessage();
        }
    }
}