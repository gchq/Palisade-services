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
package uk.gov.gchq.palisade.contract.palisade.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.core.serializer.support.SerializationFailedException;

import uk.gov.gchq.palisade.service.palisade.model.AuditErrorMessage;
import uk.gov.gchq.palisade.service.palisade.model.PalisadeRequest;

import java.io.IOException;
import java.nio.charset.Charset;

/**
 * Static configuration for kafka key/value serialisers/deserialisers
 * - Each input has a pair of key/value deserialisers
 * - Each output has a pair of key/value serialisers
 * In general, the keys are not used, so the choice of serialiser is not important
 */
public final class TestSerDesConfig {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String DESERIALIZATION_FAILED_MESSAGE = "Failed to deserialize ";

    private TestSerDesConfig() {
        // Static collection of objects, class should never be instantiated
    }

    /**
     * Kafka key deserialiser for upstream messages coming in as input
     *
     * @return an appropriate key deserialiser for the topic's message content
     */
    public static Deserializer<String> requestKeyDeserializer() {
        return new StringDeserializer();
    }

    /**
     * Kafka value deserialiser for upstream messages coming in as input
     *
     * @return an appropriate value deserialiser for the topic's message content
     */
    public static Deserializer<PalisadeRequest> requestValueDeserializer() {
        return (String ignored, byte[] palisadeRequest) -> {
            try {
                return MAPPER.readValue(palisadeRequest, PalisadeRequest.class);
            } catch (IOException e) {
                throw new SerializationFailedException(DESERIALIZATION_FAILED_MESSAGE + new String(palisadeRequest, Charset.defaultCharset()), e);
            }
        };
    }

    /**
     * Kafka key deserialiser for upstream messages coming in as input
     *
     * @return an appropriate key deserialiser for the topic's message content
     */
    public static Deserializer<String> errorKeyDeserializer() {
        return new StringDeserializer();
    }

    /**
     * Kafka value deserialiser for upstream messages coming in as input
     *
     * @return an appropriate value deserialiser for the topic's message content
     */
    public static Deserializer<AuditErrorMessage> errorValueDeserializer() {
        return (String ignored, byte[] auditErrorMessage) -> {
            try {
                return MAPPER.readValue(auditErrorMessage, AuditErrorMessage.class);
            } catch (IOException e) {
                throw new SerializationFailedException(DESERIALIZATION_FAILED_MESSAGE + new String(auditErrorMessage, Charset.defaultCharset()), e);
            }
        };
    }
}
