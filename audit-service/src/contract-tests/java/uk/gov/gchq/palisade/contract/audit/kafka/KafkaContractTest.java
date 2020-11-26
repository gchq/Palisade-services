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

package uk.gov.gchq.palisade.contract.audit.kafka;

import akka.actor.ActorSystem;
import akka.kafka.ProducerSettings;
import akka.kafka.javadsl.Producer;
import akka.stream.Materializer;
import akka.stream.javadsl.Source;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.serializer.support.SerializationFailedException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.testcontainers.containers.KafkaContainer;

import uk.gov.gchq.palisade.contract.audit.ContractTestData;
import uk.gov.gchq.palisade.service.audit.AuditApplication;
import uk.gov.gchq.palisade.service.audit.model.AuditErrorMessage;
import uk.gov.gchq.palisade.service.audit.service.AuditService;
import uk.gov.gchq.palisade.service.audit.stream.ConsumerTopicConfiguration;
import uk.gov.gchq.palisade.service.audit.stream.PropertiesConfigurer;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toMap;
import static org.apache.kafka.clients.admin.AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

/**
 * An external requirement of the service is to connect to a pair of upstream kafka topics.
 * <ol>
 *     <li>The "error" topic is written to by any service that encounters an error when processing a request</li>
 *     <li>The "success" topic should only be written to by the filtered-recource-service or the data-service</li>
 * </ol>
 * This service does not write to a downstream topic
 */
