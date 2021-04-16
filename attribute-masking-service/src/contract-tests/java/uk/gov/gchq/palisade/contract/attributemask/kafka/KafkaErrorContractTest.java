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

package uk.gov.gchq.palisade.contract.attributemask.kafka;

import akka.actor.ActorSystem;
import akka.kafka.ConsumerSettings;
import akka.kafka.ProducerSettings;
import akka.kafka.Subscriptions;
import akka.kafka.javadsl.Consumer;
import akka.kafka.javadsl.Producer;
import akka.stream.Materializer;
import akka.stream.javadsl.Source;
import akka.stream.testkit.javadsl.TestSink;
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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import scala.concurrent.duration.FiniteDuration;

import uk.gov.gchq.palisade.contract.attributemask.ContractTestData;
import uk.gov.gchq.palisade.contract.attributemask.KafkaInitializer;
import uk.gov.gchq.palisade.contract.attributemask.KafkaInitializer.ErrorDeserializer;
import uk.gov.gchq.palisade.contract.attributemask.KafkaInitializer.RequestSerializer;
import uk.gov.gchq.palisade.service.attributemask.AttributeMaskingApplication;
import uk.gov.gchq.palisade.service.attributemask.common.Context;
import uk.gov.gchq.palisade.service.attributemask.common.Token;
import uk.gov.gchq.palisade.service.attributemask.common.resource.LeafResource;
import uk.gov.gchq.palisade.service.attributemask.common.resource.impl.FileResource;
import uk.gov.gchq.palisade.service.attributemask.common.rule.Rules;
import uk.gov.gchq.palisade.service.attributemask.common.user.User;
import uk.gov.gchq.palisade.service.attributemask.model.AttributeMaskingRequest;
import uk.gov.gchq.palisade.service.attributemask.repository.PersistenceLayer;
import uk.gov.gchq.palisade.service.attributemask.service.AttributeMaskingAspect;
import uk.gov.gchq.palisade.service.attributemask.service.AttributeMaskingService;
import uk.gov.gchq.palisade.service.attributemask.service.LeafResourceMasker;
import uk.gov.gchq.palisade.service.attributemask.stream.ProducerTopicConfiguration;

import java.util.LinkedList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

/**
 * An external requirement of the service is to connect to a pair of kafka topics.
 * The upstream "rule" topic is written to by the policy-service and read by this service.
 * The downstream topic in this test is the error topic, used to send audit error messages to the Audit Service
 */
@SpringBootTest(
        classes = AttributeMaskingApplication.class,
        webEnvironment = WebEnvironment.RANDOM_PORT,
        properties = "akka.discovery.config.services.kafka.from-config=false"
)
@Import({KafkaInitializer.Config.class})
@ContextConfiguration(initializers = {KafkaInitializer.class},
        classes = {KafkaErrorContractTest.Config.class})
@ActiveProfiles({"db-test", "akka-test"})
class KafkaErrorContractTest {

    @Autowired
    private ActorSystem akkaActorSystem;
    @Autowired
    private Materializer akkaMaterializer;
    @Autowired
    private ProducerTopicConfiguration producerTopicConfiguration;

    @Test
    @DirtiesContext
    void testPersistenceFailureAuditing() {
        // Create a variable number of requests
        final Stream<ProducerRecord<String, JsonNode>> requests = Stream.of(
                Stream.of(ContractTestData.START_RECORD),
                ContractTestData.RECORD_NODE_FACTORY.get().limit(1L),
                Stream.of(ContractTestData.END_RECORD))
                .flatMap(Function.identity());

        // Given - we are already listening to the errors
        var errorConsumerSettings = ConsumerSettings
                .create(akkaActorSystem, new StringDeserializer(), new ErrorDeserializer())
                .withBootstrapServers(KafkaInitializer.KAFKA_CONTAINER.getBootstrapServers())
                .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        var errorProbe = Consumer
                .atMostOnceSource(errorConsumerSettings, Subscriptions.topics(producerTopicConfiguration.getTopics().get("error-topic").getName()))
                .runWith(TestSink.probe(akkaActorSystem), akkaMaterializer);

        // When - we write to the input
        var producerSettings = ProducerSettings
                .create(akkaActorSystem, new StringSerializer(), new RequestSerializer())
                .withBootstrapServers(KafkaInitializer.KAFKA_CONTAINER.getBootstrapServers());

        Source.fromJavaStream(() -> requests)
                .runWith(Producer.<String, JsonNode>plainSink(producerSettings), akkaMaterializer);

        // When - errors are pulled from the error stream, pulling the number of errors thrown
        var errorSeq = errorProbe.request(1);
        var errors = LongStream.range(0, 1)
                .mapToObj(i -> errorSeq.expectNext(new FiniteDuration(21, TimeUnit.SECONDS)))
                .collect(Collectors.toCollection(LinkedList::new));

        // Then - the errors are as expected
        assertAll("Errors are correct",
                // The correct number of errors were received
                () -> assertThat(errors)
                        .as("Check that one message has been returned")
                        .hasSize(1),

                () -> assertThat(errors)
                        .as("Check that the message has the correct contents")
                        .allSatisfy(error ->
                                assertAll("Error records all satisfy requirements",
                                        () -> assertThat(error.headers().lastHeader(Token.HEADER).value())
                                                .as("Assert that the original request is preserved")
                                                .isEqualTo(ContractTestData.REQUEST_TOKEN.getBytes()),

                                        () -> assertThat(error)
                                                .as("Assert that after extracting the AuditErrorMessage, it is as expected")
                                                .extracting(ConsumerRecord::value)
                                                .usingRecursiveComparison()
                                                .ignoringFieldsOfTypes(Throwable.class)
                                                .ignoringFields("timestamp")
                                                .isEqualTo(ContractTestData.AUDIT_ERROR_MESSAGE),

                                        () -> assertThat(error.value().getError())
                                                .as("Assert that the error message inside the AuditErrorMessage is the same")
                                                .isExactlyInstanceOf(Throwable.class)
                                                .hasMessageContaining(ContractTestData.AUDIT_ERROR_MESSAGE.getError().getMessage())
                                )
                        )
        );
    }

    @Configuration
    public static class Config {

        @Primary
        @Bean
        ExceptionalPersistenceLayer persistenceLayer() {
            return new ExceptionalPersistenceLayer();
        }

        @Primary
        @Bean
        LeafResourceMasker simpleLeafResourceMasker() {
            // Delete all additional attributes (if a FileResource)
            return (LeafResource x) -> {
                if (x instanceof FileResource) {
                    throw new RuntimeException("Cannot mask");
                } else {
                    return x;
                }
            };
        }

        @Primary
        @Bean
        AttributeMaskingService simpleAttributeMaskingService(final ExceptionalPersistenceLayer persistenceLayer, final LeafResourceMasker resourceMasker) {
            return new AttributeMaskingService(persistenceLayer, resourceMasker);
        }

        @Primary
        @Bean
        AttributeMaskingAspect attributeMaskingAspect() {
            return new AttributeMaskingAspect();
        }

    }

    /**
     * Override the Persistence Layer in the service and force it to throw an exception, so we can test the services auditing
     */
    private static class ExceptionalPersistenceLayer implements PersistenceLayer {

        @Override
        public CompletableFuture<AttributeMaskingRequest> putAsync(final String token, final User user, final LeafResource resource, final Context context, final Rules<?> rules) {
            throw new RuntimeException("There was an issue with persisting the resource");
        }
    }
}
