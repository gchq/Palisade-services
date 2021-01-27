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

package uk.gov.gchq.palisade.service.audit.stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.serializer.support.SerializationFailedException;

import uk.gov.gchq.palisade.service.audit.config.AuditServiceConfigProperties;
import uk.gov.gchq.palisade.service.audit.model.AuditErrorMessage;
import uk.gov.gchq.palisade.service.audit.model.AuditSuccessMessage;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Static configuration for kafka key/value serialisers/deserialisers
 * - Each input has a pair of key/value deserialisers
 * - Each output has a pair of key/value serialisers
 * In general, the keys are not used so the choice of serialiser is not important
 */
public final class SerDesConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(SerDesConfig.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String SERIALIZATION_FAILED_MESSAGE = "Failed to serialize ";
    private static final String DESERIALIZATION_FAILED_MESSAGE = "Failed to deserialize ";
    private static final Queue<Exception> SERDES_EXCEPTIONS = new ConcurrentLinkedQueue<>();

    private SerDesConfig() {
        // Static collection of objects, class should never be instantiated
    }

    public static Queue<Exception> getSerDesExceptions() {
        return SERDES_EXCEPTIONS;
    }

    /**
     * Kafka key serialiser for upstream messages coming in as input
     * Used by the Rest Controller to insert requests onto the topic
     *
     * @return an appropriate key serialiser for the topic's message content
     */
    public static Serializer<String> errorKeySerializer() {
        return new StringSerializer();
    }

    /**
     * Kafka value serialiser for upstream messages coming in as input
     * Used by the Rest Controller to insert requests onto the topic
     *
     * @return an appropriate value serialiser for the topic's message content (AuditErrorMessage)
     */
    public static Serializer<AuditErrorMessage> errorValueSerializer() {
        return (String ignored, AuditErrorMessage auditRequest) -> {
            try {
                return MAPPER.writeValueAsBytes(auditRequest);
            } catch (IOException e) {
                SERDES_EXCEPTIONS.add(e);
                throw new SerializationFailedException(SERIALIZATION_FAILED_MESSAGE + auditRequest.toString(), e);
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
     * @param configProperties contains the directory for error files
     * @return an appropriate value deserialiser for the topic's message content (AuditErrorMessage)
     */
    public static Deserializer<AuditErrorMessage> errorValueDeserializer(final AuditServiceConfigProperties configProperties) {
        return (String ignored, byte[] auditRequest) -> {
            try {
                return MAPPER.readValue(auditRequest, AuditErrorMessage.class);
            } catch (IOException e) {
                String failedAuditString = new String(auditRequest, Charset.defaultCharset());
                try {
                    String fileName = "Error-" + ZonedDateTime.now(ZoneOffset.UTC)
                            .format(DateTimeFormatter.ISO_INSTANT);
                    File directory = new File(configProperties.getErrorDirectory());
                    File parent = directory.getAbsoluteFile().getParentFile();
                    File timestampedFile = new File(parent, fileName);
                    FileWriter fileWriter = new FileWriter(timestampedFile, !timestampedFile.createNewFile());
                    fileWriter.write(failedAuditString);
                    fileWriter.close();
                    LOGGER.info("Successfully created error file {}", timestampedFile);
                } catch (IOException ioException) {
                    LOGGER.error("Failed to process audit request '{}'", failedAuditString, ioException);
                }
                SERDES_EXCEPTIONS.add(e);
                throw new SerializationFailedException(DESERIALIZATION_FAILED_MESSAGE + failedAuditString, e);
            }
        };
    }

    /**
     * Kafka key serialiser for upstream messages coming in as input
     * Used by the Rest Controller to insert requests onto the topic
     *
     * @return an appropriate key serialiser for the topic's message content
     */
    public static Serializer<String> successKeySerializer() {
        return new StringSerializer();
    }

    /**
     * Kafka value serialiser for upstream messages coming in as input
     * Used by the Rest Controller to insert requests onto the topic
     *
     * @return an appropriate value serialiser for the topic's message content (AuditSuccessMessage)
     */
    public static Serializer<AuditSuccessMessage> successValueSerializer() {
        return (String ignored, AuditSuccessMessage auditRequest) -> {
            try {
                return MAPPER.writeValueAsBytes(auditRequest);
            } catch (IOException e) {
                SERDES_EXCEPTIONS.add(e);
                throw new SerializationFailedException(SERIALIZATION_FAILED_MESSAGE + auditRequest.toString(), e);
            }
        };
    }

    /**
     * Kafka key deserialiser for upstream messages coming in as input
     *
     * @return an appropriate key deserialiser for the topic's message content
     */
    public static Deserializer<String> successKeyDeserializer() {
        return new StringDeserializer();
    }

    /**
     * Kafka value deserialiser for upstream messages coming in as input
     *
     * @param configProperties contains the directory for error files
     * @return an appropriate value deserialiser for the topic's message content (AuditSuccessMessage)
     */
    public static Deserializer<AuditSuccessMessage> successValueDeserializer(final AuditServiceConfigProperties configProperties) {
        return (String ignored, byte[] auditRequest) -> {
            try {
                return MAPPER.readValue(auditRequest, AuditSuccessMessage.class);
            } catch (IOException e) {
                String failedAuditString = new String(auditRequest, Charset.defaultCharset());
                try {
                    File directory = new File(configProperties.getErrorDirectory());
                    File parent = directory.getAbsoluteFile().getParentFile();
                    File timestampedFile = new File(parent, "Success-" + ZonedDateTime.now(ZoneOffset.UTC)
                            .format(DateTimeFormatter.ISO_INSTANT));
                    FileWriter fileWriter = new FileWriter(timestampedFile, !timestampedFile.createNewFile());
                    fileWriter.write(failedAuditString);
                    fileWriter.close();
                    LOGGER.info("Successfully created error file {}", timestampedFile);
                } catch (IOException ex) {
                    LOGGER.error("Failed to process audit request '{}'", failedAuditString, ex);
                }
                SERDES_EXCEPTIONS.add(e);
                throw new SerializationFailedException(DESERIALIZATION_FAILED_MESSAGE + failedAuditString, e);
            }
        };
    }
}
