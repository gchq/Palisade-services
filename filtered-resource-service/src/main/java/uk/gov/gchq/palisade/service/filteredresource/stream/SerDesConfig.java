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

package uk.gov.gchq.palisade.service.filteredresource.stream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.core.serializer.support.SerializationFailedException;

import uk.gov.gchq.palisade.service.filteredresource.config.ApplicationConfiguration;
import uk.gov.gchq.palisade.service.filteredresource.model.AuditErrorMessage;
import uk.gov.gchq.palisade.service.filteredresource.model.AuditSuccessMessage;
import uk.gov.gchq.palisade.service.filteredresource.model.FilteredResourceRequest;
import uk.gov.gchq.palisade.service.filteredresource.model.TopicOffsetMessage;

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
    public static Serializer<String> maskedResourceKeySerializer() {
        return new StringSerializer();
    }

    /**
     * Kafka value serialiser for upstream messages coming in as input
     * Used by the Rest Controller to insert requests onto the topic
     *
     * @return an appropriate value serialiser for the topic's message content (FilteredResourceRequest)
     */
    public static Serializer<FilteredResourceRequest> maskedResourceValueSerializer() {
        return (String ignored, FilteredResourceRequest filteredResourceRequest) -> {
            try {
                return MAPPER.writeValueAsBytes(filteredResourceRequest);
            } catch (IOException e) {
                throw new SerializationFailedException(SERIALIZATION_FAILED_MESSAGE + filteredResourceRequest.toString(), e);
            }
        };
    }

    /**
     * Kafka key deserialiser for upstream messages coming in as input
     *
     * @return an appropriate key deserialiser for the topic's message content
     */
    public static Deserializer<String> maskedResourceKeyDeserializer() {
        return new StringDeserializer();
    }

    /**
     * Kafka value deserialiser for upstream messages coming in as input
     *
     * @return an appropriate value deserialiser for the topic's message content (FilteredResourceRequest)
     */
    public static Deserializer<FilteredResourceRequest> maskedResourceValueDeserializer() {
        return (String ignored, byte[] filteredResourceRequest) -> {
            try {
                return MAPPER.readValue(filteredResourceRequest, FilteredResourceRequest.class);
            } catch (IOException e) {
                throw new SerializationFailedException(DESERIALIZATION_FAILED_MESSAGE + new String(filteredResourceRequest, Charset.defaultCharset()), e);
            }
        };
    }

    /**
     * Kafka key serialiser for downstream messages going out as success
     *
     * @return an appropriate key serialiser for the topic's message content
     */
    public static Serializer<String> maskedResourceOffsetKeySerializer() {
        return new StringSerializer();
    }

    /**
     * Kafka value serialiser for downstream messages going out as success
     *
     * @return an appropriate value serialiser for the topic's message content (AuditMessage)
     */
    public static Serializer<TopicOffsetMessage> maskedResourceOffsetValueSerializer() {
        return (String ignored, TopicOffsetMessage topicOffsetMessage) -> {
            try {
                return MAPPER.writeValueAsBytes(topicOffsetMessage);
            } catch (JsonProcessingException e) {
                throw new SerializationFailedException(SERIALIZATION_FAILED_MESSAGE + topicOffsetMessage.toString(), e);
            }
        };
    }

    /**
     * Kafka key deserialiser for upstream messages coming in as input
     *
     * @return an appropriate key deserialiser for the topic's message content
     */
    public static Deserializer<String> maskedResourceOffsetKeyDeserializer() {
        return new StringDeserializer();
    }

    /**
     * Kafka value deserialiser for upstream messages coming in as input
     *
     * @return an appropriate value deserialiser for the topic's message content (FilteredResourceRequest)
     */
    public static Deserializer<TopicOffsetMessage> maskedResourceOffsetValueDeserializer() {
        return (String ignored, byte[] topicOffsetMessage) -> {
            try {
                return MAPPER.readValue(topicOffsetMessage, TopicOffsetMessage.class);
            } catch (IOException e) {
                throw new SerializationFailedException(DESERIALIZATION_FAILED_MESSAGE + new String(topicOffsetMessage, Charset.defaultCharset()), e);
            }
        };
    }

    /**
     * Kafka key serialiser for downstream messages going out as success
     *
     * @return an appropriate key serialiser for the topic's message content
     */
    public static Serializer<String> successKeySerializer() {
        return new StringSerializer();
    }

    /**
     * Kafka value serialiser for downstream messages going out as success
     *
     * @return an appropriate value serialiser for the topic's message content (AuditMessage)
     */
    public static Serializer<AuditSuccessMessage> successValueSerializer() {
        return (String ignored, AuditSuccessMessage auditSuccessMessage) -> {
            try {
                return MAPPER.writeValueAsBytes(auditSuccessMessage);
            } catch (JsonProcessingException e) {
                throw new SerializationFailedException(SERIALIZATION_FAILED_MESSAGE + auditSuccessMessage.toString(), e);
            }
        };
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

    /**
     * Kafka key deserialiser for upstream messages coming in as input
     *
     * @return an appropriate key deserialiser for the topic's AuditErrorMessage content
     */
    public static Deserializer<String> errorKeyDeserializer() {
        return new StringDeserializer();
    }

    /**
     * Kafka value deserialiser for upstream AuditErrorMessages coming in as input
     *
     * @return an appropriate value deserialiser for the topic's message content (AuditErrorMessage)
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
