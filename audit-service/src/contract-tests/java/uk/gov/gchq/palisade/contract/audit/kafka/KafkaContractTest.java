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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.testcontainers.containers.KafkaContainer;

import uk.gov.gchq.palisade.contract.audit.ContractTestData;
import uk.gov.gchq.palisade.service.audit.AuditApplication;
import uk.gov.gchq.palisade.service.audit.config.AuditServiceConfigProperties;
import uk.gov.gchq.palisade.service.audit.model.AuditErrorMessage;
import uk.gov.gchq.palisade.service.audit.model.AuditSuccessMessage;
import uk.gov.gchq.palisade.service.audit.model.Token;
import uk.gov.gchq.palisade.service.audit.service.AuditService;
import uk.gov.gchq.palisade.service.audit.stream.ConsumerTopicConfiguration;
import uk.gov.gchq.palisade.service.audit.stream.PropertiesConfigurer;

import java.io.File;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toMap;
import static org.apache.kafka.clients.admin.AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

/**
 * An external requirement of the service is to connect to a pair of upstream kafka topics.
 * <ol>
 *     <li>The "error" topic can be written to by any service that encounters an error when processing a request</li>
 *     <li>The "success" topic should only be written to by the filtered-resource-service or the data-service</li>
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
@ActiveProfiles({"akka-test"})
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

    @Autowired
    private TestRestTemplate restTemplate;
    @Autowired
    private ActorSystem akkaActorSystem;
    @Autowired
    private Materializer akkaMaterializer;
    @Autowired
    private ConsumerTopicConfiguration consumerTopicConfiguration;
    @Autowired
    private AuditServiceConfigProperties auditServiceConfigProperties;
    @SpyBean
    private AuditService auditService;

    @Test
    @DirtiesContext
    void testErrorRequestSet() throws InterruptedException {
        // Add some messages on the error topic
        // The ContractTestData.REQUEST_TOKEN maps to partition 0 of [0, 1, 2], so the akkatest yaml connects the consumer to only partition 0
        final Stream<ProducerRecord<String, JsonNode>> requests = ContractTestData.ERROR_RECORD_NODE_FACTORY.get().limit(3L);

        // When - we write to the input
        ProducerSettings<String, JsonNode> producerSettings = ProducerSettings
                .create(akkaActorSystem, new StringSerializer(), new RequestSerializer())
                .withBootstrapServers(KafkaInitializer.kafka.getBootstrapServers());

        Source.fromJavaStream(() -> requests)
                .runWith(Producer.plainSink(producerSettings), akkaMaterializer)
                .toCompletableFuture().join();

        TimeUnit.SECONDS.sleep(1);

        // Then - check the audit service has invoked the audit method 3 times
        Mockito.verify(auditService, Mockito.timeout(3000).times(3)).audit(anyString(), any());
    }

    @Test
    @DirtiesContext
    void testGoodSuccessRequestSet() throws InterruptedException {
        // Add some messages on the success topic
        // The ContractTestData.REQUEST_TOKEN maps to partition 0 of [0, 1, 2], so the akka-test yaml connects the consumer to only partition 0
        final Stream<ProducerRecord<String, JsonNode>> requests = ContractTestData.GOOD_SUCCESS_RECORD_NODE_FACTORY.get().limit(3L);

        // When - we write to the input
        ProducerSettings<String, JsonNode> producerSettings = ProducerSettings
                .create(akkaActorSystem, new StringSerializer(), new RequestSerializer())
                .withBootstrapServers(KafkaInitializer.kafka.getBootstrapServers());

        Source.fromJavaStream(() -> requests)
                .runWith(Producer.plainSink(producerSettings), akkaMaterializer);

        TimeUnit.SECONDS.sleep(1);

        // Then - check the audit service has invoked the audit method 3 times
        Mockito.verify(auditService, Mockito.timeout(3000).times(3)).audit(anyString(), any());
    }

    @Test
    @DirtiesContext
    void testGoodAndBadSuccessRequestSet() throws InterruptedException {
        // Add 2 `Good` and 2 `Bad` success messages to the success topic
        // The ContractTestData.REQUEST_TOKEN maps to partition 0 of [0, 1, 2], so the akka-test yaml connects the consumer to only partition 0
        final Stream<ProducerRecord<String, JsonNode>> requests = Stream.of(
                ContractTestData.GOOD_SUCCESS_RECORD_NODE_FACTORY.get().limit(1L),
                ContractTestData.BAD_SUCCESS_RECORD_NODE_FACTORY.get().limit(2L),
                ContractTestData.GOOD_SUCCESS_RECORD_NODE_FACTORY.get().limit(1L))
                .flatMap(Function.identity());

        // When - we write to the input
        ProducerSettings<String, JsonNode> producerSettings = ProducerSettings
                .create(akkaActorSystem, new StringSerializer(), new RequestSerializer())
                .withBootstrapServers(KafkaInitializer.kafka.getBootstrapServers());

        Source.fromJavaStream(() -> requests)
                .runWith(Producer.plainSink(producerSettings), akkaMaterializer);

        TimeUnit.SECONDS.sleep(1);

        // Then - check the audit service has invoked the audit method for the 2 `Good` requests
        Mockito.verify(auditService, Mockito.timeout(3000).times(2)).audit(anyString(), any());
    }

    @Test
    @DirtiesContext
    void testRestEndpointForErrorMessage() throws InterruptedException {
        // Pass an error message to 'error' rest endpoint
        // When - we POST to the rest endpoint
        Map<String, List<String>> headers = Collections.singletonMap(Token.HEADER, Collections.singletonList(ContractTestData.REQUEST_TOKEN));
        HttpEntity<AuditErrorMessage> entity = new HttpEntity<>(ContractTestData.ERROR_REQUEST_OBJ, new LinkedMultiValueMap<>(headers));
        ResponseEntity<Void> response = restTemplate.postForEntity("/api/error", entity, Void.class);

        TimeUnit.SECONDS.sleep(1);

        // Then - check the REST request was accepted
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);

        // Then - check the audit service has invoked the audit method
        Mockito.verify(auditService, Mockito.timeout(3000).times(1)).audit(anyString(), any());
    }

    @Test
    @DirtiesContext
    void testRestEndpointForGoodSuccessMessage() throws InterruptedException {
        // Pass a success message to 'success' rest endpoint
        // When - we POST to the rest endpoint
        Map<String, List<String>> headers = Collections.singletonMap(Token.HEADER, Collections.singletonList(ContractTestData.REQUEST_TOKEN));
        HttpEntity<AuditSuccessMessage> entity = new HttpEntity<>(ContractTestData.GOOD_SUCCESS_REQUEST_OBJ, new LinkedMultiValueMap<>(headers));
        ResponseEntity<Void> response = restTemplate.postForEntity("/api/success", entity, Void.class);

        TimeUnit.SECONDS.sleep(1);

        // Then - check the REST request was accepted
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);

        // Then - check the audit service has invoked the audit method
        Mockito.verify(auditService, Mockito.timeout(3000).times(1)).audit(anyString(), any());
    }

    @Test
    @DirtiesContext
    void testRestEndpointForBadSuccessMessage() throws InterruptedException {
        // Pass a success message to 'success' rest endpoint
        // When - we POST to the rest endpoint
        Map<String, List<String>> headers = Collections.singletonMap(Token.HEADER, Collections.singletonList(ContractTestData.REQUEST_TOKEN));
        HttpEntity<AuditSuccessMessage> entity = new HttpEntity<>(ContractTestData.BAD_SUCCESS_REQUEST_OBJ, new LinkedMultiValueMap<>(headers));
        ResponseEntity<Void> response = restTemplate.postForEntity("/api/success", entity, Void.class);

        TimeUnit.SECONDS.sleep(1);

        // Then - check the REST request was accepted
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);

        // Then - check the audit service has invoked the audit method
        Mockito.verify(auditService, Mockito.timeout(3000).times(0)).audit(anyString(), any());
    }

    @Test
    @DirtiesContext
    void testFailedErrorDeserialization() throws InterruptedException {
        // Add a message to the 'error' topic
        // The ContractTestData.REQUEST_TOKEN maps to partition 0 of [0, 1, 2], so the akka-test yaml connects the consumer to only partition 0
        final Stream<ProducerRecord<String, JsonNode>> requests = ContractTestData.BAD_ERROR_MESSAGE_NODE_FACTORY.get().limit(1L);

        // When - we write to the input
        ProducerSettings<String, JsonNode> producerSettings = ProducerSettings
                .create(akkaActorSystem, new StringSerializer(), new RequestSerializer())
                .withBootstrapServers(KafkaInitializer.kafka.getBootstrapServers());

        Source.fromJavaStream(() -> requests)
                .runWith(Producer.plainSink(producerSettings), akkaMaterializer);

        TimeUnit.SECONDS.sleep(2);

        // Then check an "Error-..." file has been created
        Set<File> expectedFileSet = Arrays.stream(new File(".").listFiles())
                .filter(file -> file.getName().startsWith("Error"))
                .collect(Collectors.toSet());
        Set<File> actualFileSet = Arrays.stream(new File(auditServiceConfigProperties.getErrorDirectory()).listFiles())
                .filter(file -> file.getName().startsWith("Error"))
                .collect(Collectors.toSet());

        assertAll(
                () -> assertThat(actualFileSet).as("Check at least 1 'Error' file has been created").isNotEmpty(),
                () -> assertThat(actualFileSet).as("Check the 'Error' files are the same").containsAll(expectedFileSet)
        );
    }

    @Test
    @DirtiesContext
    void testFailedSuccessDeserialization() throws InterruptedException {
        // Add a message to the 'success' topic
        // The ContractTestData.REQUEST_TOKEN maps to partition 0 of [0, 1, 2], so the akka-test yaml connects the consumer to only partition 0
        final Stream<ProducerRecord<String, JsonNode>> requests = ContractTestData.BAD_SUCCESS_MESSAGE_NODE_FACTORY.get().limit(1L);

        // When - we write to the input
        ProducerSettings<String, JsonNode> producerSettings = ProducerSettings
                .create(akkaActorSystem, new StringSerializer(), new RequestSerializer())
                .withBootstrapServers(KafkaInitializer.kafka.getBootstrapServers());

        Source.fromJavaStream(() -> requests)
                .runWith(Producer.plainSink(producerSettings), akkaMaterializer);

        TimeUnit.SECONDS.sleep(2);

        // Then check an "Success-..." file has been created
        Set<File> expectedFileSet = Arrays.stream(new File(".").listFiles())
                .filter(file -> file.getName().startsWith("Success"))
                .collect(Collectors.toSet());
        Set<File> actualFileSet = Arrays.stream(new File(auditServiceConfigProperties.getErrorDirectory()).listFiles())
                .filter(file -> file.getName().startsWith("Success"))
                .collect(Collectors.toSet());

        assertAll(
                () -> assertThat(actualFileSet).as("Check at least 1 'Success' file has been created").isNotEmpty(),
                () -> assertThat(actualFileSet).as("Check the 'Success' files are the same").containsAll(expectedFileSet)
        );
    }

    public static class KafkaInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        static KafkaContainer kafka = new KafkaContainer("5.5.1")
                .withReuse(true);

        @Override
        public void initialize(final ConfigurableApplicationContext configurableApplicationContext) {
            configurableApplicationContext.getEnvironment().setActiveProfiles("akka-test");
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
