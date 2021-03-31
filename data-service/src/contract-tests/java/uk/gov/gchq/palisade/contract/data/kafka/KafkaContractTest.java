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
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.testcontainers.containers.KafkaContainer;
import scala.concurrent.duration.FiniteDuration;

import uk.gov.gchq.palisade.contract.data.common.ContractTestData;
import uk.gov.gchq.palisade.contract.data.common.TestSerDesConfig;
import uk.gov.gchq.palisade.service.data.DataApplication;
import uk.gov.gchq.palisade.service.data.common.Token;
import uk.gov.gchq.palisade.service.data.model.AuditErrorMessage;
import uk.gov.gchq.palisade.service.data.model.AuditSuccessMessage;
import uk.gov.gchq.palisade.service.data.model.DataRequest;
import uk.gov.gchq.palisade.service.data.service.AuditableDataService;
import uk.gov.gchq.palisade.service.data.stream.ProducerTopicConfiguration;
import uk.gov.gchq.palisade.service.data.stream.PropertiesConfigurer;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toMap;
import static org.apache.kafka.clients.admin.AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.gchq.palisade.contract.data.common.ContractTestData.AUDITABLE_DATA_REQUEST;
import static uk.gov.gchq.palisade.contract.data.common.ContractTestData.AUDITABLE_DATA_REQUEST_WITH_ERROR;
import static uk.gov.gchq.palisade.contract.data.common.ContractTestData.AUDITABLE_DATA_RESPONSE;

/**
 * An external requirement of the service is to audit the client's requests.
 * Both successful data requests and errors are to be sent to the audit-service to be recorded.
 */
@SpringBootTest(
        classes = DataApplication.class,
        webEnvironment = WebEnvironment.RANDOM_PORT,
        properties = {"akka.discovery.config.services.kafka.from-config=false"}
)
@Import({KafkaContractTest.KafkaInitializer.Config.class})
@ContextConfiguration(initializers = {KafkaContractTest.KafkaInitializer.class})
@ActiveProfiles({"akka-test"})
class KafkaContractTest {
    public static final String READ_CHUNKED = "/read/chunked";

    @Autowired
    private TestRestTemplate restTemplate;
    @MockBean
    private AuditableDataService serviceMock;
    @Autowired
    private ActorSystem akkaActorSystem;
    @Autowired
    private Materializer akkaMaterializer;
    @Autowired
    private ProducerTopicConfiguration producerTopicConfiguration;

    @BeforeEach
    void setUp() {
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setSupportedMediaTypes(
                Arrays.asList(MediaType.APPLICATION_JSON, MediaType.APPLICATION_OCTET_STREAM));
        restTemplate.getRestTemplate().setMessageConverters(Arrays.asList(converter, new FormHttpMessageConverter()));
    }

