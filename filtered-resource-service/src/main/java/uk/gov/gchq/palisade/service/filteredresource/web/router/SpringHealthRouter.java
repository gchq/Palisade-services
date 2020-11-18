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

package uk.gov.gchq.palisade.service.filteredresource.web.router;

import akka.http.javadsl.model.ContentTypes;
import akka.http.javadsl.model.HttpResponse;
import akka.http.javadsl.server.Directives;
import akka.http.javadsl.server.Route;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.Status;

import java.util.Map;
import java.util.Optional;

public class SpringHealthRouter implements RouteSupplier {
    private static final Logger LOGGER = LoggerFactory.getLogger(SpringHealthRouter.class);

    // Map Spring health Statuses to Akka StatusCodes
    private static final Map<Status, Integer> SPRING_STATUS_LOOKUP = Map.of(
            Status.UP, 200,
            Status.DOWN, 503,
            Status.OUT_OF_SERVICE, 503,
            Status.UNKNOWN, 500
    );
    // Used as a 'default' response if none of the above are appropriate
    private static final Integer INTERNAL_ERROR = 500;
    private static final Integer NOT_FOUND = 404;

    private final HealthEndpoint springHealthEndpoint;
    private final ObjectMapper mapper;

    public SpringHealthRouter(final HealthEndpoint springHealthEndpoint, final ObjectMapper mapper) {
        this.springHealthEndpoint = springHealthEndpoint;
        this.mapper = mapper;
    }

    private Route mapSpringToAkka(final HealthComponent healthComponent) {
        return Optional.ofNullable(healthComponent)
                // Get the component's status (UP/DOWN/UNKNOWN/etc...)
                .map(HealthComponent::getStatus)

                // Convert to an akka response with a status code
                .map(status -> {
                    Integer statusCode = SPRING_STATUS_LOOKUP.getOrDefault(status, INTERNAL_ERROR);
                    try {
                        return Directives.complete(HttpResponse.create()
                                .withStatus(statusCode)
                                .withEntity(ContentTypes.APPLICATION_JSON, this.mapper.writeValueAsBytes(status)));
                    } catch (JsonProcessingException e) {
                        LOGGER.error("Failed to serialise spring health status", e);
                        return Directives.complete(HttpResponse.create()
                                .withStatus(INTERNAL_ERROR)
                                .withEntity(ContentTypes.APPLICATION_JSON, String.format("{\"error\", \"%s\"}", e.getMessage())));
                    }
                })

                // If Spring failed to get a health component for the requested path
                .orElse(Directives.complete(HttpResponse.create().withStatus(NOT_FOUND)));
    }

    /**
     * Convert between Spring's HealthEndpoint (usually /actuator/health) and an Akka route (just /health),
     * returning a reasonable status code.
     *
     * @return an Akka {@link Route} that allows getting the application health
     */
    @Override
    public Route get() {
        return Directives.pathPrefix("health", () -> Directives.concat(
                Directives.pathEndOrSingleSlash(() -> mapSpringToAkka(springHealthEndpoint.health())),
                Directives.path(path -> mapSpringToAkka(springHealthEndpoint.healthForPath(path))))
        );
    }
}
