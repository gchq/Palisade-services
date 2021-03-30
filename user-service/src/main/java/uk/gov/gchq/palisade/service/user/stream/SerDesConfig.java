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

package uk.gov.gchq.palisade.service.user.stream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.core.serializer.support.SerializationFailedException;

import uk.gov.gchq.palisade.service.user.model.AuditErrorMessage;
import uk.gov.gchq.palisade.service.user.model.UserRequest;
import uk.gov.gchq.palisade.service.user.model.UserResponse;

import java.io.IOException;
import java.nio.charset.Charset;

/**
 * The static configuration for kafka key/value serializers/deserializers
 * - Each input has a pair of key/value deserializers
 * - Each output has a pair of key/value serializers
 * In general, the keys are not used, so the choice of serializer is not important
 */
public final class SerDesConfig {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String SERIALIZATION_FAILED_MESSAGE = "Failed to serialize ";
    private static final String DESERIALIZATION_FAILED_MESSAGE = "Failed to deserialize ";

    private SerDesConfig() {
        // Static collection of objects, class should never be instantiated
    }

    /**
     * Kafka key serialiser for upstream messages coming in as input
     * Used by the Rest Controller to insert requests onto the topic
     *
     * @return an appropriate key serializer for the topic's message content
     */
    public static Serializer<String> requestKeySerializer() {
        return new StringSerializer();
    }

    /**
     * Kafka value serialiser for upstream messages coming in as input
     * Used by the Rest Controller to insert requests onto the topic
     *
     * @return an appropriate value serializer for the topic's message content
     */
    public static Serializer<UserRequest> requestValueSerializer() {
        return (String ignored, UserRequest userRequest) -> {
            try {
                return MAPPER.writeValueAsBytes(userRequest);
            } catch (IOException e) {
                throw new SerializationFailedException(SERIALIZATION_FAILED_MESSAGE + userRequest.toString(), e);
            }
        };
    }

    /**
     * Kafka key deserializer for upstream messages coming in as input
     *
     * @return an appropriate key deserializer for the topic's message content
     */
    public static Deserializer<String> requestKeyDeserializer() {
        return new StringDeserializer();
    }

    /**
     * Kafka value deserializer for upstream messages coming in as input
     *
     * @return an appropriate value deserializer for the topic's message content
     */
    public static Deserializer<UserRequest> requestValueDeserializer() {
        return (String ignored, byte[] userRequest) -> {
            try {
                return MAPPER.readValue(userRequest, UserRequest.class);
            } catch (IOException e) {
                throw new SerializationFailedException(DESERIALIZATION_FAILED_MESSAGE + new String(userRequest, Charset.defaultCharset()), e);
            }
        };
    }

    /**
     * Kafka value deserializer for the response from the service
     *
     * @param userResponse the JsonNode to be deserialized
     * @return an appropriate value deserializer for the topic's message content
     * @throws JsonProcessingException if there was an issue deseralizing
     */
    public static UserResponse responseValueDeserializer(final JsonNode userResponse) throws JsonProcessingException {
        return MAPPER.treeToValue(userResponse, UserResponse.class);
    }

    /**
     * Kafka key serialiser for downstream messages going out as output
     *
     * @return an appropriate key serializer for the topic's message content
     */
    public static Serializer<String> userKeySerializer() {
        return new StringSerializer();
    }

    /**
     * Kafka value serialiser for downstream messages going out as output
     *
     * @return an appropriate value serializer for the topic's message content
     */
    public static Serializer<UserResponse> userValueSerializer() {
        return (String ignored, UserResponse userResponse) -> {
            try {
                return MAPPER.writeValueAsBytes(userResponse);
            } catch (JsonProcessingException e) {
                throw new SerializationFailedException(SERIALIZATION_FAILED_MESSAGE + userResponse.toString(), e);
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
