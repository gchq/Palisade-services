package uk.gov.gchq.palisade.service.attributemask.stream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.core.serializer.support.SerializationFailedException;

import uk.gov.gchq.palisade.service.attributemask.message.AttributeMaskingRequest;
import uk.gov.gchq.palisade.service.attributemask.message.AttributeMaskingResponse;

import java.io.IOException;

public class SerDesConfig {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private SerDesConfig() {
        // Static collection of objects, class should never be instantiated
    }

    public static Deserializer<String> keyDeserializer() {
        return new StringDeserializer();
    }

    public static Deserializer<AttributeMaskingRequest> valueDeserializer() {
        return (ignored, attributeMaskingRequest) -> {
            try {
                return MAPPER.readValue(attributeMaskingRequest, AttributeMaskingRequest.class);
            } catch (IOException e) {
                throw new SerializationFailedException("Failed to deserialize " + new String(attributeMaskingRequest), e);
            }
        };
    }

    public static Serializer<String> keySerializer() {
        return new StringSerializer();
    }

    public static Serializer<AttributeMaskingResponse> valueSerializer() {
        return (ignored, attributeMaskingResponse) -> {
            try {
                return MAPPER.writeValueAsBytes(attributeMaskingResponse);
            } catch (JsonProcessingException e) {
                throw new SerializationFailedException("Failed to serialize " + attributeMaskingResponse.toString(), e);
            }
        };
    }
}
