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
 * The static configuration for kafka key/value serialisers/deserialisers
 * - Each input has a pair of key/value deserialisers
 * - Each output has a pair of key/value serialisers
 */
public final class SerDesConfig {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String SERIALISATION_FAILED_MESSAGE = "Failed to serialise ";
    private static final String DESERIALISATION_FAILED_MESSAGE = "Failed to deserialise ";

    private SerDesConfig() {
        // Static collection of objects, class should never be instantiated
    }

    /**
     * Kafka key serialiser for upstream messages coming in as input
     * Used by the Rest Controller to insert requests onto the topic
     *
     * @return an appropriate key serialiser for the topic's message content
     */
    public static Serializer<String> requestKeySerialiser() {
        return new StringSerializer();
    }

    /**
     * Kafka value serialiser for upstream messages coming in as input
     * Used by the Rest Controller to insert requests onto the topic
     *
     * @return an appropriate value serialiser for the topic's message content
     */
    public static Serializer<UserRequest> requestValueSerialiser() {
        return (String ignored, UserRequest userRequest) -> {
            try {
                return MAPPER.writeValueAsBytes(userRequest);
            } catch (IOException e) {
                throw new SerializationFailedException(SERIALISATION_FAILED_MESSAGE + userRequest.toString(), e);
            }
        };
    }

    /**
     * Kafka key deserialiser for upstream messages coming in as input
     *
     * @return an appropriate key deserialiser for the topic's message content
     */
    public static Deserializer<String> requestKeyDeserialiser() {
        return new StringDeserializer();
    }

    /**
     * Kafka value deserialiser for upstream messages coming in as input
     *
     * @return an appropriate value deserialiser for the topic's message content
     */
    public static Deserializer<UserRequest> requestValueDeserialiser() {
        return (String ignored, byte[] userRequest) -> {
            try {
                return MAPPER.readValue(userRequest, UserRequest.class);
            } catch (IOException e) {
                throw new SerializationFailedException(DESERIALISATION_FAILED_MESSAGE + new String(userRequest, Charset.defaultCharset()), e);
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
     * @return an appropriate key serialiser for the topic's message content
     */
    public static Serializer<String> userKeySerialiser() {
        return new StringSerializer();
    }

    /**
     * Kafka value serialiser for downstream messages going out as output
     *
     * @return an appropriate value serialiser for the topic's message content
     */
    public static Serializer<UserResponse> userValueSerialiser() {
        return (String ignored, UserResponse userResponse) -> {
            try {
                return MAPPER.writeValueAsBytes(userResponse);
            } catch (JsonProcessingException e) {
                throw new SerializationFailedException(SERIALISATION_FAILED_MESSAGE + userResponse.toString(), e);
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

    /**
     * Kafka key serialiser for downstream messages going out as errors
     *
     * @return an appropriate key serialiser for the topic's message content
     */
    public static Serializer<String> errorKeySerialiser() {
        return new StringSerializer();
    }

    /**
     * Kafka value serialiser for downstream messages going out as errors
     *
     * @return an appropriate value serialiser for the topic's message content (AuditMessage)
     */
    public static Serializer<AuditErrorMessage> errorValueSerialiser() {
        return (String ignored, AuditErrorMessage auditErrorMessage) -> {
            try {
                return MAPPER.writeValueAsBytes(auditErrorMessage);
            } catch (JsonProcessingException e) {
                throw new SerializationFailedException(SERIALISATION_FAILED_MESSAGE + auditErrorMessage.toString(), e);
            }
        };
    }
}
