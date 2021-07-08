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
package uk.gov.gchq.palisade.service.palisade.stream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.core.serializer.support.SerializationFailedException;

import uk.gov.gchq.palisade.service.palisade.model.AuditErrorMessage;
import uk.gov.gchq.palisade.service.palisade.model.PalisadeSystemResponse;

/**
 * Static configuration for kafka key/value serialisers/deserialisers
 * - Each input has a pair of key/value deserialisers
 * - Each output has a pair of key/value serialisers
 * In general, the keys are not used, so the choice of serialiser is not important
 */
public final class SerDesConfig {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String SERIALISATION_FAILED_MESSAGE = "Failed to serialise ";

    private SerDesConfig() {
        // Static collection of objects, class should never be instantiated
    }

    /**
     * Kafka key serialiser for downstream messages going out as output
     *
     * @return an appropriate key serialiser for the topic's message content
     */
    public static Serializer<String> requestKeySerialiser() {
        return new StringSerializer();
    }

    /**
     * Kafka value serialiser for downstream messages going out as output
     *
     * @return an appropriate value serialiser for the topic's message content
     */
    public static Serializer<PalisadeSystemResponse> requestSerialiser() {
        return (String ignored, PalisadeSystemResponse request) -> {
            try {
                return MAPPER.writeValueAsBytes(request);
            } catch (JsonProcessingException e) {
                throw new SerializationFailedException(SERIALISATION_FAILED_MESSAGE + request.toString(), e);
            }
        };
    }

    /**
     * Kafka value serialiser for downstream messages going out as output
     *
     * @return an appropriate value serialiser for the topic's message content
     */
    public static Serializer<AuditErrorMessage> errorValueSerialiser() {
        return (String ignored, AuditErrorMessage errorRequest) -> {
            try {
                return MAPPER.writeValueAsBytes(errorRequest);
            } catch (JsonProcessingException e) {
                throw new SerializationFailedException(SERIALISATION_FAILED_MESSAGE + errorRequest.toString(), e);
            }
        };
    }

    /**
     * Kafka value serialiser for downstream messages going out as output
     *
     * @return an appropriate value serialiser for the topic's message content
     */
    public static Serializer<byte[]> passthroughValueSerialiser() {
        return (String ignored, byte[] bytes) -> bytes;
    }
}
