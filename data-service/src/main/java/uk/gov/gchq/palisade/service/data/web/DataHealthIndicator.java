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
package uk.gov.gchq.palisade.service.data.web;

import feign.Response;
import feign.Response.Body;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import uk.gov.gchq.palisade.service.data.service.AuditService;
import uk.gov.gchq.palisade.service.data.service.PalisadeService;

import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;

@EnableFeignClients
@Component
@Profile("!k8s")
public class DataHealthIndicator extends AbstractHealthIndicator {
    private final PalisadeService palisadeService;
    private final AuditService auditService;


    public DataHealthIndicator(final PalisadeService palisadeService, final AuditService auditService) {
        this.palisadeService = palisadeService;
        this.auditService = auditService;
    }

    @Override
    protected void doHealthCheck(final Health.Builder builder) throws Exception {
        // Use the builder to build the health status details that should be reported.
        // If you throw an exception, the status will be DOWN with the exception message.
        Response palisadeHealth = palisadeService.getHealth();

        Response auditHealth = auditService.getHealth();

        if (palisadeHealth.status() != 200) {
            throw new Exception("Palisade service down");
        }
        if (auditHealth.status() != 200) {
            throw new Exception("Audit service down");
        }
        builder.up()
                .withDetail("Audit Service", readBody(auditHealth.body()))
                .withDetail("Palisade Service", readBody(palisadeHealth.body()));
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