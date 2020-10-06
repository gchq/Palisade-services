package uk.gov.gchq.palisade.contract.topicoffset.kafka.old;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.User;
import uk.gov.gchq.palisade.UserId;
import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.resource.impl.FileResource;
import uk.gov.gchq.palisade.resource.impl.SystemResource;
import uk.gov.gchq.palisade.service.SimpleConnectionDetail;
import uk.gov.gchq.palisade.service.topicoffset.model.StreamMarker;
import uk.gov.gchq.palisade.service.topicoffset.model.Token;
import uk.gov.gchq.palisade.service.topicoffset.model.TopicOffsetRequest;
import uk.gov.gchq.palisade.service.topicoffset.model.TopicOffsetResponse;

import java.time.Duration;
import java.util.LinkedList;
import java.util.concurrent.CompletableFuture;

public class ContractTestConfiguration {
    public static final String REQUEST_TOKEN = "test-request-token";
    public static final UserId USER_ID = new UserId().id("test-user-id");
    public static final User USER = new User().userId(USER_ID);
    public static final String RESOURCE_ID = "/test/resourceId";
    public static final String RESOURCE_TYPE = "uk.gov.gchq.palisade.test.TestType";
    public static final String RESOURCE_FORMAT = "avro";
    public static final String DATA_SERVICE_NAME = "test-data-service";
    public static final String RESOURCE_PARENT = "/test";
    public static final LeafResource LEAF_RESOURCE = new FileResource()
            .id(RESOURCE_ID)
            .type(RESOURCE_TYPE)
            .serialisedFormat(RESOURCE_FORMAT)
            .connectionDetail(new SimpleConnectionDetail().serviceName(DATA_SERVICE_NAME))
            .parent(new SystemResource().id(RESOURCE_PARENT));

    public static final String PURPOSE = "test-purpose";
    public static final Context CONTEXT = new Context().purpose(PURPOSE);
    public static final String RULE_MESSAGE = "test-rule";

    public static final TopicOffsetRequest REQUEST = TopicOffsetRequest.Builder.create()
            .withUserId(USER_ID.getId())
            .withResourceId(RESOURCE_ID)
            .withContext(CONTEXT)
            .withResource(LEAF_RESOURCE);

    public static final ProducerRecord<String, TopicOffsetRequest> START = new ProducerRecord<>("masked-resource", 0, null, null);
    public static final ProducerRecord<String, TopicOffsetRequest> RECORD = new ProducerRecord<>("masked-resource", 0, null, REQUEST);
    public static final ProducerRecord<String, TopicOffsetRequest> END = new ProducerRecord<>("masked-resource", 0, null, null);

    static {
        START.headers().add(StreamMarker.HEADER, StreamMarker.START.toString().getBytes());
        START.headers().add(Token.HEADER, REQUEST_TOKEN.getBytes());

        RECORD.headers().add(Token.HEADER, REQUEST_TOKEN.getBytes());

        END.headers().add(StreamMarker.HEADER, StreamMarker.END.toString().getBytes());
        END.headers().add(Token.HEADER, REQUEST_TOKEN.getBytes());
    }

    public static class TopicOffsetRequestSerialiser extends JsonSerializer<TopicOffsetRequest> {
    }

    public static class TopicOffsetResponseDeserialiser extends JsonDeserializer<TopicOffsetResponse> {
    }

    public static <R> CompletableFuture<LinkedList<ConsumerRecord<String, R>>> consumeWithTimeout(final KafkaConsumer<String, R> consumer, final Duration timeout) {
        return CompletableFuture.supplyAsync(() -> {
                    LinkedList<ConsumerRecord<String, R>> collected = new LinkedList<>();
                    consumer.poll(timeout).forEach(collected::add);
                    return collected;
                }
        );
    }
}
