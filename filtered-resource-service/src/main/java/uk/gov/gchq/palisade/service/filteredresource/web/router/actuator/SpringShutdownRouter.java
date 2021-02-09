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

package uk.gov.gchq.palisade.service.filteredresource.web.router.actuator;

import akka.http.javadsl.server.Directives;
import akka.http.javadsl.server.Route;
import akka.http.scaladsl.model.StatusCode;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;

/**
 * Route for "/shutdown" to the Spring {@link org.springframework.boot.SpringApplication#exit(ApplicationContext, ExitCodeGenerator...)}
 * static method, to imitate the equivalent spring actuator
 */
public class SpringShutdownRouter implements ActuatorSupplier {
    private static final StatusCode HTTP_OK = StatusCode.int2StatusCode(200);

    private final ApplicationContext ctx;

    /**
     * POSTable endpoint to call the {@link SpringApplication#exit(ApplicationContext, ExitCodeGenerator...)}
     *
     * @param applicationContext the spring application context
     */
    public SpringShutdownRouter(final ApplicationContext applicationContext) {
        this.ctx = applicationContext;
    }

    private Route postShutdown() {
        SpringApplication.exit(ctx, () -> 0);
        return Directives.complete(HTTP_OK);
    }

    /**
     * Convert between Spring's ShutdownEndpoint (usually /actuator/shutdown) and an Akka route, returning a
     * reasonable status code.
     *
     * @return an Akka {@link Route} that allows POSTing to shutdown the application
     */
    public Route get() {
        return Directives.pathPrefix("shutdown", () ->
                Directives.pathEndOrSingleSlash(() ->
                        Directives.post(this::postShutdown)));
    }
}
