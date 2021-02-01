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

package uk.gov.gchq.palisade.service.filteredresource.web;

import akka.actor.ActorSystem;
import akka.http.javadsl.HandlerProvider;
import akka.http.javadsl.Http;
import akka.http.javadsl.ServerBinding;
import akka.http.javadsl.server.Directives;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.gov.gchq.palisade.service.filteredresource.web.router.RouteSupplier;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * The HTTP server will serve forever on the supplied {@code server.host} and {@code server.port}
 * config values, binding all the given {@link RouteSupplier}s using their given
 * {@link akka.http.javadsl.server.Route}s.
 */
public class AkkaHttpServer {
    private static final Logger LOGGER = LoggerFactory.getLogger(AkkaHttpServer.class);

    private final String hostname;
    private final int port;
    private final Collection<RouteSupplier> routeSuppliers;
    private final HandlerProvider bindings;

    private CompletableFuture<ServerBinding> serverBinding = new CompletableFuture<>();

    /**
     * Constructor for a new Akka-backed HTTP server, specifying the {@code hostname:port} to bind and the endpoints
     * for the server to expose.
     *
     * @param hostname       the hostname to bind the server to
     * @param port           the port that the server will listen on (likely either :80 for standard HTTP or :80xx for palisade-services)
     * @param routeSuppliers the endpoints to bind to this server and expose to any clients
     */
    public AkkaHttpServer(final String hostname, final int port, final Collection<RouteSupplier> routeSuppliers) {
        this.hostname = hostname;
        this.port = port;
        this.routeSuppliers = Collections.unmodifiableCollection(routeSuppliers);
        this.bindings = this.routeSuppliers.stream()
                .map(Supplier::get)
                .reduce(Directives::concat)
                .orElseThrow(() -> new IllegalArgumentException("No route suppliers found to create HTTP server bindings, check your config."));
    }

    /**
     * Start the server using the provided actor system and start to serve requests forever,
     * or until {@link AkkaHttpServer#terminate()}d.
     *
     * @param system the akka actor system (effectively providing the thread-pool to run this server on)
     */
    public void serveForever(final ActorSystem system) {
        this.serverBinding = Http.get(system)
                .newServerAt(this.hostname, this.port)
                .bind(this.bindings)
                .toCompletableFuture();

        LOGGER.info("Started Akka Http server at {} with {} bindings", serverBinding.join().localAddress(), this.routeSuppliers.size());
        LOGGER.debug("Bindings are: {}", this.routeSuppliers.stream()
                .map(Object::getClass)
                .map(Class::getSimpleName)
                .collect(Collectors.toList()));
    }

    /**
     * Unbind the server from its {@code hostname:port} pair and terminate execution.
     */
    public void terminate() {
        this.serverBinding.thenCompose(ServerBinding::unbind).join();
    }
}
