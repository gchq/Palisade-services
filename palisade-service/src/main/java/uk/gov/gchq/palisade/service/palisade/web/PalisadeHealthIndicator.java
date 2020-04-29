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
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import uk.gov.gchq.palisade.service.palisade.service.AuditService;
import uk.gov.gchq.palisade.service.palisade.service.PolicyService;
import uk.gov.gchq.palisade.service.palisade.service.ResourceService;
import uk.gov.gchq.palisade.service.palisade.service.UserService;

import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;

@EnableFeignClients
@Component
@Profile("!k8s")
public class PalisadeHealthIndicator extends AbstractHealthIndicator {
    private final AuditService auditService;
    private final PolicyService policyService;
    private final ResourceService resourceService;
    private final UserService userService;

    public PalisadeHealthIndicator(final AuditService auditService, final PolicyService policyService, final ResourceService resourceService, final UserService userService) {
        this.auditService = auditService;
        this.policyService = policyService;
        this.resourceService = resourceService;
        this.userService = userService;
    }

    @Override
    protected void doHealthCheck(final Health.Builder builder) throws Exception {
        // Use the builder to build the health status details that should be reported.
        // If you throw an exception, the status will be DOWN with the exception message.
        Response auditHealth = auditService.getHealth();
        Response policyHealth = policyService.getHealth();
        Response resourceHealth = resourceService.getHealth();
        Response userHealth = userService.getHealth();
        if (auditHealth.status() != 200) {
            throw new Exception("Audit service down");
        }
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
                .withDetail("Audit Service", readBody(auditHealth.body()))
                .withDetail("Policy Service", readBody(policyHealth.body()))
                .withDetail("Resource Service", readBody(resourceHealth.body()))
                .withDetail("User Service", readBody(userHealth.body()));
    }

    private String readBody(final Body body) {
        try {
            InputStream is = body.asInputStream();
            Scanner sc = new Scanner(is);
            StringBuilder sb = new StringBuilder();
            while (sc.hasNext()) {
                sb.append(sc.nextLine());
            }
            return sb.toString();
        } catch (IOException e) {
            return e.getMessage();
        }
    }
}