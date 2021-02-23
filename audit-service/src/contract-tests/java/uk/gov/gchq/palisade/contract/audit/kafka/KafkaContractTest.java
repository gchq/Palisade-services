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
import org.junit.jupiter.api.AfterEach;
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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.testcontainers.containers.KafkaContainer;

import uk.gov.gchq.palisade.contract.audit.ContractTestData;
import uk.gov.gchq.palisade.service.audit.AuditApplication;
import uk.gov.gchq.palisade.service.audit.config.AuditServiceConfigProperties;
import uk.gov.gchq.palisade.service.audit.service.AuditService;
import uk.gov.gchq.palisade.service.audit.stream.PropertiesConfigurer;

import java.io.File;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toMap;
import static org.apache.kafka.clients.admin.AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.http.HttpStatus.ACCEPTED;
import static uk.gov.gchq.palisade.contract.audit.ContractTestData.BAD_ERROR_MESSAGE_NODE_FACTORY;
import static uk.gov.gchq.palisade.contract.audit.ContractTestData.BAD_SUCCESS_RECORD_NODE_FACTORY;
import static uk.gov.gchq.palisade.contract.audit.ContractTestData.BAD_SUCCESS_REQUEST_OBJ;
import static uk.gov.gchq.palisade.contract.audit.ContractTestData.ERROR_RECORD_NODE_FACTORY;
import static uk.gov.gchq.palisade.contract.audit.ContractTestData.ERROR_REQUEST_OBJ;
import static uk.gov.gchq.palisade.contract.audit.ContractTestData.GOOD_SUCCESS_RECORD_NODE_FACTORY;
import static uk.gov.gchq.palisade.contract.audit.ContractTestData.GOOD_SUCCESS_REQUEST_OBJ;
import static uk.gov.gchq.palisade.contract.audit.ContractTestData.REQUEST_TOKEN;
import static uk.gov.gchq.palisade.service.audit.model.Token.HEADER;

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
//@ExtendWith(MockitoExtension.class)
class KafkaContractTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaContractTest.class);

    @Autowired()
    Serializer<String> keySerializer;
    @Autowired
    Serializer<JsonNode> valueSerializer;
    @Autowired
    ActorSystem akkaActorSystem;
    @Autowired
    Materializer akkaMaterializer;
    @Autowired
    AuditServiceConfigProperties auditServiceConfigProperties;

    @SpyBean
    private AuditService auditService;

    private final Function<String, Integer> fileCount = (final String prefix) -> Arrays
        .stream(new File(auditServiceConfigProperties.getErrorDirectory()).listFiles())
        .filter(file -> file.getName().startsWith(prefix))
        .collect(Collectors.toSet())
        .size();

    private final Supplier<Integer> currentErrorCount = () -> fileCount.apply("Error");
    private final Supplier<Integer> currentSuccessCount = () -> fileCount.apply("Success");

    @AfterEach
    void tearDown() {
        Arrays.stream(new File(auditServiceConfigProperties.getErrorDirectory()).listFiles())
            .filter(file -> (file.getName().startsWith("Success") || file.getName().startsWith("Error")))
            .peek(file -> LOGGER.info("Deleting file {}", file.getName()))
            .forEach(File::deleteOnExit);
    }

    @Test
    @DirtiesContext
    void testErrorRequestSet() throws InterruptedException {

        // Add some messages on the error topic
        // The ContractTestData.REQUEST_TOKEN maps to partition 0 of [0, 1, 2], so the akkatest yaml connects the consumer to only partition 0
        var requests = ERROR_RECORD_NODE_FACTORY.get().limit(3L);

        // When - we write to the input
        runStreamOf(requests);
        waitForService();

        // Then - check the audit service has invoked the audit method 3 times
        verify(auditService, timeout(3000).times(3)).audit(anyString(), any());
    }

    @Test
    @DirtiesContext
    void testGoodSuccessRequestSet() throws InterruptedException {

        // Add some messages on the success topic
        // The ContractTestData.REQUEST_TOKEN maps to partition 0 of [0, 1, 2], so the akka-test yaml connects the consumer to only partition 0
        var requests = GOOD_SUCCESS_RECORD_NODE_FACTORY.get().limit(3L);

        // When - we write to the input
        runStreamOf(requests);
        waitForService();

        // Then - check the audit service has invoked the audit method 3 times
        verify(auditService, timeout(3000).times(3)).audit(anyString(), any());
    }

    @Test
    @DirtiesContext
    void testGoodAndBadSuccessRequestSet() throws InterruptedException {

        // Add 2 `Good` and 2 `Bad` success messages to the success topic
        // The ContractTestData.REQUEST_TOKEN maps to partition 0 of [0, 1, 2], so the akka-test yaml connects the consumer to only partition 0
        var requests = Stream.of(
                GOOD_SUCCESS_RECORD_NODE_FACTORY.get().limit(1L),
                BAD_SUCCESS_RECORD_NODE_FACTORY.get().limit(2L),
                GOOD_SUCCESS_RECORD_NODE_FACTORY.get().limit(1L))
            .flatMap(Function.identity());

        // When - we write to the input
        runStreamOf(requests);
        waitForService();

        // Then - check the audit service has invoked the audit method for the 2 `Good` requests
        verify(auditService, timeout(3000).times(2)).audit(anyString(), any());
    }

    @Test
    @DirtiesContext
    void testRestEndpointForErrorMessage(
            @Autowired final TestRestTemplate restTemplate) throws InterruptedException {

        // Pass an error message to 'error' rest endpoint
        // When - we POST to the rest endpoint
        var headers = singletonMap(HEADER, singletonList(REQUEST_TOKEN));
        var httpEntity = new HttpEntity<>(ERROR_REQUEST_OBJ, new LinkedMultiValueMap<>(headers));
        var responseEntity = restTemplate.postForEntity("/api/error", httpEntity, Void.class);

        waitForService();

        // Then - check the REST request was accepted
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);

        // Then - check the audit service has invoked the audit method
        verify(auditService, timeout(3000).times(1)).audit(anyString(), any());
    }

    @Test
    @DirtiesContext
    void testRestEndpointForGoodSuccessMessage(
            @Autowired final TestRestTemplate restTemplate) throws InterruptedException {

        // Pass a success message to 'success' rest endpoint
        // When - we POST to the rest endpoint
        var headers = singletonMap(HEADER, singletonList(REQUEST_TOKEN));
        var httpEntity = new HttpEntity<>(GOOD_SUCCESS_REQUEST_OBJ, new LinkedMultiValueMap<>(headers));
        var responseEntity = restTemplate.postForEntity("/api/success", httpEntity, Void.class);

        waitForService();

        // Then - check the REST request was accepted
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);

        // Then - check the audit service has invoked the audit method
        Mockito.verify(auditService, Mockito.timeout(3000).times(1)).audit(anyString(), any());
    }

    @Test
    @DirtiesContext
    void testRestEndpointForBadSuccessMessage(
            @Autowired final TestRestTemplate restTemplate) throws InterruptedException {

        // Pass a success message to 'success' rest endpoint
        // When - we POST to the rest endpoint
        var headers = singletonMap(HEADER, singletonList(REQUEST_TOKEN));
        var httpEntity = new HttpEntity<>(BAD_SUCCESS_REQUEST_OBJ, new LinkedMultiValueMap<>(headers));
        var responseEntity = restTemplate.postForEntity("/api/success", httpEntity, Void.class);

        waitForService();

        // Then - check the REST request was accepted
        assertThat(responseEntity.getStatusCode()).isEqualTo(ACCEPTED);

        // Then - check the audit service has invoked the audit method
        verify(auditService, timeout(3000).times(0)).audit(anyString(), any());
    }

    @Test
    @DirtiesContext
    void testFailedErrorDeserialization(
            @Autowired final AuditServiceConfigProperties auditServiceConfigProperties) throws InterruptedException {

        var expectedErrorCount = currentErrorCount.get() + 1;

        // Add a message to the 'error' topic
        // The ContractTestData.REQUEST_TOKEN maps to partition 0 of [0, 1, 2], so the akka-test yaml connects the consumer to only partition 0
        var requests = BAD_ERROR_MESSAGE_NODE_FACTORY.get().limit(1L);

        // When - we write to the input and wait
        runStreamOf(requests);
        waitForService();

        // Then check an "Error-..." file has been created
        var actualErrorCount = currentErrorCount.get();

        assertThat(actualErrorCount)
            .as("Check exactly 1 'Error' file has been created")
            .isEqualTo(expectedErrorCount);
    }

    @Test
    @DirtiesContext
    void testFailedSuccessDeserialization() throws InterruptedException {

        var expectedSuccessCount = currentSuccessCount.get() + 1;

        // Add a message to the 'success' topic
        // The ContractTestData.REQUEST_TOKEN maps to partition 0 of [0, 1, 2], so the akka-test yaml connects the consumer to only partition 0
        var requests = ContractTestData.BAD_SUCCESS_MESSAGE_NODE_FACTORY.get().limit(1L);

        // When - we write to the input
        runStreamOf(requests);
        waitForService();

        // Then check a "Success-..." file has been created
        var actualSuccessCount = currentSuccessCount.get();

        assertThat(actualSuccessCount)
            .as("Check exactly 1 'Success' file has been created")
            .isEqualTo(expectedSuccessCount);

    }

    @SuppressWarnings("resource")
    private final void runStreamOf(final Stream<ProducerRecord<String, JsonNode>> requests) {

        var bootstrapServers = KafkaInitializer.kafka.getBootstrapServers();

        // When - we write to the input
        ProducerSettings<String, JsonNode> producerSettings = ProducerSettings
            .create(akkaActorSystem, keySerializer, valueSerializer)
            .withBootstrapServers(bootstrapServers);

        Source.fromJavaStream(() -> requests)
            .runWith(Producer.plainSink(producerSettings), akkaMaterializer)
            .toCompletableFuture().join();

    }

    private void waitForService() throws InterruptedException {
        TimeUnit.SECONDS.sleep(2);
    }

    public static class KafkaInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

        static KafkaContainer kafka = new KafkaContainer("5.5.1").withReuse(true);

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
            var host = String.format("%s:%d", "localhost", kafka.getFirstMappedPort());
            var adminProperties = Map.<String, Object>of(BOOTSTRAP_SERVERS_CONFIG, host);
            try (var admin = AdminClient.create(adminProperties)) {
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
                var portKey = "akka.discovery.config.services.kafka.endpoints[0].port";
                var port = Integer.toString(kafka.getFirstMappedPort());

                // remove current port if found and then add back in with the port Kafka is
                // listening on
                var config = props.toHoconConfig(Stream
                    .concat(
                        props.getAllActiveProperties()
                            .entrySet()
                            .stream()
                            .filter(kafkaPort -> !kafkaPort.getKey().equals(portKey)),
                        Stream.of(new AbstractMap.SimpleEntry<>(portKey, port)))
                    .collect(toMap(Map.Entry::getKey, Map.Entry::getValue)));

                return ActorSystem.create("actor-with-overrides", config);
            }

            @Bean
            @Primary
            Materializer materializer(final ActorSystem system) {
                return Materializer.createMaterializer(system);
            }

            @Bean
            Serializer<JsonNode> requestSerializer(@Autowired final ObjectMapper objectMapper) {
                return (final String s, final JsonNode auditRequest) -> {
                    try {
                        return objectMapper.writeValueAsBytes(auditRequest);
                    } catch (JsonProcessingException e) {
                        throw new SerializationFailedException("Failed to serialize " + auditRequest.toString(), e);
                    }
                };
            }

            @Bean
            Serializer<String> stringSerializer() {
                return new StringSerializer();
            }

        }
    }

}
