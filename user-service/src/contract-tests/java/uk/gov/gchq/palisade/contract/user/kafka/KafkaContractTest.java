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

package uk.gov.gchq.palisade.contract.user.kafka;

import akka.actor.ActorSystem;
import akka.kafka.ConsumerSettings;
import akka.kafka.ProducerSettings;
import akka.kafka.Subscriptions;
import akka.kafka.javadsl.Consumer;
import akka.kafka.javadsl.Producer;
import akka.stream.Materializer;
import akka.stream.javadsl.Source;
import akka.stream.testkit.TestSubscriber.Probe;
import akka.stream.testkit.javadsl.TestSink;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.util.LinkedMultiValueMap;
import scala.concurrent.duration.FiniteDuration;

import uk.gov.gchq.palisade.contract.user.common.StreamMarker;
import uk.gov.gchq.palisade.contract.user.kafka.KafkaInitializer.Config;
import uk.gov.gchq.palisade.contract.user.kafka.KafkaInitializer.RequestSerialiser;
import uk.gov.gchq.palisade.contract.user.kafka.KafkaInitializer.ResponseDeserialiser;
import uk.gov.gchq.palisade.service.user.UserApplication;
import uk.gov.gchq.palisade.service.user.common.Context;
import uk.gov.gchq.palisade.service.user.common.Token;
import uk.gov.gchq.palisade.service.user.common.user.User;
import uk.gov.gchq.palisade.service.user.common.user.UserId;
import uk.gov.gchq.palisade.service.user.exception.NoSuchUserIdException;
import uk.gov.gchq.palisade.service.user.model.UserRequest;
import uk.gov.gchq.palisade.service.user.stream.ConsumerTopicConfiguration;
import uk.gov.gchq.palisade.service.user.stream.ProducerTopicConfiguration;
import uk.gov.gchq.palisade.service.user.stream.SerDesConfig;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static uk.gov.gchq.palisade.contract.user.common.ContractTestData.END_RECORD;
import static uk.gov.gchq.palisade.contract.user.common.ContractTestData.NO_USER_ID_RECORD_NODE_FACTORY;
import static uk.gov.gchq.palisade.contract.user.common.ContractTestData.RECORD_NODE_FACTORY;
import static uk.gov.gchq.palisade.contract.user.common.ContractTestData.REQUEST_OBJ;
import static uk.gov.gchq.palisade.contract.user.common.ContractTestData.REQUEST_TOKEN;
import static uk.gov.gchq.palisade.contract.user.common.ContractTestData.START_RECORD;
import static uk.gov.gchq.palisade.service.user.stream.SerDesConfig.responseValueDeserializer;

/**
 * An external requirement of the service is to connect to a pair of kafka topics.
 * The upstream "request" topic is written to by the Palisade-service and read by this service.
 * The downstream "user" topic is written to by this service and read by the Resource-service.
 * The downstream "error" topic is written to by this service when errors are thrown, and they are read by the Audit-Service
 * Upon writing to the upstream topic, appropriate messages should be written to the downstream topic.
 */
@SpringBootTest(
        classes = UserApplication.class,
        webEnvironment = WebEnvironment.RANDOM_PORT,
        properties = {"akka.discovery.config.services.kafka.from-config=false", "spring.cache.caffeine.spec=expireAfterWrite=1m, maximumSize=100"}
)
@Import(Config.class)
@ContextConfiguration(initializers = KafkaInitializer.class)
@ActiveProfiles({"caffeine", "akka-test", "pre-population"})
class KafkaContractTest {

    @Autowired
    private TestRestTemplate restTemplate;
    @Autowired
    private ActorSystem akkaActorSystem;
    @Autowired
    private Materializer akkaMaterialiser;
    @Autowired
    private ConsumerTopicConfiguration consumerTopicConfiguration;
    @Autowired
    private ProducerTopicConfiguration producerTopicConfiguration;

