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

package uk.gov.gchq.palisade.contract.filteredresource.kafka;

import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.http.javadsl.Http;
import akka.http.javadsl.model.ContentType;
import akka.http.javadsl.model.ContentTypes;
import akka.http.javadsl.model.HttpHeader;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.headers.RawHeader;
import akka.http.javadsl.model.ws.Message;
import akka.http.javadsl.model.ws.WebSocketRequest;
import akka.http.javadsl.model.ws.WebSocketUpgradeResponse;
import akka.http.scaladsl.model.ws.TextMessage.Strict;
import akka.japi.Pair;
import akka.kafka.ConsumerSettings;
import akka.kafka.Subscriptions;
import akka.kafka.javadsl.Consumer;
import akka.stream.Materializer;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Keep;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import akka.stream.testkit.TestSubscriber.Probe;
import akka.stream.testkit.javadsl.TestSink;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.serializer.support.SerializationFailedException;
import org.springframework.lang.NonNull;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import scala.concurrent.duration.FiniteDuration;

import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.contract.filteredresource.kafka.KafkaRestWebSocketContractTest.KafkaInitializer;
import uk.gov.gchq.palisade.contract.filteredresource.kafka.KafkaRestWebSocketContractTest.KafkaInitializer.ErrorDeserializer;
import uk.gov.gchq.palisade.contract.filteredresource.kafka.KafkaRestWebSocketContractTest.RedisInitializer;
import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.resource.impl.FileResource;
import uk.gov.gchq.palisade.resource.impl.SystemResource;
import uk.gov.gchq.palisade.service.SimpleConnectionDetail;
import uk.gov.gchq.palisade.service.filteredresource.FilteredResourceApplication;
import uk.gov.gchq.palisade.service.filteredresource.exception.NoResourcesObservedException;
import uk.gov.gchq.palisade.service.filteredresource.exception.NoStartMarkerObservedException;
import uk.gov.gchq.palisade.service.filteredresource.model.AuditErrorMessage;
import uk.gov.gchq.palisade.service.filteredresource.model.FilteredResourceRequest;
import uk.gov.gchq.palisade.service.filteredresource.model.MessageType;
import uk.gov.gchq.palisade.service.filteredresource.model.StreamMarker;
import uk.gov.gchq.palisade.service.filteredresource.model.Token;
import uk.gov.gchq.palisade.service.filteredresource.model.TopicOffsetMessage;
import uk.gov.gchq.palisade.service.filteredresource.model.WebSocketMessage;
import uk.gov.gchq.palisade.service.filteredresource.repository.TokenOffsetPersistenceLayer;
import uk.gov.gchq.palisade.service.filteredresource.stream.ProducerTopicConfiguration;
import uk.gov.gchq.palisade.service.filteredresource.stream.PropertiesConfigurer;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toMap;
import static org.apache.kafka.clients.admin.AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = FilteredResourceApplication.class, webEnvironment = WebEnvironment.NONE, properties = "akka.discovery.config.services.kafka.from-config=false")
@Import({KafkaRestWebSocketContractTest.KafkaInitializer.Config.class})
@ContextConfiguration(initializers = {KafkaInitializer.class, RedisInitializer.class})
@ActiveProfiles("k8s")
class KafkaRestWebSocketContractTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaRestWebSocketContractTest.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int REDIS_PORT = 6379;
    private static final String HOST = "localhost";
    @Value("${server.port}")
    private Integer port;

    private final ActorSystem system = ActorSystem.create("contract-test");
    private final Materializer materializer = Materializer.createMaterializer(system);
    private final Http http = Http.get(system);

    @Autowired
    private TokenOffsetPersistenceLayer persistenceLayer;
    @Autowired
    private ActorSystem akkaActorSystem;
    @Autowired
    private Materializer akkaMaterializer;
    @Autowired
    private ProducerTopicConfiguration producerTopicConfiguration;

    private static class ParameterizedArguments implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(final ExtensionContext extensionContext) throws Exception {
            // Builders
            Function<String, FilteredResourceRequest> resourceBuilder = resourceId -> FilteredResourceRequest.Builder.create()
                    .withUserId("userId")
                    .withResourceId("file:/file/")
                    .withContext(new Context().purpose("purpose"))
                    .withResource(new FileResource()
                            .id("file:/file/" + resourceId)
                            .serialisedFormat("fmt")
                            .type("type")
                            .connectionDetail(new SimpleConnectionDetail()
                                    .serviceName("data-service"))
                            .parent(new SystemResource().id("file:/file/")));
            Function<Long, TopicOffsetMessage> offsetBuilder = TopicOffsetMessage.Builder.create()
                    ::withQueuePointer;
            BiFunction<String, LeafResource, WebSocketMessage> responseBuilder = (token, leafResource) -> WebSocketMessage.Builder.create()
                    .withType(MessageType.RESOURCE)
                    .withHeader(Token.HEADER, token)
                    .noHeaders()
                    .withBody(leafResource);
            // Special instances
            HttpHeader startHeader = RawHeader.create(StreamMarker.HEADER, String.valueOf(StreamMarker.START));
            HttpHeader endHeader = RawHeader.create(StreamMarker.HEADER, String.valueOf(StreamMarker.END));
            WebSocketMessage ctsMsg = WebSocketMessage.Builder.create().withType(MessageType.CTS).noHeaders().noBody();
            Function<String, WebSocketMessage> completeMsgBuilder = (token) -> WebSocketMessage.Builder.create()
                    .withType(MessageType.COMPLETE)
                    .withHeader(Token.HEADER, token)
                    .noHeaders()
                    .noBody();
            return Stream.of(
                    // Test for 'early' client - topic offset message has offset
                    Arguments.of(
                            "test-token-1",
                            List.of(
                                    Pair.create(List.of(RawHeader.create(Token.HEADER, "test-token-1"), startHeader), null),
                                    Pair.create(List.of(RawHeader.create(Token.HEADER, "test-token-1")), resourceBuilder.apply("resource.1")),
                                    Pair.create(List.of(RawHeader.create(Token.HEADER, "test-token-1")), resourceBuilder.apply("resource.2")),
                                    Pair.create(List.of(RawHeader.create(Token.HEADER, "test-token-1")), resourceBuilder.apply("resource.3")),
                                    Pair.create(List.of(RawHeader.create(Token.HEADER, "test-token-1"), endHeader), null)
                            ),
                            List.of(
                                    Pair.create(List.of(RawHeader.create(Token.HEADER, "test-token-1")), offsetBuilder.apply(0L))
                            ),
                            List.of(),
                            Map.of(),
                            List.of(
                                    ctsMsg, ctsMsg, ctsMsg, ctsMsg
                            ),
                            List.of(
                                    responseBuilder.apply("test-token-1", resourceBuilder.apply("resource.1").getResource()),
                                    responseBuilder.apply("test-token-1", resourceBuilder.apply("resource.2").getResource()),
                                    responseBuilder.apply("test-token-1", resourceBuilder.apply("resource.3").getResource()),
                                    completeMsgBuilder.apply("test-token-1")
                            ),
                            List.of()
                    ),
                    // Test for 'late' client - persistence has offset
                    Arguments.of(
                            "test-token-2",
                            List.of(
                                    Pair.create(List.of(RawHeader.create(Token.HEADER, "test-token-2"), startHeader), null),
                                    Pair.create(List.of(RawHeader.create(Token.HEADER, "test-token-2")), resourceBuilder.apply("resource.1")),
                                    Pair.create(List.of(RawHeader.create(Token.HEADER, "test-token-2")), resourceBuilder.apply("resource.2")),
                                    Pair.create(List.of(RawHeader.create(Token.HEADER, "test-token-2")), resourceBuilder.apply("resource.3")),
                                    Pair.create(List.of(RawHeader.create(Token.HEADER, "test-token-2"), endHeader), null)
                            ),
                            List.of(),
                            List.of(),
                            Map.of(
                                    "test-token-2", 0L
                            ),
                            List.of(
                                    ctsMsg, ctsMsg, ctsMsg, ctsMsg
                            ),
                            List.of(
                                    responseBuilder.apply("test-token-2", resourceBuilder.apply("resource.1").getResource()),
                                    responseBuilder.apply("test-token-2", resourceBuilder.apply("resource.2").getResource()),
                                    responseBuilder.apply("test-token-2", resourceBuilder.apply("resource.3").getResource()),
                                    completeMsgBuilder.apply("test-token-2")
                            ),
                            List.of()
                    ),
                    Arguments.of(
                            "test-token-3",
                            List.of(
                                    //No Start Marker
                                    Pair.create(List.of(RawHeader.create(Token.HEADER, "test-token-3")), resourceBuilder.apply("resource.1")),
                                    Pair.create(List.of(RawHeader.create(Token.HEADER, "test-token-3")), resourceBuilder.apply("resource.2")),
                                    Pair.create(List.of(RawHeader.create(Token.HEADER, "test-token-3")), resourceBuilder.apply("resource.3")),
                                    Pair.create(List.of(RawHeader.create(Token.HEADER, "test-token-3"), endHeader), null)
                            ),
                            List.of(
                                    Pair.create(List.of(RawHeader.create(Token.HEADER, "test-token-3")), offsetBuilder.apply(0L))
                            ),
                            List.of(),
                            Map.of(),
                            List.of(
                                    ctsMsg, ctsMsg, ctsMsg, ctsMsg
                            ),
                            List.of(
                                    responseBuilder.apply("test-token-3", resourceBuilder.apply("resource.1").getResource()),
                                    responseBuilder.apply("test-token-3", resourceBuilder.apply("resource.2").getResource()),
                                    responseBuilder.apply("test-token-3", resourceBuilder.apply("resource.3").getResource()),
                                    completeMsgBuilder.apply("test-token-3")
                            ),
                            List.of(
                                    AuditErrorMessage.Builder.create().withUserId("userId")
                                            .withResourceId("file:/file/resource.1")
                                            .withContext(new Context().purpose("purpose"))
                                            .withAttributes(Collections.emptyMap())
                                            .withError(new NoStartMarkerObservedException())
                            )
                    ),
                    // Test no resources
                    Arguments.of(
                            "test-token-4",
                            List.of(
                                    Pair.create(List.of(RawHeader.create(Token.HEADER, "test-token-4"), startHeader), null),
                                    Pair.create(List.of(RawHeader.create(Token.HEADER, "test-token-4"), endHeader), null)
                            ),
                            List.of(
                                    Pair.create(List.of(RawHeader.create(Token.HEADER, "test-token-4")), offsetBuilder.apply(0L))
                            ),
                            List.of(),
                            Map.of(),
                            List.of(
                                    ctsMsg
                            ),
                            List.of(
                                    WebSocketMessage.Builder.create().withType(MessageType.COMPLETE).withHeader(Token.HEADER, "test-token-4").noHeaders().noBody()
                            ),
                            List.of(
                                    AuditErrorMessage.Builder.create().withUserId("unknown")
                                            .withResourceId("unknown")
                                            .withContext(new Context().purpose("unknown"))
                                            .withAttributes(Collections.emptyMap())
                                            .withError(new NoResourcesObservedException("No Resources were observed for token: " + "test-token-3"))
                            )
                    )
            );
        }
    }

    @ParameterizedTest
    @ArgumentsSource(ParameterizedArguments.class)
    void testRunKafka(
            // Parameterised input
            final String requestToken,
            final List<Pair<Iterable<HttpHeader>, FilteredResourceRequest>> maskedResourceTopic,
            final List<Pair<Iterable<HttpHeader>, TopicOffsetMessage>> maskedResourceOffsetTopic,
            final List<Pair<Iterable<HttpHeader>, AuditErrorMessage>> errorTopic,
            final Map<String, Long> offsetsPersistence,
            final List<WebSocketMessage> websocketRequests,
            // Expected output
            final List<WebSocketMessage> websocketResponses,
            final List<AuditErrorMessage> auditErrorMessages
    ) throws InterruptedException, ExecutionException, TimeoutException {
        ContentType jsonType = ContentTypes.APPLICATION_JSON;

        // Given
        // POST maskedResource to KafkaController
        maskedResourceTopic.forEach(resource -> http
                .singleRequest(HttpRequest.POST(String.format("http://%s:%d/api/masked-resource", HOST, port))
                        .withHeaders(resource.first())
                        .withEntity(jsonType, serialize(resource.second()).getBytes()))
                .toCompletableFuture()
                .thenAccept(response -> assertThat(response.status().intValue()).isEqualTo(202))
                .join());
        // Given POST maskedResourceOffset to KafkaController - runnable is then called 'late' to simulate arrival of offsets *after* client request
        Runnable postOffsets = () -> maskedResourceOffsetTopic.forEach(offset -> http
                .singleRequest(HttpRequest.POST(String.format("http://%s:%d/api/masked-resource-offset", HOST, port))
                        .withHeaders(offset.first())
                        .withEntity(jsonType, serialize(offset.second()).getBytes()))
                .toCompletableFuture()
                .thenAccept(response -> assertThat(response.status().intValue()).isEqualTo(202))
                .join());
        // POST error to KafkaController
        errorTopic.forEach(error -> http
                .singleRequest(HttpRequest.POST(String.format("http://%s:%d/api/error", HOST, port))
                        .withHeaders(error.first())
                        .withEntity(jsonType, serialize(error.second()).getBytes()))
                .toCompletableFuture()
                .thenAccept(response -> assertThat(response.status().intValue()).isEqualTo(202))
                .join());
        // Write offsets to persistence
        offsetsPersistence.forEach((token, offset) -> persistenceLayer
                .overwriteOffset(token, offset)
                .join());

        // When
        // Send each websocketMessage request and receive responses
        Source<Message, NotUsed> wsMsgSource = Source.fromIterator(websocketRequests::iterator).map(this::writeTextMessage);
        Sink<Message, CompletionStage<List<WebSocketMessage>>> listSink = Flow.<Message>create().map(this::readTextMessage).toMat(Sink.seq(), Keep.right());
        // Create client Sink/Source Flow (send the payload, collect the responses)
        Flow<Message, Message, CompletionStage<List<WebSocketMessage>>> clientFlow = Flow.fromSinkAndSourceMat(listSink, wsMsgSource, Keep.left());
        Pair<CompletionStage<WebSocketUpgradeResponse>, CompletionStage<List<WebSocketMessage>>> request = http.singleWebSocketRequest(
                WebSocketRequest.create(String.format("ws://%s:%s/resource/" + requestToken, HOST, port)),
                clientFlow,
                materializer);
        // Get the (HTTP) response, a websocket upgrade
        request.first().toCompletableFuture()
                .get(1, TimeUnit.SECONDS);

        // Late POST of offsets after client request has been initialized
        postOffsets.run();

        // Get the result of the client sink, a list of (WebSocket) responses
        LinkedList<WebSocketMessage> actualResponses = new LinkedList<>(request.second().toCompletableFuture()
                .get(30 + websocketRequests.size(), TimeUnit.SECONDS));

        // Then
        // Assert each received response matches up with the expected
        assertThat(actualResponses)
                .as("Testing Websocket Response")
                .isEqualTo(websocketResponses);

        // When you read off the error queue
        ConsumerSettings<String, AuditErrorMessage> consumerSettings = ConsumerSettings
                .create(akkaActorSystem, new StringDeserializer(), new ErrorDeserializer())
                .withBootstrapServers(KafkaInitializer.KAFKA_CONTAINER.getBootstrapServers())
                .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        Probe<ConsumerRecord<String, AuditErrorMessage>> errorProbe = Consumer
                .atMostOnceSource(consumerSettings, Subscriptions.topics(producerTopicConfiguration.getTopics().get("error-topic").getName()))
                .runWith(TestSink.probe(akkaActorSystem), akkaMaterializer);

        Probe<ConsumerRecord<String, AuditErrorMessage>> errorResultSeq = errorProbe.request(errorTopic.size() + auditErrorMessages.size());
        LongStream.range(0, errorTopic.size())
                .mapToObj(i -> errorResultSeq.expectNext(new FiniteDuration(20, TimeUnit.SECONDS)))
                .forEach(ignored -> LOGGER.info("ignoring message {}", ignored));

        LinkedList<ConsumerRecord<String, AuditErrorMessage>> errorResults = LongStream.range(0, auditErrorMessages.size())
                .mapToObj(i -> errorResultSeq.expectNext(new FiniteDuration(20, TimeUnit.SECONDS)))
                .collect(Collectors.toCollection(LinkedList::new));

        // Assert that there is a message on the error topic, check the header contains the token and check the error message value
        assertThat(errorResults)
                .extracting(ConsumerRecord::value)
                .isEqualTo(auditErrorMessages);
    }


    // Handle deserialising JSON TextMessages to WebSocketMessages
    private WebSocketMessage readTextMessage(final Message message) {
        // Akka will sometimes convert a StrictMessage to a StreamedMessage, so we have to handle both cases here
        StringBuilder builder;
        if (message.asTextMessage().isStrict()) {
            builder = new StringBuilder(message.asTextMessage().getStrictText());
        } else {
            builder = message.asTextMessage().getStreamedText()
                    .runFold(new StringBuilder(), StringBuilder::append, this.system)
                    .toCompletableFuture().join();
        }
        return deserialize(builder.toString(), WebSocketMessage.class);
    }

    // Handle serialising WebSocketMessages to JSON TextMessages
    private Message writeTextMessage(final WebSocketMessage message) {
        return new Strict(serialize(message));
    }

    // Handle serializing Objects to JSON Strings
    private static <T> T deserialize(final String json, final Class<T> clazz) {
        try {
            return MAPPER.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            throw new SerializationFailedException("Failed to write message", e);
        }
    }

    // Handle serializing Objects to JSON Strings
    private static String serialize(final Object o) {
        try {
            return MAPPER.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            throw new SerializationFailedException("Failed to write message", e);
        }
    }


    public static class RedisInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        static final GenericContainer<?> REDIS = new GenericContainer<>("redis:6-alpine")
                .withExposedPorts(REDIS_PORT)
                .withReuse(true);

        @Override
        public void initialize(@NonNull final ConfigurableApplicationContext context) {
            // Start container
            REDIS.start();

            // Override Redis configuration
            String redisContainerIP = "spring.redis.host=" + REDIS.getContainerIpAddress();
            // Configure the testcontainer random port
            String redisContainerPort = "spring.redis.port=" + REDIS.getMappedPort(REDIS_PORT);
            // Override the configuration at runtime
            TestPropertySourceUtils.addInlinedPropertiesToEnvironment(context, redisContainerIP, redisContainerPort);
        }
    }


    public static class KafkaInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        static final KafkaContainer KAFKA_CONTAINER = new KafkaContainer("5.5.1")
                .withReuse(true);

        @Override
        public void initialize(final ConfigurableApplicationContext configurableApplicationContext) {
            KAFKA_CONTAINER.addEnv("KAFKA_AUTO_CREATE_TOPICS_ENABLE", "false");
            KAFKA_CONTAINER.addEnv("KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR", "1");
            KAFKA_CONTAINER.start();

            // test kafka config
            String kafkaConfig = "akka.discovery.config.services.kafka.from-config=false";
            String kafkaPort = "akka.discovery.config.services.kafka.endpoints[0].port=" + KAFKA_CONTAINER.getFirstMappedPort();
            TestPropertySourceUtils.addInlinedPropertiesToEnvironment(configurableApplicationContext, kafkaConfig, kafkaPort);
        }

        static void createTopics(final List<NewTopic> newTopics, final KafkaContainer kafka) throws ExecutionException, InterruptedException {
            try (AdminClient admin = AdminClient.create(Map.of(BOOTSTRAP_SERVERS_CONFIG, String.format("%s:%d", "localhost", kafka.getFirstMappedPort())))) {
                admin.createTopics(newTopics);
                LOGGER.info("Created topics: {}", admin.listTopics().names().get());
            }
        }

        @Configuration
        public static class Config {

            private final List<NewTopic> topics = List.of(
                    new NewTopic("masked-resource", 1, (short) 1),
                    new NewTopic("masked-resource-offset", 1, (short) 1),
                    new NewTopic("success", 1, (short) 1),
                    new NewTopic("error", 1, (short) 1));

            @Bean
            KafkaContainer kafkaContainer() throws ExecutionException, InterruptedException {
                createTopics(this.topics, KAFKA_CONTAINER);
                return KAFKA_CONTAINER;
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
                return ActorSystem.create("actor-with-overrides", props.toHoconConfig(Stream
                        .concat(props.getAllActiveProperties().entrySet().stream()
                                        .filter(kafkaPort -> !kafkaPort.getKey().equals("akka.discovery.config.services.kafka.endpoints[0].port")),
                                Stream.of(new AbstractMap.SimpleEntry<>("akka.discovery.config.services.kafka.endpoints[0].port", Integer.toString(kafka.getFirstMappedPort()))))
                        .peek(entry -> LOGGER.debug("Config key {} = {}", entry.getKey(), entry.getValue()))
                        .collect(toMap(Map.Entry::getKey, Map.Entry::getValue))));
            }

            @Bean
            @Primary
            Materializer materializer(final ActorSystem system) {
                return Materializer.createMaterializer(system);
            }
        }

        // Deserializer for downstream test error output
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
    }
}
