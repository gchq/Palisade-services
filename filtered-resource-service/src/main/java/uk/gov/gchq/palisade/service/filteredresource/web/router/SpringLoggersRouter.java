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

import akka.http.javadsl.marshallers.jackson.Jackson;
import akka.http.javadsl.server.Directives;
import akka.http.javadsl.server.Route;
import akka.http.scaladsl.model.StatusCode;
import org.springframework.boot.actuate.logging.LoggersEndpoint;
import org.springframework.boot.actuate.logging.LoggersEndpoint.LoggerLevels;
import org.springframework.boot.logging.LogLevel;

import java.util.Map;

public class SpringLoggersRouter implements RouteSupplier {
    /**
     * Provide Jackson with a zero-args-constructor creator method to call for incoming setLoggerLevel requests (POST LoggerLevels)
     */
    private static class DeserializableLoggerLevels extends LoggerLevels {
        protected DeserializableLoggerLevels() {
            super(null);
        }
    }

    private final LoggersEndpoint springLoggersEndpoint;

    public SpringLoggersRouter(final LoggersEndpoint springLoggersEndpoint) {
        this.springLoggersEndpoint = springLoggersEndpoint;
    }

    private Route getLoggerLevel(final String path) {
        return Directives.get(() -> {
            LoggerLevels entity = springLoggersEndpoint.loggerLevels(path);
            return Directives.complete(StatusCode.int2StatusCode(200), entity, Jackson.marshaller());
        });
    }

    private Route setLoggerLevel(final String path) {
        return Directives.post(() ->
                Directives.entity(Jackson.unmarshaller(DeserializableLoggerLevels.class), (DeserializableLoggerLevels loggerLevel) -> {
                    springLoggersEndpoint.configureLogLevel(path, LogLevel.valueOf(loggerLevel.getConfiguredLevel()));
                    LoggerLevels entity = springLoggersEndpoint.loggerLevels(path);
                    return Directives.complete(StatusCode.int2StatusCode(200), entity, Jackson.marshaller());
                }));
    }

    private Route getLoggers() {
        return Directives.get(() -> {
            Map<String, Object> entity = springLoggersEndpoint.loggers();
            return Directives.complete(StatusCode.int2StatusCode(200), entity, Jackson.marshaller());
        });
    }

    /**
     * Convert between Spring's LoggersEndpoint (usually /actuator/loggers) and an Akka route (just /loggers).
     *
     * @return an Akka {@link Route} that allows getting and setting application logging level
     */
    @Override
    public Route get() {
        return Directives.pathPrefix("loggers", () ->
                Directives.concat(
                        Directives.path(path ->
                                Directives.concat(this.getLoggerLevel(path), this.setLoggerLevel(path))),
                        Directives.pathEndOrSingleSlash(this::getLoggers)
                ));
    }

}