    @Test
    @DirtiesContext
    void testRequestSet() throws JsonProcessingException {
        // Only 1 request will be received by the user-service
        // The ContractTestData.REQUEST_TOKEN maps to partition 0 of [0, 1, 2], so the akka-test yaml connects the consumer to only partition 0
        final Stream<ProducerRecord<String, JsonNode>> requests = Stream.of(
                Stream.of(START_RECORD),
                RECORD_NODE_FACTORY.get().limit(1L),
                Stream.of(END_RECORD))
                .flatMap(Function.identity());
        final long recordCount = 3;

        // Given - we are already listening to the output
        ConsumerSettings<String, JsonNode> consumerSettings = ConsumerSettings
                .create(akkaActorSystem, new StringDeserializer(), new ResponseDeserialiser())
                .withBootstrapServers(KafkaInitializer.KAFKA_CONTAINER.getBootstrapServers())
                .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        Probe<ConsumerRecord<String, JsonNode>> probe = Consumer
                .atMostOnceSource(consumerSettings, Subscriptions.topics(producerTopicConfiguration.getTopics().get("output-topic").getName()))
                .runWith(TestSink.probe(akkaActorSystem), akkaMaterialiser);

        // When - we write to the input
        ProducerSettings<String, JsonNode> producerSettings = ProducerSettings
                .create(akkaActorSystem, new StringSerializer(), new RequestSerialiser())
                .withBootstrapServers(KafkaInitializer.KAFKA_CONTAINER.getBootstrapServers());

        Source.fromJavaStream(() -> requests)
                .runWith(Producer.plainSink(producerSettings), akkaMaterialiser);

        // When - results are pulled from the output stream
        Probe<ConsumerRecord<String, JsonNode>> resultSeq = probe.request(recordCount);
        LinkedList<ConsumerRecord<String, JsonNode>> results = LongStream.range(0, recordCount)
                .mapToObj(i -> resultSeq.expectNext(new FiniteDuration(20 + recordCount, TimeUnit.SECONDS)))
                .collect(Collectors.toCollection(LinkedList::new));

        // Then - the results are as expected

        // All messages have a correct Token in the header
        assertAll("Headers have correct token",
                () -> assertThat(results)
                        .hasSize((int) recordCount),

                () -> assertThat(results)
                        .allSatisfy(result ->
                                assertThat(result.headers().lastHeader(Token.HEADER).value())
                                        .isEqualTo(REQUEST_TOKEN.getBytes()))
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
        var userResponse = responseValueDeserializer(results.getFirst().value());
        assertAll("Results are correct and ordered",
                () -> assertThat(results)
                        .as("Check that there is one message on the output topic")
                        .hasSize(1),
                () -> assertThat(results)
                        .as("Check the message contains the correct headers")
                        .allSatisfy(result ->
                                assertThat(result.headers().lastHeader(Token.HEADER).value())
                                        .isEqualTo(REQUEST_TOKEN.getBytes())),

                () -> assertThat(userResponse)
                        .as("Check the response has all the correct User information")
                        .extracting("userId", "resourceId", "context", "user")
                        .containsExactly(REQUEST_OBJ.getUserId(),
                                "/test/resourceId",
                                new Context().purpose("purpose"),
                                new User().userId(new UserId().id(REQUEST_OBJ.getUserId())).roles("role").auths("auth"))
        );
    }

    @Test
    @DirtiesContext
    void testNoSuchUserIdExceptionIsThrown() {
        // Create a variable number of requests
        // The ContractTestData.REQUEST_TOKEN maps to partition 0 of [0, 1, 2], so the akka-test yaml connects the consumer to only partition 0
        final Stream<ProducerRecord<String, JsonNode>> requests = Stream.of(
                Stream.of(START_RECORD),
                NO_USER_ID_RECORD_NODE_FACTORY.get().limit(1L),
                Stream.of(END_RECORD))
                .flatMap(Function.identity());

        // Given - we are already listening to the output
        ConsumerSettings<String, JsonNode> consumerSettings = ConsumerSettings
                .create(akkaActorSystem, new StringDeserializer(), new ResponseDeserialiser())
                .withBootstrapServers(KafkaInitializer.KAFKA_CONTAINER.getBootstrapServers())
                .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        Probe<ConsumerRecord<String, JsonNode>> probe = Consumer
                .atMostOnceSource(consumerSettings, Subscriptions.topics(producerTopicConfiguration.getTopics().get("output-topic").getName()))
                .runWith(TestSink.probe(akkaActorSystem), akkaMaterialiser);
        Probe<ConsumerRecord<String, JsonNode>> errorProbe = Consumer
                .atMostOnceSource(consumerSettings, Subscriptions.topics(producerTopicConfiguration.getTopics().get("error-topic").getName()))
                .runWith(TestSink.probe(akkaActorSystem), akkaMaterialiser);

        // When - we write to the input
        ProducerSettings<String, JsonNode> producerSettings = ProducerSettings
                .create(akkaActorSystem, new StringSerializer(), new RequestSerialiser())
                .withBootstrapServers(KafkaInitializer.KAFKA_CONTAINER.getBootstrapServers());

        Source.fromJavaStream(() -> requests)
                .runWith(Producer.plainSink(producerSettings), akkaMaterialiser);


        // When - results are pulled from the output stream
        // record count set to 2, as one record will be removed as no policy exists for it
        Probe<ConsumerRecord<String, JsonNode>> resultSeq = probe.request(2);
        Probe<ConsumerRecord<String, JsonNode>> errorResultSeq = errorProbe.request(1);

        LinkedList<ConsumerRecord<String, JsonNode>> results = LongStream.range(0, 2)
                .mapToObj(i -> resultSeq.expectNext(new FiniteDuration(20 + 2, TimeUnit.SECONDS)))
                .collect(Collectors.toCollection(LinkedList::new));
        LinkedList<ConsumerRecord<String, JsonNode>> errorResults = LongStream.range(0, 1)
                .mapToObj(i -> errorResultSeq.expectNext(new FiniteDuration(20, TimeUnit.SECONDS)))
                .collect(Collectors.toCollection(LinkedList::new));

        // Then - the results are as expected

        // Assert that the `START` and `END` messages have been added to the output topic,
        // check the header contains the token and check that there is no Response message on the topic
        assertAll("Asserting on the output topic",
                () -> assertThat(results)
                        .as("Check that there are 2 messages on the output topic")
                        .hasSize(2),

                () -> assertThat(results)
                        .as("All messages should contain the token header")
                        .allSatisfy(result ->
                                assertThat(result.headers().lastHeader(Token.HEADER).value())
                                        .isEqualTo(REQUEST_TOKEN.getBytes())),

                () -> assertAll("Start and End of stream markers have the correct headers",
                        () -> assertThat(results.getFirst().headers().lastHeader(StreamMarker.HEADER).value())
                                .isEqualTo(StreamMarker.START.toString().getBytes()),
                        () -> assertThat(results.getLast().headers().lastHeader(StreamMarker.HEADER).value())
                                .isEqualTo(StreamMarker.END.toString().getBytes())
                ),

                () -> assertAll("After removing the start and end of stream message",
                        results::removeFirst,
                        results::removeLast,
                        () -> assertThat(results).isEmpty()
                )
        );

        // Assert that there is a message on the error topic, check the header contains the token and check the error message value
        assertThat(errorResults)
                .hasSize(1)
                .allSatisfy(result -> assertThat(result.headers().lastHeader(Token.HEADER).value())
                        .isEqualTo(REQUEST_TOKEN.getBytes()))
                .allSatisfy(result -> assertThat(errorResults.get(0).value().get("error").get("message").asText())
                        .isEqualTo(NoSuchUserIdException.class.getName() + ": No userId matching invalid-user-id found in cache"));
    }

    @Test
    @DirtiesContext
    void testRestEndpoint() {
        // Given - we are already listening to the service input
        ConsumerSettings<String, UserRequest> consumerSettings = ConsumerSettings
                .create(akkaActorSystem, SerDesConfig.requestKeyDeserialiser(), SerDesConfig.requestValueDeserialiser())
                .withGroupId("test-group")
                .withBootstrapServers(KafkaInitializer.KAFKA_CONTAINER.getBootstrapServers())
                .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        Probe<ConsumerRecord<String, UserRequest>> probe = Consumer
                .atMostOnceSource(consumerSettings, Subscriptions.topics(consumerTopicConfiguration.getTopics().get("input-topic").getName()))
                .runWith(TestSink.probe(akkaActorSystem), akkaMaterialiser);

        // When - we POST to the rest endpoint
        Map<String, List<String>> headers = Collections.singletonMap(Token.HEADER, Collections.singletonList(REQUEST_TOKEN));
        HttpEntity<UserRequest> entity = new HttpEntity<>(REQUEST_OBJ, new LinkedMultiValueMap<>(headers));
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
                        .as("Check one message is on the output topic")
                        .hasSize(1),

                () -> assertThat(results)
                        .as("Check the message has the correct header")
                        .allSatisfy(result -> {
                            assertThat(result.headers().lastHeader(Token.HEADER).value())
                                    .isEqualTo(REQUEST_TOKEN.getBytes());

                            assertThat(result.value())
                                    .as("Check the UserRequest has been processed correctly")
                                    .usingRecursiveComparison()
                                    .isEqualTo(REQUEST_OBJ);
                        })
        );
    }

}
