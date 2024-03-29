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

package uk.gov.gchq.palisade.contract.data.kafka;

import akka.actor.ActorSystem;
import akka.kafka.ConsumerSettings;
import akka.kafka.Subscriptions;
import akka.kafka.javadsl.Consumer;
import akka.stream.Materializer;
import akka.stream.testkit.TestSubscriber.Probe;
import akka.stream.testkit.javadsl.TestSink;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.LinkedMultiValueMap;
import org.testcontainers.containers.KafkaContainer;
import scala.concurrent.duration.FiniteDuration;

import uk.gov.gchq.palisade.contract.data.common.ContractTestData;
import uk.gov.gchq.palisade.contract.data.common.TestSerDesConfig;
import uk.gov.gchq.palisade.service.data.DataApplication;
import uk.gov.gchq.palisade.service.data.model.AuditErrorMessage;
import uk.gov.gchq.palisade.service.data.model.AuditMessage;
import uk.gov.gchq.palisade.service.data.model.AuditSuccessMessage;
import uk.gov.gchq.palisade.service.data.model.DataRequest;
import uk.gov.gchq.palisade.service.data.model.ExceptionSource;
import uk.gov.gchq.palisade.service.data.model.Token;
import uk.gov.gchq.palisade.service.data.service.ReadChunkedDataService;
import uk.gov.gchq.palisade.service.data.service.authorisation.AuditableAuthorisationService;
import uk.gov.gchq.palisade.service.data.service.reader.SimpleDataReader;
import uk.gov.gchq.palisade.service.data.stream.ProducerTopicConfiguration;
import uk.gov.gchq.palisade.service.data.web.AkkaHttpServer;

import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static uk.gov.gchq.palisade.contract.data.common.ContractTestData.AUDITABLE_DATA_REQUEST;
import static uk.gov.gchq.palisade.contract.data.common.ContractTestData.AUDITABLE_DATA_REQUEST_WITH_ERROR;

/**
 * An external requirement of the service is to audit the client's requests.
 * Both successful data requests and errors are to be sent to the Audit Service to be recorded.
 */
@SpringBootTest(
        classes = DataApplication.class,
        webEnvironment = WebEnvironment.MOCK,
        properties = {"akka.discovery.config.services.kafka.from-config=false", "server.port=0"}
)
@Import({KafkaTestConfiguration.class})
@ActiveProfiles({"akka-test", "testcontainers"})
class KafkaContractTest {
    public static final String READ_CHUNKED = "/read/chunked";

    private TestRestTemplate restTemplate;
    @MockBean
    private AuditableAuthorisationService serviceMock;
    @SpyBean
    private SimpleDataReader readerSpy;
    @Autowired
    private ReadChunkedDataService writer;
    @Autowired
    private KafkaContainer kafkaContainer;
    @Autowired
    private ActorSystem akkaActorSystem;
    @Autowired
    private Materializer akkaMaterializer;
    @Autowired
    private ProducerTopicConfiguration producerTopicConfiguration;
    @Autowired
    private AkkaHttpServer akkaHttpServer;

    @BeforeEach
    void setUp() {
        var localAddress = akkaHttpServer.getServerBinding().join().localAddress();
        var rootUri = "http://localhost:" + localAddress.getPort();
        restTemplate = new TestRestTemplate(new RestTemplateBuilder().rootUri(rootUri));

        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setSupportedMediaTypes(List.of(MediaType.APPLICATION_JSON, MediaType.APPLICATION_OCTET_STREAM));
        restTemplate.getRestTemplate().setMessageConverters(Arrays.asList(converter, new FormHttpMessageConverter()));
    }

