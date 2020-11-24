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
import uk.gov.gchq.palisade.service.filteredresource.service.WebsocketEventService;
import uk.gov.gchq.palisade.service.filteredresource.web.AkkaHttpServer;
import uk.gov.gchq.palisade.service.filteredresource.web.router.KafkaRestWriterRouter;
import uk.gov.gchq.palisade.service.filteredresource.web.router.RouteSupplier;
import uk.gov.gchq.palisade.service.filteredresource.web.router.SpringHealthRouter;
import uk.gov.gchq.palisade.service.filteredresource.web.router.SpringLoggersRouter;
import uk.gov.gchq.palisade.service.filteredresource.web.router.WebsocketRouter;

import java.net.InetAddress;
import java.util.Collection;
import java.util.Optional;

@Configuration
public class EndpointConfiguration {
    @Bean
    AkkaHttpServer akkaHttpServer(final ServerProperties properties, final Collection<RouteSupplier> routeSuppliers) {
        String hostname = Optional.ofNullable(properties.getAddress())
                .map(InetAddress::getHostAddress)
                .orElse("localhost");
        return new AkkaHttpServer(hostname, properties.getPort(), routeSuppliers);
    }

    @Bean
    WebsocketRouter websocketRouter(final WebsocketEventService websocketEventService, final ObjectMapper objectMapper) {
        return new WebsocketRouter(websocketEventService, objectMapper);
    }

    @Bean
    @ConditionalOnBean(KafkaProducerService.class)
    KafkaRestWriterRouter kafkaRestWriterRouter(final KafkaProducerService kafkaProducerService) {
        return new KafkaRestWriterRouter(kafkaProducerService);
    }

    @Bean
    SpringHealthRouter springHealthRouter(final HealthEndpoint springHealthEndpoint, final ApplicationAvailability applicationAvailability) {
        return new SpringHealthRouter(springHealthEndpoint, applicationAvailability);
    }

    @Bean
    SpringLoggersRouter springLoggersRouter(final LoggersEndpoint loggersEndpoint) {
        return new SpringLoggersRouter(loggersEndpoint);
    }
}
