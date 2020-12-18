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

package uk.gov.gchq.palisade.component.filteredresource.web;

import akka.actor.ActorSystem;
import akka.http.javadsl.Http;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.http.scaladsl.model.StatusCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.actuate.health.DefaultHealthContributorRegistry;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthContributorRegistry;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.HealthEndpointGroups;
import org.springframework.boot.actuate.health.PingHealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.availability.ApplicationAvailability;
import org.springframework.boot.availability.LivenessState;
import org.springframework.boot.availability.ReadinessState;

import uk.gov.gchq.palisade.service.filteredresource.web.AkkaHttpServer;
import uk.gov.gchq.palisade.service.filteredresource.web.router.SpringHealthRouter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

class AkkaHealthTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String HOST = "localhost";
    private static final int PORT = 18080;

    // Health test objects
    HealthContributorRegistry hcr = new DefaultHealthContributorRegistry(Map.of("valid-health-component", new PingHealthIndicator()));
    HealthEndpointGroups heg = Mockito.mock(HealthEndpointGroups.class);
    HealthEndpoint healthEndpoint = Mockito.spy(new HealthEndpoint(hcr, heg));
    ApplicationAvailability applicationAvailability = Mockito.mock(ApplicationAvailability.class);

    // Health endpoint to be tested
    SpringHealthRouter healthRouter = new SpringHealthRouter(healthEndpoint, applicationAvailability);
    AkkaHttpServer server;

    // Akka runtime
    ActorSystem system = ActorSystem.create("websocket-test");

    @BeforeEach
    void setUp() {
        server = new AkkaHttpServer(HOST, PORT, List.of(healthRouter));
        server.serveForever(system);
    }

    @AfterEach
    void tearDown() {
        server.terminate();
    }

    @Test
    void testHealthReturns200() {
        // Given the server is running
        // Set the root-level /health to report healthy
        Mockito.doReturn(new Health.Builder(Status.UP).build()).when(healthEndpoint).health();

        // When
        CompletableFuture<HttpResponse> response = Http.get(system)
                .singleRequest(HttpRequest.GET(String.format("http://%s:%d/health", HOST, PORT)))
                .toCompletableFuture()
                .exceptionally(throwable -> fail("CompletableFuture failed while getting HTTP response", throwable));

        // Then
        assertThat(response.join())
                .extracting(HttpResponse::status)
                .isEqualTo(StatusCode.int2StatusCode(200));
    }

    @Test
    void testComponentPathReturns200() {
        // Given the server is running (and presumably healthy)
        // We'll use the "/valid-health-component" in our health component registry

        // When
        CompletableFuture<HttpResponse> response = Http.get(system)
                .singleRequest(HttpRequest.GET(String.format("http://%s:%d/health/valid-health-component", HOST, PORT)))
                .toCompletableFuture()
                .exceptionally(throwable -> fail("CompletableFuture failed while getting HTTP response", throwable));

        // Then
        assertThat(response.join())
                .extracting(HttpResponse::status)
                .isEqualTo(StatusCode.int2StatusCode(200));
    }

    @Test
    void testBadPathReturnsNot200() {
        // Given the server is running

        // When
        CompletableFuture<HttpResponse> response = Http.get(system)
                .singleRequest(HttpRequest.GET(String.format("http://%s:%d/health/invalid-health-component", HOST, PORT)))
                .toCompletableFuture()
                .exceptionally(throwable -> fail("CompletableFuture failed while getting HTTP response", throwable));

        // Then
        assertThat(response.join())
                .extracting(HttpResponse::status)
                .isNotEqualTo(StatusCode.int2StatusCode(200));
    }

    @Test
    void testCorrectLivenessReturns200() {
        // Given the server is running
        // Set /health/liveness to report healthy
        Mockito.when(applicationAvailability.getLivenessState()).thenReturn(LivenessState.CORRECT);

        // When
        CompletableFuture<HttpResponse> response = Http.get(system)
                .singleRequest(HttpRequest.GET(String.format("http://%s:%d/health/liveness", HOST, PORT)))
                .toCompletableFuture()
                .exceptionally(throwable -> fail("CompletableFuture failed while getting HTTP response", throwable));

        // Then
        assertThat(response.join())
                .extracting(HttpResponse::status)
                .isEqualTo(StatusCode.int2StatusCode(200));
    }

    @Test
    void testBrokenLivenessReturnsNot200() {
        // Given the server is running
        // Set /health/liveness to report healthy
        Mockito.when(applicationAvailability.getLivenessState()).thenReturn(LivenessState.BROKEN);

        // When
        CompletableFuture<HttpResponse> response = Http.get(system)
                .singleRequest(HttpRequest.GET(String.format("http://%s:%d/health/liveness", HOST, PORT)))
                .toCompletableFuture()
                .exceptionally(throwable -> fail("CompletableFuture failed while getting HTTP response", throwable));

        // Then
        assertThat(response.join())
                .extracting(HttpResponse::status)
                .isNotEqualTo(StatusCode.int2StatusCode(200));
    }

    @Test
    void testAcceptingTrafficReadinessReturns200() {
        // Given the server is running
        // Set /health/readiness to report healthy
        Mockito.when(applicationAvailability.getReadinessState()).thenReturn(ReadinessState.ACCEPTING_TRAFFIC);

        // When
        CompletableFuture<HttpResponse> response = Http.get(system)
                .singleRequest(HttpRequest.GET(String.format("http://%s:%d/health/readiness", HOST, PORT)))
                .toCompletableFuture()
                .exceptionally(throwable -> fail("CompletableFuture failed while getting HTTP response", throwable));

        // Then
        assertThat(response.join())
                .extracting(HttpResponse::status)
                .isEqualTo(StatusCode.int2StatusCode(200));
    }

    @Test
    void testRefusingTrafficReadinessReturnsNot200() {
        // Given the server is running
        // Set /health/readiness to report healthy
        Mockito.when(applicationAvailability.getReadinessState()).thenReturn(ReadinessState.REFUSING_TRAFFIC);

        // When
        CompletableFuture<HttpResponse> response = Http.get(system)
                .singleRequest(HttpRequest.GET(String.format("http://%s:%d/health/readiness", HOST, PORT)))
                .toCompletableFuture()
                .exceptionally(throwable -> fail("CompletableFuture failed while getting HTTP response", throwable));

        // Then
        assertThat(response.join())
                .extracting(HttpResponse::status)
                .isNotEqualTo(StatusCode.int2StatusCode(200));
    }
}
