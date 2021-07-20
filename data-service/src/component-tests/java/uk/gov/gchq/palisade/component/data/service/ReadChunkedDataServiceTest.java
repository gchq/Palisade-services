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

package uk.gov.gchq.palisade.component.data.service;

import akka.Done;
import akka.actor.ActorSystem;
import akka.http.javadsl.model.ContentTypes;
import akka.http.javadsl.model.HttpEntities;
import akka.http.javadsl.model.HttpMethods;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.StatusCodes;
import akka.stream.Materializer;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import akka.util.ByteString;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import uk.gov.gchq.palisade.component.data.common.CommonTestData;
import uk.gov.gchq.palisade.component.data.service.ReadChunkedDataServiceTest.TestConfiguration;
import uk.gov.gchq.palisade.service.data.config.ApplicationConfiguration;
import uk.gov.gchq.palisade.service.data.config.SerialiserConfiguration;
import uk.gov.gchq.palisade.service.data.domain.AuthorisedRequestEntity;
import uk.gov.gchq.palisade.service.data.exception.ForbiddenException;
import uk.gov.gchq.palisade.service.data.exception.ReaderNotFoundException;
import uk.gov.gchq.palisade.service.data.exception.SerialiserNotFoundException;
import uk.gov.gchq.palisade.service.data.repository.AuthorisedRequestsRepository;
import uk.gov.gchq.palisade.service.data.service.ReadChunkedDataService;
import uk.gov.gchq.palisade.service.data.service.reader.DataReader;

import java.nio.charset.Charset;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = {ApplicationConfiguration.class, TestConfiguration.class})
class ReadChunkedDataServiceTest {
    @Configuration
    static class TestConfiguration {
        @Bean
        Materializer materialiser(final ActorSystem actorSystem) {
            return Materializer.createMaterializer(actorSystem);
        }

        @Bean
        ActorSystem actorSystem() {
            return ActorSystem.create(ReadChunkedDataServiceTest.class.getSimpleName() + "ActorSystem");
        }
    }

    private static final ObjectMapper MAPPER = new ObjectMapper();
    public static final String TEST_RECORD = "record";

    @MockBean
    AuthorisedRequestsRepository mockAuthorisedRequestsRepository;
    @MockBean
    DataReader mockDataReader;
    @MockBean
    SerialiserConfiguration mockSerialiserConfiguration;

    @Autowired
    ReadChunkedDataService service;
    @Autowired
    ActorSystem testActorSystem;
    @Autowired
    Materializer testMaterialiser;

    @BeforeEach
    void setUp() {
        Mockito.reset(mockAuthorisedRequestsRepository, mockDataReader, mockSerialiserConfiguration);
        service.setSerialisers(Map.of());
    }

    @Test
    void testContextLoads() {
        assertThat(service)
                .isNotNull();
    }

    @Test
    void testServiceWithoutAuthorisation() throws JsonProcessingException {
        // Given - convert the service request/response route into a simple flow
        var httpFlow = service.get().flow(testActorSystem, testMaterialiser);
        // Given - we make a request
        var httpRequest = HttpRequest.create()
                .withUri("http://data-service/read/chunked")
                .withMethod(HttpMethods.POST)
                .withEntity(HttpEntities.create(ContentTypes.APPLICATION_JSON, MAPPER.writeValueAsBytes(CommonTestData.DATA_REQUEST)));

        // When
        var httpResponse = Source.single(httpRequest)
                .via(httpFlow)
                .runWith(Sink.head(), testMaterialiser)
                .toCompletableFuture().join();
        assertThat(httpResponse.status())
                .isEqualTo(StatusCodes.FORBIDDEN);
        var futureResponse = httpResponse.entity()
                .getDataBytes()
                .runWith(Sink.seq(), testMaterialiser)
                .toCompletableFuture();
        assertThatThrownBy(futureResponse::join)
                .isInstanceOf(CompletionException.class)
                .hasCauseInstanceOf(ForbiddenException.class);
    }

    @Test
    void testServiceWithoutReader() throws JsonProcessingException {
        // Given - convert the service request/response route into a simple flow
        var httpFlow = service.get().flow(testActorSystem, testMaterialiser);
        // Given - we make a request
        var httpRequest = HttpRequest.create()
                .withUri("http://data-service/read/chunked")
                .withMethod(HttpMethods.POST)
                .withEntity(HttpEntities.create(ContentTypes.APPLICATION_JSON, MAPPER.writeValueAsBytes(CommonTestData.DATA_REQUEST)));

        var authorised = new AuthorisedRequestEntity(CommonTestData.TOKEN, CommonTestData.USER, CommonTestData.RESOURCE, CommonTestData.CONTEXT, CommonTestData.RULES);
        Mockito.when(mockAuthorisedRequestsRepository.findByTokenAndResourceId(CommonTestData.TOKEN, CommonTestData.LEAF_RESOURCE_ID))
                .thenReturn(Optional.of(authorised));

        // When
        var httpResponse = Source.single(httpRequest)
                .via(httpFlow)
                .runWith(Sink.head(), testMaterialiser)
                .toCompletableFuture().join();
        assertThat(httpResponse.status())
                .isEqualTo(StatusCodes.OK);
        var futureResponse = httpResponse.entity()
                .getDataBytes()
                .runWith(Sink.seq(), testMaterialiser)
                .toCompletableFuture();
        assertThatThrownBy(futureResponse::join)
                .isInstanceOf(CompletionException.class)
                .hasCauseInstanceOf(ReaderNotFoundException.class);
    }