@SpringBootTest(
        classes = AuditApplication.class,
        webEnvironment = WebEnvironment.RANDOM_PORT,
        properties = {"akka.discovery.config.services.kafka.from-config=false"}
)
@Import(KafkaContractTest.KafkaInitializer.Config.class)
@ContextConfiguration(initializers = {KafkaContractTest.KafkaInitializer.class})
@ActiveProfiles({"akkatest"})
class KafkaContractTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaContractTest.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    // Serialiser for upstream test input
    static class RequestSerializer implements Serializer<JsonNode> {
        @Override
        public byte[] serialize(final String s, final JsonNode auditRequest) {
            try {
                return MAPPER.writeValueAsBytes(auditRequest);
            } catch (JsonProcessingException e) {
                throw new SerializationFailedException("Failed to serialize " + auditRequest.toString(), e);
            }
        }
    }

    // Deserialiser for downstream test output
    static class ResponseDeserializer implements Deserializer<JsonNode> {
        @Override
        public JsonNode deserialize(final String s, final byte[] auditResponse) {
            try {
                return MAPPER.readTree(auditResponse);
            } catch (IOException e) {
                throw new SerializationFailedException("Failed to deserialize " + new String(auditResponse), e);
            }
        }
    }

    // Deserialiser for downstream test error output
    static class ErrorDeserializer implements Deserializer<AuditErrorMessage> {
        @Override
        public AuditErrorMessage deserialize(final String s, final byte[] auditErrorMessage) {
            try {
                return MAPPER.readValue(auditErrorMessage, AuditErrorMessage.class);
            } catch (IOException e) {
                throw new SerializationFailedException("Failed to deserialize " + new String(auditErrorMessage), e);
            }
        }
    }

    @Autowired
    private TestRestTemplate restTemplate;
    @Autowired
    private ActorSystem akkaActorSystem;
    @Autowired
    private Materializer akkaMaterializer;
    @Autowired
    private ConsumerTopicConfiguration consumerTopicConfiguration;
    @SpyBean
    private AuditService auditService;

    @Test
    @DirtiesContext
    void testErrorRequestSet() {
        // Create an message on the error topic
        // The ContractTestData.REQUEST_TOKEN maps to partition 0 of [0, 1, 2], so the akkatest yaml connects the consumer to only partition 0
        final Stream<ProducerRecord<String, JsonNode>> requests = ContractTestData.ERROR_RECORD_NODE_FACTORY.get().limit(3L);

        // When - we write to the input
        ProducerSettings<String, JsonNode> producerSettings = ProducerSettings
                .create(akkaActorSystem, new StringSerializer(), new RequestSerializer())
                .withBootstrapServers(KafkaInitializer.kafka.getBootstrapServers());

        Source.fromJavaStream(() -> requests)
                .runWith(Producer.plainSink(producerSettings), akkaMaterializer)
                .toCompletableFuture().join();

        // Then - check the audit service has invoked the audit method
        Mockito.verify(auditService, Mockito.timeout(3000).times(3)).audit(anyString(), any());

    }

    /*@Test
    @DirtiesContext
    void testNoSuchUserIdExceptionIsThrown() {
        // Create a variable number of requests
        // The ContractTestData.REQUEST_TOKEN maps to partition 0 of [0, 1, 2], so the akka-test yaml connects the consumer to only partition 0
        final Stream<ProducerRecord<String, JsonNode>> requests = Stream.of(
                Stream.of(ContractTestData.START_RECORD),
                ContractTestData.SUCCESS_RECORD_NODE_FACTORY.get().limit(1L),
                Stream.of(ContractTestData.END_RECORD))
                .flatMap(Function.identity());

        // Given - we are already listening to the output
        ConsumerSettings<String, JsonNode> consumerSettings = ConsumerSettings
                .create(akkaActorSystem, new StringDeserializer(), new ResponseDeserializer())
                .withBootstrapServers(KafkaInitializer.kafka.getBootstrapServers())
                .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        Probe<ConsumerRecord<String, JsonNode>> probe = Consumer
                .atMostOnceSource(consumerSettings, Subscriptions.topics(producerTopicConfiguration.getTopics().get("output-topic").getName()))
                .runWith(TestSink.probe(akkaActorSystem), akkaMaterializer);

        // When - we write to the input
        ProducerSettings<String, JsonNode> producerSettings = ProducerSettings
                .create(akkaActorSystem, new StringSerializer(), new RequestSerializer())
                .withBootstrapServers(KafkaInitializer.kafka.getBootstrapServers());

        Source.fromJavaStream(() -> requests)
                .runWith(Producer.plainSink(producerSettings), akkaMaterializer);


        // When - results are pulled from the output stream
        // record count set to 2, as one record will be removed as no policy exists for it
        Probe<ConsumerRecord<String, JsonNode>> resultSeq = probe.request(2);
        LinkedList<ConsumerRecord<String, JsonNode>> results = LongStream.range(0, 2)
                .mapToObj(i -> resultSeq.expectNext(new FiniteDuration(20 + 2, TimeUnit.SECONDS)))
                .collect(Collectors.toCollection(LinkedList::new));

        // Then - the results are as expected

        // All messages have a correct Token in the header
        assertAll("Headers have correct token",
                () -> assertThat(results)
                        .hasSize(2),

                () -> assertThat(results)
                        .allSatisfy(result ->
                                assertThat(result.headers().lastHeader(Token.HEADER).value())
                                        .isEqualTo(ContractTestData.REQUEST_TOKEN.getBytes()))
        );

        // The first and last have a correct StreamMarker header
        assertAll("StreamMarkers are correct START and END",
                () -> assertThat(results.getFirst().headers().lastHeader(StreamMarker.HEADER).value())
                        .isEqualTo(StreamMarker.START.toString().getBytes()),

                () -> assertThat(results.getLast().headers().lastHeader(StreamMarker.HEADER).value())
                        .isEqualTo(StreamMarker.END.toString().getBytes())
        );

        // All but the first and last have the expected message
        results.removeFirst();
        results.removeLast();
        assertThat(results).isEmpty();
    }

    @Test
    @DirtiesContext
    void testRestEndpoint() {
        // Given - we are already listening to the service input
        ConsumerSettings<String, UserRequest> consumerSettings = ConsumerSettings
                .create(akkaActorSystem, SerDesConfig.requestKeyDeserializer(), SerDesConfig.requestValueDeserializer())
                .withGroupId("test-group")
                .withBootstrapServers(KafkaInitializer.kafka.getBootstrapServers())
                .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        Probe<ConsumerRecord<String, UserRequest>> probe = Consumer
                .atMostOnceSource(consumerSettings, Subscriptions.topics(consumerTopicConfiguration.getTopics().get("input-topic").getName()))
                .runWith(TestSink.probe(akkaActorSystem), akkaMaterializer);

        // When - we POST to the rest endpoint
        Map<String, List<String>> headers = Collections.singletonMap(Token.HEADER, Collections.singletonList(ContractTestData.REQUEST_TOKEN));
        HttpEntity<UserRequest> entity = new HttpEntity<>(ContractTestData.REQUEST_OBJ, new LinkedMultiValueMap<>(headers));
        ResponseEntity<Void> response = restTemplate.postForEntity("/api/user", entity, Void.class);

        // Then - the REST request was accepted
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);

        // When - results are pulled from the output stream
        Probe<ConsumerRecord<String, UserRequest>> resultSeq = probe.request(1);
        LinkedList<ConsumerRecord<String, UserRequest>> results = LongStream.range(0, 1)
                .mapToObj(i -> resultSeq.expectNext(new FiniteDuration(20, TimeUnit.SECONDS)))
                .collect(Collectors.toCollection(LinkedList::new));

        // Then - the results are as expected
        // The request was written with the correct header
        assertAll("Records returned are correct",
                () -> assertThat(results)
                        .hasSize(1),

                () -> assertThat(results)
                        .allSatisfy(result -> {
                            assertThat(result.headers().lastHeader(Token.HEADER).value())
                                    .isEqualTo(ContractTestData.REQUEST_TOKEN.getBytes());

                            assertThat(result.value())
                                    .isEqualTo(ContractTestData.REQUEST_OBJ);
                        })
        );
    }*/

    public static class KafkaInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        static KafkaContainer kafka = new KafkaContainer("5.5.1")
                .withReuse(true);

        @Override
        public void initialize(final ConfigurableApplicationContext configurableApplicationContext) {
            configurableApplicationContext.getEnvironment().setActiveProfiles("akkatest", "debug");
            kafka.addEnv("KAFKA_AUTO_CREATE_TOPICS_ENABLE", "false");
            kafka.addEnv("KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR", "1");
            kafka.start();

            // test kafka config
            String kafkaConfig = "akka.discovery.config.services.kafka.from-config=false";
            String kafkaPort = "akka.discovery.config.services.kafka.endpoints[0].port" + kafka.getFirstMappedPort();
            TestPropertySourceUtils.addInlinedPropertiesToEnvironment(configurableApplicationContext, kafkaConfig, kafkaPort);
        }

        static void createTopics(final List<NewTopic> newTopics, final KafkaContainer kafka) throws ExecutionException, InterruptedException {
            try (AdminClient admin = AdminClient.create(Map.of(BOOTSTRAP_SERVERS_CONFIG, String.format("%s:%d", "localhost", kafka.getFirstMappedPort())))) {
                admin.createTopics(newTopics);
                LOGGER.info("created topics: " + admin.listTopics().names().get());
            }
        }

        @Configuration
        public static class Config {

            private final List<NewTopic> topics = List.of(
                    new NewTopic("success", 1, (short) 1),
                    new NewTopic("error", 1, (short) 1));

            @Bean
            KafkaContainer kafkaContainer() throws ExecutionException, InterruptedException {
                createTopics(this.topics, kafka);
                return kafka;
            }

            @Bean
            @ConditionalOnMissingBean
            static PropertiesConfigurer propertiesConfigurer(final ResourceLoader resourceLoader, final Environment environment) {
                return new PropertiesConfigurer(resourceLoader, environment);
            }

            @Bean
            @Primary
            ActorSystem actorSystem(final PropertiesConfigurer props, final KafkaContainer kafka, final ConfigurableApplicationContext context) {
                LOGGER.info("Starting Kafka with port {}", kafka.getFirstMappedPort());
                return ActorSystem.create("actor-with-overrides", props.toHoconConfig(Stream.concat(
                        props.getAllActiveProperties().entrySet().stream()
                                .filter(kafkaPort -> !kafkaPort.getKey().equals("akka.discovery.config.services.kafka.endpoints[0].port")),
                        Stream.of(new AbstractMap.SimpleEntry<>("akka.discovery.config.services.kafka.endpoints[0].port", Integer.toString(kafka.getFirstMappedPort()))))
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
