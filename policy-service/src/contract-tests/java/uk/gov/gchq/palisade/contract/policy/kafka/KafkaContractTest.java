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
package uk.gov.gchq.palisade.contract.policy.kafka;

import akka.actor.ActorSystem;
import akka.kafka.ConsumerSettings;
import akka.kafka.ProducerSettings;
import akka.kafka.Subscriptions;
import akka.kafka.javadsl.Consumer;
import akka.kafka.javadsl.Producer;
import akka.stream.Materializer;
import akka.stream.javadsl.Source;
import akka.stream.testkit.javadsl.TestSink;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.testcontainers.containers.KafkaContainer;
import scala.concurrent.duration.FiniteDuration;

import uk.gov.gchq.palisade.contract.policy.common.ContractTestData;
import uk.gov.gchq.palisade.contract.policy.common.StreamMarker;
import uk.gov.gchq.palisade.contract.policy.kafka.KafkaContractTest.KafkaInitializer;
import uk.gov.gchq.palisade.contract.policy.kafka.KafkaContractTest.KafkaInitializer.Config;
import uk.gov.gchq.palisade.service.policy.PolicyApplication;
import uk.gov.gchq.palisade.service.policy.common.Token;
import uk.gov.gchq.palisade.service.policy.exception.NoSuchPolicyException;
import uk.gov.gchq.palisade.service.policy.stream.ConsumerTopicConfiguration;
import uk.gov.gchq.palisade.service.policy.stream.ProducerTopicConfiguration;
import uk.gov.gchq.palisade.service.policy.stream.PropertiesConfigurer;
import uk.gov.gchq.palisade.service.policy.stream.SerDesConfig;

import java.io.IOException;
import java.util.AbstractMap.SimpleEntry;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toMap;
import static org.apache.kafka.clients.admin.AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

/**
 * An external requirement of the service is to connect to a pair of kafka topics.
 * The upstream "resource" topic is written to by the Resource Service and read by this service.
 * The downstream "rule" topic is written to by this service and read by the Attribute-Masking Service.
 * Upon writing to the upstream topic, appropriate messages should be written to the downstream topic.
 */