    /**
     * Tests the handling of the error messages on the kafka stream for data-service.  The expected results will be an
     * AuditErrorMessage on a Kafka stream to the "error-topic" and return HTTP Internal Server Error.
     */
    @Test
    @DirtiesContext
    void testRestEndpointError() {
        when(serviceMock.authoriseRequest(any()))
                .thenReturn(CompletableFuture.completedFuture(AUDITABLE_DATA_REQUEST_WITH_ERROR));

        // Given - we are already listening to the service error output
        ConsumerSettings<String, AuditErrorMessage> consumerSettings = ConsumerSettings
                .create(akkaActorSystem, TestSerDesConfig.errorKeyDeserializer(), TestSerDesConfig.errorValueDeserializer())
                .withGroupId("test-group")
                .withBootstrapServers(KafkaInitializer.KAFKA.getBootstrapServers())
                .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        Probe<ConsumerRecord<String, AuditErrorMessage>> errorProbe = Consumer
                .atMostOnceSource(consumerSettings, Subscriptions.topics(producerTopicConfiguration.getTopics().get("error-topic").getName()))
                .runWith(TestSink.probe(akkaActorSystem), akkaMaterializer);

        // When - we POST to the rest endpoint
        Map<String, List<String>> headers = Collections.singletonMap(Token.HEADER, Collections.singletonList(ContractTestData.REQUEST_TOKEN));
        HttpEntity<DataRequest> entity = new HttpEntity<>(ContractTestData.REQUEST_OBJ, new LinkedMultiValueMap<>(headers));
        ResponseEntity<Void> response = restTemplate.postForEntity(READ_CHUNKED, entity, Void.class);

        // Then - the REST request was accepted
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
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
                            .usingRecursiveComparison()
                            .ignoringFieldsOfTypes(Throwable.class)
                            .isEqualTo(ContractTestData.AUDIT_ERROR_MESSAGE);

                    assertThat(result.value())
                            .as("Check the Error Message in the AuditErrorMessage object")
                            .extracting(AuditErrorMessage::getError)
                            .extracting(Throwable::getMessage)
                            .isEqualTo(ContractTestData.AUDIT_ERROR_MESSAGE.getError().getMessage());

                    assertThat(result.headers().lastHeader(Token.HEADER).value())
                            .as("Check the bytes of the request token")
                            .isEqualTo(ContractTestData.REQUEST_TOKEN.getBytes());
                });
    }

    /**
     * Tests the handling of the successful messages on the kafka stream for data-service. The expected results will be an
     * AuditSuccessMessage on a Kafka stream to the "success-topic" and return HTTP Accepted.
     */
    @Test
    @DirtiesContext
    void testRestEndpointSuccess() {
        when(serviceMock.authoriseRequest(any()))
                .thenReturn(CompletableFuture.completedFuture(AUDITABLE_DATA_REQUEST));

        when(serviceMock.read(any(), any()))
                .thenReturn(CompletableFuture.completedFuture(AUDITABLE_DATA_RESPONSE));

        // Given - we are already listening to the service error output
        ConsumerSettings<String, AuditSuccessMessage> consumerSettings = ConsumerSettings
                .create(akkaActorSystem, TestSerDesConfig.keyDeserializer(), TestSerDesConfig.valueDeserializer())
                .withGroupId("test-group")
                .withBootstrapServers(KafkaInitializer.KAFKA.getBootstrapServers())
                .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        Probe<ConsumerRecord<String, AuditSuccessMessage>> probe = Consumer
                .atMostOnceSource(consumerSettings, Subscriptions.topics(producerTopicConfiguration.getTopics().get("success-topic").getName()))
                .runWith(TestSink.probe(akkaActorSystem), akkaMaterializer);

        Map<String, List<String>> headers = Collections.singletonMap(Token.HEADER, Collections.singletonList(ContractTestData.REQUEST_TOKEN));
        HttpEntity<DataRequest> entity = new HttpEntity<>(ContractTestData.REQUEST_OBJ, new LinkedMultiValueMap<>(headers));
        ResponseEntity<Void> response = restTemplate.postForEntity(READ_CHUNKED, entity, Void.class);

        // Then - the REST request was accepted
        assertThat(response.getStatusCode())
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
                            .isEqualTo(ContractTestData.AUDIT_SUCCESS_MESSAGE);

                    assertThat(result.headers().lastHeader(Token.HEADER).value())
                            .as("Check the bytes of the request token")
                            .isEqualTo(ContractTestData.REQUEST_TOKEN.getBytes());
                });
    }

    public static class KafkaInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        private static final Logger LOGGER = LoggerFactory.getLogger(KafkaInitializer.class);

        static final KafkaContainer KAFKA = new KafkaContainer("5.5.1")
                .withReuse(true);

        static void createTopics(final List<NewTopic> newTopics) throws ExecutionException, InterruptedException {
            try (AdminClient admin = AdminClient.create(Map.of(BOOTSTRAP_SERVERS_CONFIG, String.format("%s:%d", "localhost", KafkaInitializer.KAFKA.getFirstMappedPort())))) {
                admin.createTopics(newTopics);
                LOGGER.info("created topics: " + admin.listTopics().names().get());
            }
        }

        @Override
        public void initialize(final ConfigurableApplicationContext configurableApplicationContext) {
            configurableApplicationContext.getEnvironment().setActiveProfiles("akka-test");
            KAFKA.addEnv("KAFKA_AUTO_CREATE_TOPICS_ENABLE", "false");
            KAFKA.addEnv("KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR", "1");
            KAFKA.start();

            // test kafka config
            String kafkaConfig = "akka.discovery.config.services.kafka.from-config=false";
            String kafkaPort = "akka.discovery.config.services.kafka.endpoints[0].port" + KAFKA.getFirstMappedPort();
            TestPropertySourceUtils.addInlinedPropertiesToEnvironment(configurableApplicationContext, kafkaConfig, kafkaPort);
        }

        @Configuration
        public static class Config {

            private final List<NewTopic> topics = List.of(
                    new NewTopic("success", 1, (short) 1),
                    new NewTopic("error", 1, (short) 1));

            @Bean
            @ConditionalOnMissingBean
            static PropertiesConfigurer propertiesConfigurer(final ResourceLoader resourceLoader, final Environment environment) {
                return new PropertiesConfigurer(resourceLoader, environment);
            }

            @Bean
            KafkaContainer kafkaContainer() throws ExecutionException, InterruptedException {
                createTopics(this.topics);
                return KAFKA;
            }

            @Bean
            @Primary
            ActorSystem actorSystem(final PropertiesConfigurer props, final KafkaContainer kafka) {
                LOGGER.info("Starting Kafka with port {}", kafka.getFirstMappedPort());
                return ActorSystem.create("actor-with-overrides", props.toHoconConfig(Stream.concat(
                        props.getAllActiveProperties().entrySet().stream()
                                .filter(kafkaPort -> !kafkaPort.getKey().equals("akka.discovery.config.services.kafka.endpoints[0].port")),
                        Stream.of(new AbstractMap.SimpleEntry<>("akka.discovery.config.services.kafka.endpoints[0].port", Integer.toString(kafka.getFirstMappedPort()))))
                        .peek(entry -> LOGGER.info("Config key {} = {}", entry.getKey(), entry.getValue()))
                        .collect(toMap(Map.Entry::getKey, Map.Entry::getValue))));
            }

            @Bean
            @Primary
            Materializer materializer(final ActorSystem system) {
                return Materializer.createMaterializer(system);
            }
        }
    }
}
