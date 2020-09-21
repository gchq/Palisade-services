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
import uk.gov.gchq.palisade.service.attributemask.message.AuditErrorMessage;

import java.io.IOException;

/**
 * Static configuration for kafka key/value serialisers/deserialisers
 * - Each input has a pair of key/value deserialisers
 * - Each output has a pair of key/value serialisers
 * In general, the keys are not used so the choice of serialiser is not important
 */
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


    public static Serializer<String> errorKeySerializer() {
        return new StringSerializer();
    }

    public static Serializer<AuditErrorMessage> errorValueSerializer() {
        return (ignored, auditErrorMessage) -> {
            try {
                return MAPPER.writeValueAsBytes(auditErrorMessage);
            } catch (JsonProcessingException e) {
                throw new SerializationFailedException("Failed to serialize " + auditErrorMessage.toString(), e);
            }
        };
    }
}