@SpringBootTest(
        classes = PolicyApplication.class,
        webEnvironment = WebEnvironment.RANDOM_PORT,
        properties = {"akka.discovery.config.services.kafka.from-config=false", "spring.cache.caffeine.spec=expireAfterWrite=2m, maximumSize=100"}
)
@Import({Config.class})
@ContextConfiguration(initializers = {KafkaInitializer.class})
@ActiveProfiles({"caffeine", "akka-test", "pre-population"})
class KafkaContractTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaContractTest.class);
    @Autowired
    private TestRestTemplate restTemplate;
    @Autowired
    private ActorSystem akkaActorSystem;
    @Autowired
    private Materializer akkaMaterializer;
    @Autowired
    private ConsumerTopicConfiguration consumerTopicConfiguration;
    @Autowired
    private ProducerTopicConfiguration producerTopicConfiguration;
    @Qualifier("objectMapper")
    @Autowired
    private ObjectMapper mapper;

    /**
     * Creates a number of requests, including a start and end record, and the body, JSON, on the right topic
     * Listens on the producer topic, then writes to the consumer topic and retrieves the processed messages from the producer topic.
     * Then asserts that the messages are valid, have the correct token and headers, the right number of messages exist, then are correctly ordered.
     * Finally, checks the body of the message has appropriately been processed, and the right objects are returned
     */
    @Test
    @DirtiesContext
    void testPolicyExistsForValidRequest() {
        // Create a variable number of requests
        // The ContractTestData.REQUEST_TOKEN maps to partition 0 of [0, 1, 2], so the akka-test yaml connects the consumer to only partition 0
        final Stream<ProducerRecord<String, JsonNode>> requests = Stream.of(
                Stream.of(ContractTestData.START_RECORD),
                ContractTestData.RECORD_NODE_FACTORY.get().limit(1L),
                Stream.of(ContractTestData.END_RECORD))
                .flatMap(Function.identity());
        final long recordCount = 3;

        // Given - we are already listening to the output
        var consumerSettings = ConsumerSettings
                .create(akkaActorSystem, new StringDeserializer(), new ResponseDeserialiser())
                .withBootstrapServers(KafkaInitializer.KAFKA.getBootstrapServers())
                .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        var probe = Consumer
                .atMostOnceSource(consumerSettings, Subscriptions.topics(producerTopicConfiguration.getTopics().get("output-topic").getName()))
                .runWith(TestSink.probe(akkaActorSystem), akkaMaterializer);

        // When - we write to the input
        var producerSettings = ProducerSettings
                .create(akkaActorSystem, new StringSerializer(), new RequestSerialiser())
                .withBootstrapServers(KafkaInitializer.KAFKA.getBootstrapServers());

        Source.fromJavaStream(() -> requests)
                .runWith(Producer.<String, JsonNode>plainSink(producerSettings), akkaMaterializer)
                .toCompletableFuture()
                .join();

        // When - results are pulled from the output stream
        var results = LongStream.range(0, recordCount)
                .mapToObj(i -> probe.requestNext(new FiniteDuration(20 + recordCount, TimeUnit.SECONDS)))
                .collect(Collectors.toCollection(LinkedList::new));

        // Then - the results are as expected

        // All messages have a correct Token in the header
        assertAll("Headers have correct token",
                () -> assertThat(results)
                        .as("Check that there are %s messages on the topic", (int) recordCount)
                        .hasSize((int) recordCount),

                () -> assertThat(results)
                        .as("Check that all messages have the correct token")
                        .allSatisfy(result ->
                                assertThat(result.headers().lastHeader(Token.HEADER).value())
                                        .as("Message headers should contain the request token %s", "test-request-token")
                                        .isEqualTo(ContractTestData.REQUEST_TOKEN.getBytes()))
        );

        // The first and last have a correct StreamMarker header
        assertAll("The START and END StreamMarkers are correct",
                () -> assertThat(results.getFirst().headers().lastHeader(StreamMarker.HEADER).value())
                        .as("The first message should contain the START Marker")
                        .isEqualTo(StreamMarker.START.toString().getBytes()),

                () -> assertThat(results.getLast().headers().lastHeader(StreamMarker.HEADER).value())
                        .as("The last message should contain the END Marker")
                        .isEqualTo(StreamMarker.END.toString().getBytes())
        );

        // All but the first and last have the expected message
        results.removeFirst();
        results.removeLast();
        assertAll("Results are correct and ordered",
                () -> assertThat(results)
                        .as("There should be one message left after the first and last result have been removed")
                        .hasSize(1),

                () -> assertThat(results)
                        .as("Check that all messages contain the correct token")
                        .allSatisfy(result ->
                                assertThat(result.headers().lastHeader(Token.HEADER).value())
                                        .as("The message should contain the request token %s", "test-request-token")
                                        .isEqualTo(ContractTestData.REQUEST_TOKEN.getBytes())),

                () -> assertThat(results.stream()
                        .map(ConsumerRecord::value)
                        .map(response -> response.get("resource").get("type").asInt())
                        .collect(Collectors.toList()))
                        .as("The results should be sorted correctly")
                        .isSorted(),

                () -> assertThat(results)
                        .as("The message should have been processed and deserialized correctly")
                        .allSatisfy(result -> {
                            assertThat(result.value().get("user").get("userId").get("id").asText())
                                    .as("The userId inside the message should be %s", "test-user-id")
                                    .isEqualTo("test-user-id");

                            assertThat(result.value().get("resourceId").asText())
                                    .as("The resourceId inside the message should be %s", "file:/test/resourceId")
                                    .isEqualTo("file:/test/resourceId");

                            assertThat(result.value().get("context").get("contents").get("purpose").asText())
                                    .as("The purpose inside the context object should be %s", "test-purpose")
                                    .isEqualTo("test-purpose");

                            assertThat(result.value().get("rules").get("message").asText())
                                    .as("The message inside the rules object should be %s", "no rules set")
                                    .isEqualTo("no rules set");

                            assertThat(result.value().get("rules").get("rules").get("1-PassThroughRule").get("@type").asText())
                                    .as("The class of the rules object inside the message should be %s", "PassThroughRule")
                                    .isEqualTo("PassThroughRule");
                        })
        );
    }

    /**
     * Creates a number of requests, including a start and end record, and the body, JSON, on the right topic
     * Listens on the producer topic, then writes to the consumer topic and retrieves the processed messages from the producer topic.
     * Then asserts that the messages are valid, have the correct token and headers, the right number of messages exist, then are correctly ordered.
     * Finally, checks the body of the message has appropriately been processed and the right objects are returned
     */
    @Test
    @DirtiesContext
    void testNoSuchPolicyExceptionIsThrown() {
        // Create a variable number of requests
        // The ContractTestData.REQUEST_TOKEN maps to partition 0 of [0, 1, 2], so the akka-test yaml connects the consumer to only partition 0
        final Stream<ProducerRecord<String, JsonNode>> requests = Stream.of(
                Stream.of(ContractTestData.START_RECORD),
                ContractTestData.NO_RESOURCE_RULES_RECORD_NODE_FACTORY.get().limit(1L),
                Stream.of(ContractTestData.END_RECORD))
                .flatMap(Function.identity());

        // Given - we are already listening to the output
        var consumerSettings = ConsumerSettings
                .create(akkaActorSystem, new StringDeserializer(), new ResponseDeserialiser())
                .withBootstrapServers(KafkaInitializer.KAFKA.getBootstrapServers())
                .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        var probe = Consumer
                .atMostOnceSource(consumerSettings, Subscriptions.topics(producerTopicConfiguration.getTopics().get("output-topic").getName()))
                .runWith(TestSink.probe(akkaActorSystem), akkaMaterializer);
        var errorProbe = Consumer
                .atMostOnceSource(consumerSettings, Subscriptions.topics(producerTopicConfiguration.getTopics().get("error-topic").getName()))
                .runWith(TestSink.probe(akkaActorSystem), akkaMaterializer);


        // When - we write to the input
        var producerSettings = ProducerSettings
                .create(akkaActorSystem, new StringSerializer(), new RequestSerialiser())
                .withBootstrapServers(KafkaInitializer.KAFKA.getBootstrapServers());

        Source.fromJavaStream(() -> requests)
                .runWith(Producer.<String, JsonNode>plainSink(producerSettings), akkaMaterializer)
                .toCompletableFuture()
                .join();


        // When - results are pulled from the output stream

        var results = LongStream.range(0, 2)
                .mapToObj(i -> probe.requestNext(new FiniteDuration(20 + 2, TimeUnit.SECONDS)))
                .collect(Collectors.toCollection(LinkedList::new));
        var errorResults = LongStream.range(0, 1)
                .mapToObj(i -> errorProbe.requestNext(new FiniteDuration(20, TimeUnit.SECONDS)))
                .collect(Collectors.toCollection(LinkedList::new));

        // Then - the results are as expected

        // All messages have a correct Token in the header
        assertAll("Headers have correct token",
                () -> assertThat(results)
                        .as("There should be two messages returned")
                        .hasSize(2),

                () -> assertThat(results)
                        .as("Check that all messages contain the correct token")
                        .allSatisfy(result ->
                                assertThat(result.headers().lastHeader(Token.HEADER).value())
                                        .as("Message headers should contain the request token %s", "test-request-token")
                                        .isEqualTo(ContractTestData.REQUEST_TOKEN.getBytes()))
        );

        // The first and last have a correct StreamMarker header
        assertAll("StreamMarkers are correct START and END",
                () -> assertThat(results.getFirst().headers().lastHeader(StreamMarker.HEADER).value())
                        .as("The first message should contain the START Marker")
                        .isEqualTo(StreamMarker.START.toString().getBytes()),

                () -> assertThat(results.getLast().headers().lastHeader(StreamMarker.HEADER).value())
                        .as("The last message should contain the END Marker")
                        .isEqualTo(StreamMarker.END.toString().getBytes())
        );

        // All but the first and last have the expected message
        results.removeFirst();
        results.removeLast();
        assertThat(results)
                .as("After removing the first and last message there should be nothing left")
                .isEmpty();

        assertAll("Asserting on the error topic",
                // One error is produced
                () -> assertThat(errorResults)
                        .as("There should be one message on the error topic")
                        .hasSize(1),

                // The error has the relevant headers, including the token
                () -> assertThat(errorResults)
                        .as("Check that all messages contain the correct token")
                        .allSatisfy(result ->
                                assertThat(result.headers().lastHeader(Token.HEADER).value())
                                        .as("Message headers should contain the request token %s", "test-request-token")
                                        .isEqualTo(ContractTestData.REQUEST_TOKEN.getBytes())),

                // The error has a message that contains the throwable exception, and the message
                () -> assertThat(errorResults.get(0).value().get("error").get("message").asText())
                        .as("The error message within the result on the error queue should be %s",
                                "No Resource Rules found for the resource")
                        .isEqualTo(NoSuchPolicyException.class.getName() + ": No Resource Rules found for the resource: file:/test/noRulesResource")
        );
    }

    /**
     * Tests the rest endpoint used for mocking a kafka entry point exists and is working as expected, returns a HTTP.ACCEPTED.
     * Then checks the token and headers are correct
     */
    @Test
    @DirtiesContext
    void testRestEndpoint() {
        // Given - we are already listening to the service input
        var consumerSettings = ConsumerSettings
                .create(akkaActorSystem, SerDesConfig.resourceKeyDeserializer(), SerDesConfig.resourceValueDeserializer())
                .withGroupId("test-group")
                .withBootstrapServers(KafkaInitializer.KAFKA.getBootstrapServers())
                .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        var probe = Consumer
                .atMostOnceSource(consumerSettings, Subscriptions.topics(consumerTopicConfiguration.getTopics().get("input-topic").getName()))
                .runWith(TestSink.probe(akkaActorSystem), akkaMaterializer);

        // When - we POST to the rest endpoint
        var headers = Collections.singletonMap(Token.HEADER, Collections.singletonList(ContractTestData.REQUEST_TOKEN));
        var entity = new HttpEntity<>(ContractTestData.REQUEST_OBJ, new LinkedMultiValueMap<>(headers));
        var response = restTemplate.postForEntity("/api/policy", entity, Void.class);

        // Then - the REST request was accepted
        assertThat(response.getStatusCode())
                .as("Check that the REST call was accepted")
                .isEqualTo(HttpStatus.ACCEPTED);

        // When - results are pulled from the output stream
        var results = LongStream.range(0, 1)
                .mapToObj(i -> probe.requestNext(new FiniteDuration(20, TimeUnit.SECONDS)))
                .collect(Collectors.toCollection(LinkedList::new));

        // Then - the results are as expected
        // The request was written with the correct header
        assertAll("Records returned are correct",
                () -> assertThat(results)
                        .as("There should be one message on the kafka input-topic")
                        .hasSize(1),

                () -> assertThat(results)
                        .as("Check that all messages have the correct headers and body")
                        .allSatisfy(result -> {
                            assertThat(result.headers().lastHeader(Token.HEADER).value())
                                    .as("Message headers should contain the request token %s", "test-request-token")
                                    .isEqualTo(ContractTestData.REQUEST_TOKEN.getBytes());
                            assertThat(result.value())
                                    .usingRecursiveComparison()
                                    .as("The message from the input topic should have been processed and deserialized " +
                                            "correctly and should match the same object in ContractTestData")
                                    .isEqualTo(ContractTestData.REQUEST_OBJ);
                        })
        );
    }

    /**
     * Creates a number of requests, including a start and end record, and the body, JSON, on the right topic
     * Listens on the producer topic, then writes to the consumer topic and retrieves the processed messages from the producer topic.
     * Then asserts that the messages are valid, have the correct token and headers, the right number of messages exist, then are correctly ordered.
     * Finally, checks the body of the message has appropriately been processed, and the right objects are returned
     * In this case, the resourceRules applied to the resource have redacted the resource so nothing is returned to the user
     */
    @Test
    @DirtiesContext
    void testResourceRulesRedactedResource() {
        // Create a variable number of requests
        // The ContractTestData.REQUEST_TOKEN maps to partition 0 of [0, 1, 2], so the akka-test yaml connects the consumer to only partition 0
        final Stream<ProducerRecord<String, JsonNode>> requests = Stream.of(
                Stream.of(ContractTestData.START_RECORD),
                ContractTestData.REDACTED_RESOURCE_RULES_RECORD_NODE_FACTORY.get().limit(1L),
                Stream.of(ContractTestData.END_RECORD))
                .flatMap(Function.identity());

        // Given - we are already listening to the output
        var consumerSettings = ConsumerSettings
                .create(akkaActorSystem, new StringDeserializer(), new ResponseDeserialiser())
                .withBootstrapServers(KafkaContractTest.KafkaInitializer.KAFKA.getBootstrapServers())
                .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        var probe = Consumer
                .atMostOnceSource(consumerSettings, Subscriptions.topics(producerTopicConfiguration.getTopics().get("output-topic").getName()))
                .runWith(TestSink.probe(akkaActorSystem), akkaMaterializer);

        // When - we write to the input
        var producerSettings = ProducerSettings
                .create(akkaActorSystem, new StringSerializer(), new RequestSerialiser())
                .withBootstrapServers(KafkaContractTest.KafkaInitializer.KAFKA.getBootstrapServers());

        Source.fromJavaStream(() -> requests)
                .runWith(Producer.<String, JsonNode>plainSink(producerSettings), akkaMaterializer)
                .toCompletableFuture()
                .join();


        // When - results are pulled from the output stream
        // record count set to 2, as one record will be removed as no policy exists for it
        var results = LongStream.range(0, 2)
                .mapToObj(i -> probe.requestNext(new FiniteDuration(20 + 2, TimeUnit.SECONDS)))
                .collect(Collectors.toCollection(LinkedList::new));

        // Then - the results are as expected

        // All messages have a correct Token in the header
        assertAll("Headers have correct token",
                () -> assertThat(results)
                        .as("There should be two messages on the queue")
                        .hasSize(2),

                () -> assertThat(results)
                        .as("Check that all messages contain the correct token")
                        .allSatisfy(result ->
                                assertThat(result.headers().lastHeader(Token.HEADER).value())
                                        .as("Message headers should contain the request token %s", "test-request-token")
                                        .isEqualTo(ContractTestData.REQUEST_TOKEN.getBytes()))
        );

        // The first and last have a correct StreamMarker header
        assertAll("StreamMarkers are correct START and END",
                () -> assertThat(results.getFirst().headers().lastHeader(StreamMarker.HEADER).value())
                        .as("The first message should contain the START Marker")
                        .isEqualTo(StreamMarker.START.toString().getBytes()),

                () -> assertThat(results.getLast().headers().lastHeader(StreamMarker.HEADER).value())
                        .as("The last message should contain the END Marker")
                        .isEqualTo(StreamMarker.END.toString().getBytes())
        );

        // Remove the START and END messages from the results list
        results.removeFirst();
        results.removeLast();
        assertThat(results)
                .as("The results list should be empty after removing the START and END messages")
                .isEmpty();
    }

    // Serializer for upstream test input
    class RequestSerialiser implements Serializer<JsonNode> {
        @Override
        public byte[] serialize(final String s, final JsonNode policyRequest) {
            try {
                return mapper.writeValueAsBytes(policyRequest);
            } catch (JsonProcessingException e) {
                throw new SerializationFailedException("Failed to serialize " + policyRequest.toString(), e);
            }
        }
    }

    // Deserializer for downstream test output
    class ResponseDeserialiser implements Deserializer<JsonNode> {
        @Override
        public JsonNode deserialize(final String s, final byte[] policyResponse) {
            try {
                return mapper.readTree(policyResponse);
            } catch (IOException e) {
                throw new SerializationFailedException("Failed to deserialize " + new String(policyResponse), e);
            }
        }
    }

    public static class KafkaInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
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
            configurableApplicationContext.getEnvironment().setActiveProfiles("caffeine", "akka-test", "pre-population", "debug");
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
                    new NewTopic("resource", 1, (short) 1),
                    new NewTopic("rule", 1, (short) 1),
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
            ActorSystem actorSystem(final PropertiesConfigurer props, final KafkaContainer kafka, final ConfigurableApplicationContext context) {
                LOGGER.info("Starting Kafka with port {}", kafka.getFirstMappedPort());
                return ActorSystem.create("actor-with-overrides", props.toHoconConfig(Stream.concat(
                        props.getAllActiveProperties().entrySet().stream()
                                .filter(kafkaPort -> !kafkaPort.getKey().equals("akka.discovery.config.services.kafka.endpoints[0].port")),
                        Stream.of(new SimpleEntry<>("akka.discovery.config.services.kafka.endpoints[0].port", Integer.toString(kafka.getFirstMappedPort()))))
                        .peek(entry -> LOGGER.info("Config key {} = {}", entry.getKey(), entry.getValue()))
                        .collect(toMap(Entry::getKey, Entry::getValue))));
            }

            @Bean
            @Primary
            Materializer materializer(final ActorSystem system) {
                return Materializer.createMaterializer(system);
            }
        }
    }
}
