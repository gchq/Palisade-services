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

package uk.gov.gchq.palisade.service.policy.stream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.core.serializer.support.SerializationFailedException;

import uk.gov.gchq.palisade.service.policy.config.ApplicationConfiguration;
import uk.gov.gchq.palisade.service.policy.model.AuditErrorMessage;
import uk.gov.gchq.palisade.service.policy.model.PolicyRequest;
import uk.gov.gchq.palisade.service.policy.model.PolicyResponse;

import java.io.IOException;
import java.nio.charset.Charset;

/**
 * Static configuration for kafka key/value serialisers/deserialisers
 * - Each input has a pair of key/value deserialisers
 * - Each output has a pair of key/value serialisers
 * In general, the keys are not used so the choice of serialiser is not important
 */
public final class SerDesConfig {
    private static final ObjectMapper MAPPER = new ApplicationConfiguration().objectMapper();
    private static final String SERIALIZATION_FAILED_MESSAGE = "Failed to serialize ";
    private static final String DESERIALIZATION_FAILED_MESSAGE = "Failed to deserialize ";

    private SerDesConfig() {
        // Static collection of objects, class should never be instantiated
    }

    /**
     * Kafka key serialiser for upstream messages coming in as input
     * Used by the Rest Controller to insert requests onto the topic
     *
     * @return an appropriate key serialiser for the topic's message content
     */
    public static Serializer<String> resourceKeySerializer() {
        return new StringSerializer();
    }

    /**
     * Kafka value serialiser for upstream messages coming in as input
     * Used by the Rest Controller to insert requests onto the topic
     *
     * @return an appropriate value serialiser for the topic's message content (PolicyRequest)
     */
    public static Serializer<PolicyRequest> resourceValueSerializer() {
        return (String ignored, PolicyRequest policyRequest) -> {
            try {
                return MAPPER.writeValueAsBytes(policyRequest);
            } catch (IOException e) {
                throw new SerializationFailedException(SERIALIZATION_FAILED_MESSAGE + policyRequest.toString(), e);
            }
        };
    }

    /**
     * Kafka key deserialiser for upstream messages coming in as input
     *
     * @return an appropriate key deserialiser for the topic's message content
     */
    public static Deserializer<String> resourceKeyDeserializer() {
        return new StringDeserializer();
    }

    /**
     * Kafka value deserialiser for upstream messages coming in as input
     *
     * @return an appropriate value deserialiser for the topic's message content (PolicyRequest)
     */
    public static Deserializer<PolicyRequest> resourceValueDeserializer() {
        return (String ignored, byte[] policyRequest) -> {
            try {
                return MAPPER.readValue(policyRequest, PolicyRequest.class);
            } catch (IOException e) {
                throw new SerializationFailedException(DESERIALIZATION_FAILED_MESSAGE + new String(policyRequest, Charset.defaultCharset()), e);
            }
        };
    }

    /**
     * Kafka key serialiser for downstream messages going out as output
     *
     * @return an appropriate key serialiser for the topic's message content
     */
    public static Serializer<String> ruleKeySerializer() {
        return new StringSerializer();
    }

    /**
     * Kafka value serialiser for downstream messages going out as output
     *
     * @return an appropriate value serialiser for the topic's message content (PolicyResponse)
     */
    public static Serializer<PolicyResponse> ruleValueSerializer() {
        return (String ignored, PolicyResponse policyResponse) -> {
            try {
                return MAPPER.writeValueAsBytes(policyResponse);
            } catch (JsonProcessingException e) {
                throw new SerializationFailedException(SERIALIZATION_FAILED_MESSAGE + policyResponse.toString(), e);
            }
        };
    }

    /**
     * Kafka value serialiser for downstream messages going out as output
     *
     * @return an appropriate value serialiser for the topic's message content (PolicyResponse)
     */
    public static Serializer<byte[]> passthroughValueSerializer() {
        return (String ignored, byte[] bytes) -> bytes;
    }

    /**
     * Kafka key serialiser for downstream messages going out as errors
     *
     * @return an appropriate key serialiser for the topic's message content
     */
    public static Serializer<String> errorKeySerializer() {
        return new StringSerializer();
    }

    /**
     * Kafka value serialiser for downstream messages going out as errors
     *
     * @return an appropriate value serialiser for the topic's message content (AuditMessage)
     */
    public static Serializer<AuditErrorMessage> errorValueSerializer() {
        return (String ignored, AuditErrorMessage auditErrorMessage) -> {
            try {
                return MAPPER.writeValueAsBytes(auditErrorMessage);
            } catch (JsonProcessingException e) {
                throw new SerializationFailedException(SERIALIZATION_FAILED_MESSAGE + auditErrorMessage.toString(), e);
            }
        };
    }
}
