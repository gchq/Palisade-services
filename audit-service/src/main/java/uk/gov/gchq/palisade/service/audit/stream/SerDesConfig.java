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
import uk.gov.gchq.palisade.service.audit.web.SerDesHealthIndicator;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Static configuration for kafka key/value serialisers/deserialisers
 * - Each input has a pair of key/value deserialisers
 * - Each output has a pair of key/value serialisers
 * In general, the keys are not used, so the choice of serialiser is not important
 */
public final class SerDesConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(SerDesConfig.class);
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
    public static Serializer<String> errorKeySerialiser() {
        return new StringSerializer();
    }

    /**
     * Kafka value serialiser for upstream messages coming in as input
     * Used by the Rest Controller to insert requests onto the topic
     *
     * @return an appropriate value serialiser for the topic's message content (AuditErrorMessage)
     */
    public static Serializer<AuditErrorMessage> errorValueSerialiser() {
        return (String ignored, AuditErrorMessage auditRequest) -> {
            try {
                return MAPPER.writeValueAsBytes(auditRequest);
            } catch (IOException e) {
                SerDesHealthIndicator.addSerDesExceptions(e);
                throw new SerializationFailedException(SERIALISATION_FAILED_MESSAGE + auditRequest.toString(), e);
            }
        };
    }

    /**
     * Kafka key deserialiser for upstream messages coming in as input
     *
     * @return an appropriate key deserialiser for the topic's message content
     */
    public static Deserializer<String> errorKeyDeserialiser() {
        return new StringDeserializer();
    }

    /**
     * Kafka value deserialiser for upstream messages coming in as input
     *
     * @param configProperties contains the directory for error files
     * @return an appropriate value deserialiser for the topic's message content (AuditErrorMessage)
     */
    public static Deserializer<AuditErrorMessage> errorValueDeserialiser(final AuditServiceConfigProperties configProperties) {
        return (String ignored, byte[] auditRequest) -> {
            try {
                return MAPPER.readValue(auditRequest, AuditErrorMessage.class);
            } catch (IOException e) {
                String failedAuditString = new String(auditRequest, Charset.defaultCharset());
                createFile("Error-", failedAuditString, configProperties);
                SerDesHealthIndicator.addSerDesExceptions(e);
                throw new SerializationFailedException(DESERIALISATION_FAILED_MESSAGE + failedAuditString, e);
            }
        };
    }

    /**
     * Kafka key serialiser for upstream messages coming in as input
     * Used by the Rest Controller to insert requests onto the topic
     *
     * @return an appropriate key serialiser for the topic's message content
     */
    public static Serializer<String> successKeySerialiser() {
        return new StringSerializer();
    }

    /**
     * Kafka value serialiser for upstream messages coming in as input
     * Used by the Rest Controller to insert requests onto the topic
     *
     * @return an appropriate value serialiser for the topic's message content (AuditSuccessMessage)
     */
    public static Serializer<AuditSuccessMessage> successValueSerialiser() {
        return (String ignored, AuditSuccessMessage auditRequest) -> {
            try {
                return MAPPER.writeValueAsBytes(auditRequest);
            } catch (IOException e) {
                SerDesHealthIndicator.addSerDesExceptions(e);
                throw new SerializationFailedException(SERIALISATION_FAILED_MESSAGE + auditRequest.toString(), e);
            }
        };
    }

    /**
     * Kafka key deserialiser for upstream messages coming in as input
     *
     * @return an appropriate key deserialiser for the topic's message content
     */
    public static Deserializer<String> successKeyDeserialiser() {
        return new StringDeserializer();
    }

    /**
     * Kafka value deserialiser for upstream messages coming in as input
     *
     * @param configProperties contains the directory for error files
     * @return an appropriate value deserialiser for the topic's message content (AuditSuccessMessage)
     */
    public static Deserializer<AuditSuccessMessage> successValueDeserialiser(final AuditServiceConfigProperties configProperties) {
        return (String ignored, byte[] auditRequest) -> {
            try {
                return MAPPER.readValue(auditRequest, AuditSuccessMessage.class);
            } catch (IOException e) {
                String failedAuditString = new String(auditRequest, Charset.defaultCharset());
                createFile("Success-", failedAuditString, configProperties);
                SerDesHealthIndicator.addSerDesExceptions(e);
                throw new SerializationFailedException(DESERIALISATION_FAILED_MESSAGE + failedAuditString, e);
            }
        };
    }

    private static void createFile(final String prefix, final String failedAuditString, final AuditServiceConfigProperties configProperties) {
        // Create a fileName using the prefix value and a timestamp.
        // A replacement needs to be done on the timestamp value to allow saving a file on Windows machines
        String fileName = prefix + ZonedDateTime.now(ZoneOffset.UTC)
                .format(DateTimeFormatter.ISO_INSTANT).replace(":", "-");
        File directory = new File(configProperties.getErrorDirectory());
        File parent = directory.getAbsoluteFile().getParentFile();
        File timestampedFile = new File(parent, fileName);
        try (FileWriter fileWriter = new FileWriter(timestampedFile, StandardCharsets.UTF_8, !timestampedFile.createNewFile())) {
            fileWriter.write(failedAuditString);
            LOGGER.warn("Failed to deserialise the '{}' audit message. Created file {}", prefix, timestampedFile);
        } catch (IOException ex) {
            LOGGER.error("Failed to write file to directory: {}", directory.getAbsoluteFile());
            LOGGER.error("Failed to process audit request '{}'", failedAuditString, ex);
        }
    }
}
