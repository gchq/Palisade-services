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
package uk.gov.gchq.palisade.service.palisade.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.gov.gchq.palisade.rule.Rules;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import java.io.IOException;
import java.util.Optional;

@Converter
public class RulesConverter implements AttributeConverter<Rules<?>, String> {
    private static final Logger LOGGER = LoggerFactory.getLogger(RulesConverter.class);

    private final ObjectMapper objectMapper;

    public RulesConverter(final ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String convertToDatabaseColumn(final Rules<?> rules) {
        try {
            return this.objectMapper.writeValueAsString(rules);
        } catch (JsonProcessingException e) {
            LOGGER.error("Could not convert rules to json string.", e);
            return null;
        }
    }

    @Override
    public Rules<?> convertToEntityAttribute(final String attribute) {
        if (Optional.ofNullable(attribute).isPresent()) {
            try {
                return this.objectMapper.readValue(attribute, Rules.class);
            } catch (IOException e) {
                LOGGER.error("Conversion error whilst trying to convert string(JSON) to user.", e);
            }
        }
        return new Rules<>();
    }
}
