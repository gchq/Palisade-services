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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import uk.gov.gchq.palisade.service.data.web.DataController;

@WebMvcTest(controllers = {DataController.class})
@ContextConfiguration(classes = {KafkaContractTest2.class, DataController.class})
@ActiveProfiles("akka-test")
public class KafkaContractTest2 {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /*
    @MockBean
    private AuditableDataService serviceMock;

    @MockBean
    private AuditMessageService auditMessageServiceMock;

    @Autowired
    private MockMvc mockMvc;

   @Autowired
    private ActorSystem akkaActorSystem;


    @Autowired
    private Materializer akkaMaterializer;

    @Autowired
    private ProducerTopicConfiguration producerTopicConfiguration;
*/

    /**
     * Tests the rest endpoint used for mocking a kafka entry point exists and is working as expected, returns HTTP.INTERNAL_SERVER_ERROR.
     * Then checks the token and message are correct.
     */

    @Test
    @DirtiesContext
    void testRestEndpointError() {

/*
        when(serviceMock.authoriseRequest(any()))
                .thenReturn(CompletableFuture.completedFuture(AUDITABLE_DATA_READER_REQUEST_WITH_ERROR));

        when(auditMessageServiceMock.auditMessage(any()))
                .thenReturn(CompletableFuture.completedFuture(null));

 */

/*
        // Given - we are already listening to the service input
        ConsumerSettings<String, AuditMessage> consumerSettings = ConsumerSettings
                .create(akkaActorSystem, TestSerDesConfig.keyDeserializer(), TestSerDesConfig.valueDeserializer())
                .withGroupId("test-group")
                .withBootstrapServers(KafkaContractTest.KafkaInitializer.KAFKA.getBootstrapServers())
                .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        final long recordCount = 1;


/*
        // Given - we are already listening to the service input
        ConsumerSettings<String, AuditMessage> consumerSettings = ConsumerSettings
                .create(akkaActorSystem, TestSerDesConfig.keyDeserializer(), TestSerDesConfig.valueDeserializer())
                .withGroupId("test-group")
                .withBootstrapServers(KafkaContractTest.KafkaInitializer.KAFKA.getBootstrapServers())
                .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        final long recordCount = 1;

        TestSubscriber.Probe<ConsumerRecord<String, AuditMessage>> probe = Consumer
                .atMostOnceSource(consumerSettings, Subscriptions.topics(producerTopicConfiguration.getTopics().get("error-topic").getName()))
                .runWith(TestSink.probe(akkaActorSystem), akkaMaterializer);
*/

        /*
        MvcResult result = mockMvc.perform(post("/read/chunked")
                .contentType("application/json")
                .characterEncoding(StandardCharsets.UTF_8.name())
                .content(MAPPER.writeValueAsBytes(DATA_REQUEST_MODEL)))
                .andExpect(request().asyncNotStarted())
                .andExpect(status().is5xxServerError())
                .andDo(print())
                .andReturn();

         */
    }
}
