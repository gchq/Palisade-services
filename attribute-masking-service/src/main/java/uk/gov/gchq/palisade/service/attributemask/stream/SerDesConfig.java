package uk.gov.gchq.palisade.service.attributemask.stream;

import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import uk.gov.gchq.palisade.service.attributemask.message.AttributeMaskingRequest;
import uk.gov.gchq.palisade.service.attributemask.message.AttributeMaskingResponse;

public class SerDesConfig {
    public static Deserializer<String> keyDeserializer() {
        return new StringDeserializer();
    }

    public static Deserializer<AttributeMaskingRequest> valueDeserializer() {
        return new JsonDeserializer<>();
    }

    public static Serializer<String> keySerializer() {
        return new StringSerializer();
    }

    public static Serializer<AttributeMaskingResponse> valueSerializer() {
        return new JsonSerializer<>();
    }
}