    /**
     * Tests the handling of the error messages on the kafka stream for the Data Service.  The expected results will be an
     * AuditErrorMessage on a Kafka stream to the "error-topic" and return HTTP Internal Server Error.
     */
    @Test
    @DirtiesContext
    void testRestEndpointError() {
        when(serviceMock.authoriseRequest(any()))
                .thenReturn(CompletableFuture.completedFuture(AUDITABLE_DATA_REQUEST_WITH_ERROR));

        // Given - we are already listening to the service error output
        ConsumerSettings<String, AuditErrorMessage> consumerSettings = ConsumerSettings
                .create(akkaActorSystem, TestSerDesConfig.errorKeyDeserialiser(), TestSerDesConfig.errorValueDeserialiser())
                .withGroupId("test-group")
                .withBootstrapServers(kafkaContainer.isRunning() ? kafkaContainer.getBootstrapServers() : "localhost:9092")
                .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        Probe<ConsumerRecord<String, AuditErrorMessage>> errorProbe = Consumer
                .atMostOnceSource(consumerSettings, Subscriptions.topics(producerTopicConfiguration.getTopics().get("error-topic").getName()))
                .runWith(TestSink.probe(akkaActorSystem), akkaMaterializer);

        // When - we POST to the rest endpoint
        Map<String, List<String>> headers = Collections.singletonMap(Token.HEADER, Collections.singletonList(ContractTestData.REQUEST_TOKEN));
        HttpEntity<DataRequest> entity = new HttpEntity<>(ContractTestData.REQUEST_OBJ, new LinkedMultiValueMap<>(headers));
        ResponseEntity<Void> response = restTemplate.postForEntity(READ_CHUNKED, entity, Void.class);

        // Then - the REST request was accepted, and returned a FORBIDDEN
        assertThat(response.getStatusCode())
                .as("Check the response was FORBIDDEN as the client has no authorisation to read the data")
                .isEqualTo(HttpStatus.FORBIDDEN);
        // When - results are pulled from the output stream
        Probe<ConsumerRecord<String, AuditErrorMessage>> resultSeq = errorProbe.request(1);

        LinkedList<ConsumerRecord<String, AuditErrorMessage>> results = LongStream.range(0, 1)
                .mapToObj(i -> resultSeq.expectNext(new FiniteDuration(21, TimeUnit.SECONDS)))
                .collect(Collectors.toCollection(LinkedList::new));

        // Then - the results are as expected
        assertThat(results)
                .hasSize(1)
                .allSatisfy(result -> {
                    assertThat(result.value())
                            .as("Recursively check the result against the AuditErrorMessage, ignoring the error")
                            .extracting(AuditMessage::getAttributes)
                            .isEqualTo(Map.of(ExceptionSource.ATTRIBUTE_KEY, ExceptionSource.AUTHORISED_REQUEST.toString()));

                    assertThat(result.value())
                            .as("Check the Error Message in the AuditErrorMessage object")
                            .extracting(AuditErrorMessage::getError)
                            .extracting(Throwable::getMessage)
                            .isEqualTo("Authorisation denied for request " + ContractTestData.REQUEST_OBJ.toString());

                    assertThat(result.headers().lastHeader(Token.HEADER).value())
                            .as("Check the bytes of the request token")
                            .isEqualTo(ContractTestData.REQUEST_TOKEN.getBytes());
                });
    }

    /**
     * Tests the handling of the successful messages on the kafka stream for the Data Service. The expected results will be an
     * AuditSuccessMessage on a Kafka stream to the "success-topic" and return HTTP Accepted.
     */
    @Test
    @DirtiesContext
    void testRestEndpointSuccess() {
        writer.setReaders(Collections.singleton(readerSpy));

        when(serviceMock.authoriseRequest(any()))
                .thenReturn(CompletableFuture.completedFuture(AUDITABLE_DATA_REQUEST));

        doReturn(new ByteArrayInputStream("some data".getBytes(Charset.defaultCharset())))
                .when(readerSpy).read(any());

        // Given - we are already listening to the service success output
        ConsumerSettings<String, AuditSuccessMessage> consumerSettings = ConsumerSettings
                .create(akkaActorSystem, TestSerDesConfig.successKeyDeserialiser(), TestSerDesConfig.successValueDeserialiser())
                .withGroupId("test-group")
                .withBootstrapServers(kafkaContainer.isRunning() ? kafkaContainer.getBootstrapServers() : "localhost:9092")
                .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        Probe<ConsumerRecord<String, AuditSuccessMessage>> probe = Consumer
                .atMostOnceSource(consumerSettings, Subscriptions.topics(producerTopicConfiguration.getTopics().get("success-topic").getName()))
                .runWith(TestSink.probe(akkaActorSystem), akkaMaterializer);

        Map<String, List<String>> headers = Collections.singletonMap(Token.HEADER, Collections.singletonList(ContractTestData.REQUEST_TOKEN));
        HttpEntity<DataRequest> entity = new HttpEntity<>(ContractTestData.REQUEST_OBJ, new LinkedMultiValueMap<>(headers));
        ResponseEntity<Void> response = restTemplate.postForEntity(READ_CHUNKED, entity, Void.class);

        // Then - the REST request was accepted
        assertThat(response.getStatusCode())
                .as("Check the response was OK as the client is authorised to read the data")
                .isEqualTo(HttpStatus.OK);
        // When - results are pulled from the output stream
        Probe<ConsumerRecord<String, AuditSuccessMessage>> resultSeq = probe.request(1);

        LinkedList<ConsumerRecord<String, AuditSuccessMessage>> results = LongStream.range(0, 1)
                .mapToObj(i -> resultSeq.expectNext(new FiniteDuration(21, TimeUnit.SECONDS)))
                .collect(Collectors.toCollection(LinkedList::new));

        // Then - the results are as expected
        assertThat(results)
                .hasSize(1)
                .allSatisfy(result -> {
                    assertThat(result.value())
                            .as("Recursivley check the result against the AuditSuccessMessage")
                            .usingRecursiveComparison()
                            .ignoringFields("timestamp")
                            .isEqualTo(ContractTestData.AUDIT_SUCCESS_MESSAGE);

                    assertThat(result.headers().lastHeader(Token.HEADER).value())
                            .as("Check the bytes of the request token")
                            .isEqualTo(ContractTestData.REQUEST_TOKEN.getBytes());
                });
    }

}