    @Test
    void testServiceWithoutSerialiser() throws JsonProcessingException {
        // Given - convert the service request/response route into a simple flow
        var httpFlow = service.get().flow(testActorSystem, testMaterialiser);
        // Given - we make a request
        var httpRequest = HttpRequest.create()
                .withUri("http://data-service/read/chunked")
                .withMethod(HttpMethods.POST)
                .withEntity(HttpEntities.create(ContentTypes.APPLICATION_JSON, MAPPER.writeValueAsBytes(CommonTestData.DATA_REQUEST)));

        var authorised = new AuthorisedRequestEntity(CommonTestData.TOKEN, CommonTestData.USER, CommonTestData.RESOURCE, CommonTestData.CONTEXT, CommonTestData.RULES);
        Mockito.when(mockAuthorisedRequestsRepository.findByTokenAndResourceId(CommonTestData.TOKEN, CommonTestData.LEAF_RESOURCE_ID))
                .thenReturn(Optional.of(authorised));
        Mockito.when(mockDataReader.accepts(CommonTestData.RESOURCE))
                .thenReturn(true);

        // When
        var httpResponse = Source.single(httpRequest)
                .via(httpFlow)
                .runWith(Sink.head(), testMaterialiser)
                .toCompletableFuture().join();
        assertThat(httpResponse.status())
                .isEqualTo(StatusCodes.OK);
        var futureResponse = httpResponse.entity()
                .getDataBytes()
                .runWith(Sink.seq(), testMaterialiser)
                .toCompletableFuture();
        assertThatThrownBy(futureResponse::join)
                .isInstanceOf(CompletionException.class)
                .hasCauseInstanceOf(SerialiserNotFoundException.class);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    void testServiceWithoutData() throws JsonProcessingException {
        // Given - convert the service request/response route into a simple flow
        var httpFlow = service.get().flow(testActorSystem, testMaterialiser);
        // Given - we make a request
        var httpRequest = HttpRequest.create()
                .withUri("http://data-service/read/chunked")
                .withMethod(HttpMethods.POST)
                .withEntity(HttpEntities.create(ContentTypes.APPLICATION_JSON, MAPPER.writeValueAsBytes(CommonTestData.DATA_REQUEST)));

        var authorised = new AuthorisedRequestEntity(CommonTestData.TOKEN, CommonTestData.USER, CommonTestData.RESOURCE, CommonTestData.CONTEXT, CommonTestData.RULES);
        Mockito.when(mockAuthorisedRequestsRepository.findByTokenAndResourceId(CommonTestData.TOKEN, CommonTestData.LEAF_RESOURCE_ID))
                .thenReturn(Optional.of(authorised));
        Mockito.when(mockDataReader.accepts(CommonTestData.RESOURCE))
                .thenReturn(true);
        service.setSerialisers(Map.of("text/plain", (Class) TestSerialiser.class));

        // When
        var httpResponse = Source.single(httpRequest)
                .via(httpFlow)
                .runWith(Sink.head(), testMaterialiser)
                .toCompletableFuture().join();
        assertThat(httpResponse.status())
                .isEqualTo(StatusCodes.OK);
        var futureResponse = httpResponse.entity()
                .getDataBytes()
                .runWith(Sink.seq(), testMaterialiser)
                .toCompletableFuture();
        assertThatThrownBy(futureResponse::join)
                .isInstanceOf(CompletionException.class)
                .hasCauseInstanceOf(NullPointerException.class);
    }

    @Test
    void testServiceWithAllComponents() throws JsonProcessingException {
        // Given - convert the service request/response route into a simple flow
        var httpFlow = service.get().flow(testActorSystem, testMaterialiser);
        // Given - we make a request
        var httpRequest = HttpRequest.create()
                .withUri("http://data-service/read/chunked")
                .withMethod(HttpMethods.POST)
                .withEntity(HttpEntities.create(ContentTypes.APPLICATION_JSON, MAPPER.writeValueAsBytes(CommonTestData.DATA_REQUEST)));

        var authorised = new AuthorisedRequestEntity(CommonTestData.TOKEN, CommonTestData.USER, CommonTestData.RESOURCE, CommonTestData.CONTEXT, CommonTestData.RULES);
        Mockito.when(mockAuthorisedRequestsRepository.findByTokenAndResourceId(CommonTestData.TOKEN, CommonTestData.LEAF_RESOURCE_ID))
                .thenReturn(Optional.of(authorised));
        Mockito.when(mockDataReader.accepts(CommonTestData.RESOURCE))
                .thenReturn(true);
        service.setSerialisers(Map.of("text/plain", (Class) TestSerialiser.class));
        Mockito.when(mockDataReader.readSource(CommonTestData.RESOURCE))
                .thenReturn(Source.single(ByteString.fromString(ReadChunkedDataServiceTest.TEST_RECORD))
                        .mapMaterializedValue(ign -> CompletableFuture.completedStage(Done.done())));

        // When
        var httpResponse = Source.single(httpRequest)
                .via(httpFlow)
                .runWith(Sink.head(), testMaterialiser)
                .toCompletableFuture().join();
        assertThat(httpResponse.status())
                .isEqualTo(StatusCodes.OK);
        var response = httpResponse.entity()
                .getDataBytes()
                .runWith(Sink.seq(), testMaterialiser)
                .toCompletableFuture().join()
                .stream()
                .map(bs -> bs.decodeString(Charset.defaultCharset()))
                .collect(Collectors.toList());

        assertThat(response)
                .containsExactly(TEST_RECORD);
    }

}
