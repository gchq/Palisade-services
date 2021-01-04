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
package uk.gov.gchq.palisade.service.palisade.stream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.core.serializer.support.SerializationFailedException;

import uk.gov.gchq.palisade.service.palisade.model.AuditErrorMessage;
import uk.gov.gchq.palisade.service.palisade.model.PalisadeRequest;

/**
 * Static configuration for kafka key/value serialisers/deserialisers
 * - Each input has a pair of key/value deserialisers
 * - Each output has a pair of key/value serialisers
 * In general, the keys are not used, so the choice of serialiser is not important
 */
public final class SerDesConfig {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String SERIALIZATION_FAILED_MESSAGE = "Failed to serialize ";

    private SerDesConfig() {
        // Static collection of objects, class should never be instantiated
    }

    /**
     * Kafka key serialiser for downstream messages going out as output
     *
     * @return an appropriate key serialiser for the topic's message content
     */
    public static Serializer<String> requestKeySerializer() {
        return new StringSerializer();
    }

    /**
     * Kafka value serialiser for downstream messages going out as output
     *
     * @return an appropriate value serialiser for the topic's message content
     */
    public static Serializer<PalisadeRequest> requestSerializer() {
        return (String ignored, PalisadeRequest palisadeRequest) -> {
            try {
                return MAPPER.writeValueAsBytes(palisadeRequest);
            } catch (JsonProcessingException e) {
                throw new SerializationFailedException(SERIALIZATION_FAILED_MESSAGE + palisadeRequest.toString(), e);
            }
        };
    }

    /**
     * Kafka key serialiser for downstream messages going out as output
     *
     * @return an appropriate key serialiser for the topic's message content
     */
    public static Serializer<String> errorKeySerializer() {
        return new StringSerializer();
    }

    /**
     * Kafka value serialiser for downstream messages going out as output
     *
     * @return an appropriate value serialiser for the topic's message content
     */
    public static Serializer<AuditErrorMessage> errorValueSerializer() {
        return (String ignored, AuditErrorMessage errorRequest) -> {
            try {
                return MAPPER.writeValueAsBytes(errorRequest);
            } catch (JsonProcessingException e) {
                throw new SerializationFailedException(SERIALIZATION_FAILED_MESSAGE + errorRequest.toString(), e);
            }
        };
    }

    /**
     * Kafka value serialiser for downstream messages going out as output
     *
     * @return an appropriate value serialiser for the topic's message content
     */
    public static Serializer<byte[]> passthroughValueSerializer() {
        return (String ignored, byte[] bytes) -> bytes;
    }
}
