package uk.gov.gchq.palisade.contract.data.kafka;

import akka.actor.ActorSystem;
import akka.kafka.ConsumerSettings;
import akka.kafka.Subscriptions;
import akka.kafka.javadsl.Consumer;
import akka.stream.Materializer;
import akka.stream.testkit.TestSubscriber;
import akka.stream.testkit.javadsl.TestSink;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import uk.gov.gchq.palisade.contract.data.common.TestSerDesConfig;
import uk.gov.gchq.palisade.service.data.DataApplication;
import uk.gov.gchq.palisade.service.data.model.AuditMessage;
import uk.gov.gchq.palisade.service.data.service.AuditMessageService;
import uk.gov.gchq.palisade.service.data.service.AuditableDataService;
import uk.gov.gchq.palisade.service.data.stream.ProducerTopicConfiguration;
import uk.gov.gchq.palisade.service.data.web.DataController;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.gchq.palisade.component.data.common.CommonTestData.AUDITABLE_DATA_READER_REQUEST_WITH_ERROR;
import static uk.gov.gchq.palisade.component.data.common.CommonTestData.DATA_REQUEST_MODEL;


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
    void testRestEndpointError() throws Exception {

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
