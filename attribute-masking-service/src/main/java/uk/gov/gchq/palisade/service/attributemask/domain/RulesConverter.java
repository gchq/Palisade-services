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
package uk.gov.gchq.palisade.service.attributemask.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.serializer.support.SerializationFailedException;

import uk.gov.gchq.palisade.rule.Rules;

import javax.persistence.AttributeConverter;

import java.util.Optional;

/**
 * Convert between Java {@link Rules} objects and serialised {@link String}s stored in a database.
 * Simply wraps an {@link ObjectMapper}, elevating any {@link JsonProcessingException}s to {@link RuntimeException}s.
 */
public class RulesConverter implements AttributeConverter<Rules<?>, String> {
    private final ObjectMapper objectMapper;

    /**
     * Default constructor specifying the object mapper for (de)serializing objects.
     *
     * @param objectMapper the object mapper for reading and writing columns and objects
     */
    public RulesConverter(final ObjectMapper objectMapper) {
        this.objectMapper = Optional.ofNullable(objectMapper)
                .orElseThrow(() -> new IllegalArgumentException("objectMapper cannot be null"));
    }

    @Override
    public String convertToDatabaseColumn(final Rules<?> rules) {
        try {
            return this.objectMapper.writeValueAsString(rules);
        } catch (JsonProcessingException e) {
            throw new SerializationFailedException("Could not convert rules to json string", e);
        }
    }

    @Override
    public Rules<?> convertToEntityAttribute(final String attribute) {
        try {
            return this.objectMapper.readValue(attribute, Rules.class);
        } catch (JsonProcessingException e) {
            throw new SerializationFailedException("Could not convert json string to rules", e);
        }
    }
}
