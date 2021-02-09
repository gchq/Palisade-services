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

package uk.gov.gchq.palisade.service.filteredresource.web.router;

import akka.http.javadsl.server.Directives;
import akka.http.javadsl.server.Route;

import uk.gov.gchq.palisade.service.filteredresource.web.router.actuator.ActuatorSupplier;

import java.util.Collection;
import java.util.Collections;
import java.util.function.Supplier;

/**
 * Collect all {@link ActuatorSupplier}s and return them under the /actuator REST path.
 */
public class SpringActuatorRouter implements RouteSupplier {
    private final Collection<ActuatorSupplier> actuatorSuppliers;

    /**
     * Serve all ActuatorSuppliers under the /actuator endpoint
     *
     * @param actuatorSuppliers a collection of actuators to serve
     */
    public SpringActuatorRouter(final Collection<ActuatorSupplier> actuatorSuppliers) {
        this.actuatorSuppliers = Collections.unmodifiableCollection(actuatorSuppliers);
    }

    private Route concatActuators() {
        return this.actuatorSuppliers.stream()
                .map(Supplier::get)
                .reduce(Directives::concat)
                .orElseThrow(() -> new IllegalArgumentException("No actuator suppliers found to create HTTP server bindings, check your config."));
    }

    /**
     * Serve all ActuatorSuppliers under the /actuator endpoint
     *
     * @return an Akka {@link Route} for all Spring Actuator imitators
     */
    @Override
    public Route get() {
        return Directives.pathPrefix("actuator", this::concatActuators);
    }
}
