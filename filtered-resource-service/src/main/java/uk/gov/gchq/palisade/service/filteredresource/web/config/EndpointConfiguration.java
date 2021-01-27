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

package uk.gov.gchq.palisade.service.filteredresource.web.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.logging.LoggersEndpoint;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.availability.ApplicationAvailability;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import uk.gov.gchq.palisade.service.filteredresource.service.KafkaProducerService;
import uk.gov.gchq.palisade.service.filteredresource.service.WebSocketEventService;
import uk.gov.gchq.palisade.service.filteredresource.web.AkkaHttpServer;
import uk.gov.gchq.palisade.service.filteredresource.web.router.KafkaRestWriterRouter;
import uk.gov.gchq.palisade.service.filteredresource.web.router.RouteSupplier;
import uk.gov.gchq.palisade.service.filteredresource.web.router.SpringHealthRouter;
import uk.gov.gchq.palisade.service.filteredresource.web.router.SpringLoggersRouter;
import uk.gov.gchq.palisade.service.filteredresource.web.router.WebSocketRouter;

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
     * Route for "/resource/{token}" to the {@link WebSocketEventService}
     *
     * @param websocketEventService the websocket event service to connect to
     * @param objectMapper          an object mapper for object serialisation
     * @return the websocket router
     */
    @Bean
    WebSocketRouter websocketRouter(final WebSocketEventService websocketEventService, final ObjectMapper objectMapper) {
        return new WebSocketRouter(websocketEventService, objectMapper);
    }

    /**
     * Route for "/api/{topic}" to the {@link KafkaProducerService}
     *
     * @param kafkaProducerService the kafka producer service to connect to
     * @return the kafka rest writer router
     */
    @Bean
    @ConditionalOnBean(KafkaProducerService.class)
    KafkaRestWriterRouter kafkaRestWriterRouter(final KafkaProducerService kafkaProducerService) {
        return new KafkaRestWriterRouter(kafkaProducerService);
    }

    /**
     * Route for "/health[/|/liveliness|/readiness|/{component}]" to the Spring {@link HealthEndpoint}
     * or {@link ApplicationAvailability} objects
     *
     * @param springHealthEndpoint    Spring internal default health endpoints
     * @param applicationAvailability Spring internal default application readiness and liveliness indicator
     * @return the spring health router
     */
    @Bean
    SpringHealthRouter springHealthRouter(final HealthEndpoint springHealthEndpoint, final ApplicationAvailability applicationAvailability) {
        return new SpringHealthRouter(springHealthEndpoint, applicationAvailability);
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
}
