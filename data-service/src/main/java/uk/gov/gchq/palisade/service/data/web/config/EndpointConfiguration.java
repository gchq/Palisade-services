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

package uk.gov.gchq.palisade.service.data.web.config;

import org.springframework.boot.actuate.context.ShutdownEndpoint;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.logging.LoggersEndpoint;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.availability.ApplicationAvailability;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import uk.gov.gchq.palisade.service.data.service.AuditMessageService;
import uk.gov.gchq.palisade.service.data.service.AuditableDataService;
import uk.gov.gchq.palisade.service.data.web.AkkaHttpServer;
import uk.gov.gchq.palisade.service.data.web.router.ChunkedHttpWriter;
import uk.gov.gchq.palisade.service.data.web.router.RouteSupplier;
import uk.gov.gchq.palisade.service.data.web.router.SpringActuatorRouter;
import uk.gov.gchq.palisade.service.data.web.router.actuator.ActuatorSupplier;
import uk.gov.gchq.palisade.service.data.web.router.actuator.SpringHealthRouter;
import uk.gov.gchq.palisade.service.data.web.router.actuator.SpringLoggersRouter;
import uk.gov.gchq.palisade.service.data.web.router.actuator.SpringShutdownRouter;

import java.net.InetAddress;
import java.util.Collection;
import java.util.Optional;

/**
 * Spring bean configuration and dependency injection specifically for each
 * {@link RouteSupplier} for the {@link AkkaHttpServer}.
 */
@Configuration
public class EndpointConfiguration {

    /**
     * The HTTP server will serve forever on the supplied {@code server.host} and {@code server.port}
     * config values.
     *
     * @param properties     spring internal {@code server.xxx} config file object
     * @param routeSuppliers collection of routes to bind for this server (see below)
     * @return the http server
     */
    @Bean
    AkkaHttpServer akkaHttpServer(final ServerProperties properties, final Collection<RouteSupplier> routeSuppliers) {
        String hostname = Optional.ofNullable(properties.getAddress())
                .map(InetAddress::getHostAddress)
                .orElse("0.0.0.0");
        return new AkkaHttpServer(hostname, properties.getPort(), routeSuppliers);
    }

    /**
     * Route for "/read/chunked" Data Service endpoint, used by a client to read data as an HTTP stream.
     *
     * @param auditableDataService the {@link AuditableDataService} used for reading the data
     * @param auditMessageService  the {@link AuditMessageService} used for audinting the result of a data request,
     *                             this might be success with records processed and returned, or error with the cause
     * @return
     */
    @Bean
    ChunkedHttpWriter chunkedHttpWriter(final AuditableDataService auditableDataService, final AuditMessageService auditMessageService) {
        return new ChunkedHttpWriter(auditableDataService, auditMessageService);
    }

    /**
     * Route for "/actuator" Spring actuator imitators, which are all Spring***Routers
     *
     * @param actuatorSuppliers below beans for {@link ActuatorSupplier}s
     * @return an endpoint concatenating all provided actuators
     */
    @Bean
    SpringActuatorRouter springActuatorRouter(final Collection<ActuatorSupplier> actuatorSuppliers) {
        return new SpringActuatorRouter(actuatorSuppliers);
    }

    /**
     * Route for "/health[/|/liveliness|/readiness|/{component}]" to the Spring {@link HealthEndpoint}
     * or {@link ApplicationAvailability} objects
     *
     * @param healthEndpoint          Spring internal default health endpoint
     * @param applicationAvailability Spring internal default application readiness and liveliness indicator
     * @return the spring health router
     */
    @Bean
    SpringHealthRouter springHealthRouter(final HealthEndpoint healthEndpoint, final ApplicationAvailability applicationAvailability) {
        return new SpringHealthRouter(healthEndpoint, applicationAvailability);
    }

    /**
     * Route for "/loggers/{classpath}" to the Spring {@link LoggersEndpoint} for setting logging levels
     *
     * @param loggersEndpoint Spring internal default loggers endpoint
     * @return the spring loggers router
     */
    @Bean
    SpringLoggersRouter springLoggersRouter(final LoggersEndpoint loggersEndpoint) {
        return new SpringLoggersRouter(loggersEndpoint);
    }

    /**
     * Route for "/shutdown" to the Spring {@link org.springframework.context.ApplicationContext} for exiting the application
     *
     * @param shutdownEndpoint Spring internal default shutdown endpoint
     * @return the spring shutdown router
     */
    @Bean
    SpringShutdownRouter springShutdownRouter(final ShutdownEndpoint shutdownEndpoint) {
        return new SpringShutdownRouter(shutdownEndpoint);
    }
}
