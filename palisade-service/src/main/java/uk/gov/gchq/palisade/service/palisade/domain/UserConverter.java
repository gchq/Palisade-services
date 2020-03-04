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

import uk.gov.gchq.palisade.User;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import java.io.IOException;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

@Converter
public class UserConverter implements AttributeConverter<User, String> {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserConverter.class);

    private final ObjectMapper objectMapper;

    public UserConverter(final ObjectMapper objectMapper) {
        this.objectMapper = requireNonNull(objectMapper, "objectMapper");
    }

    @Override
    public String convertToDatabaseColumn(final User user) {
        if (Optional.ofNullable(user).isEmpty()) {
            return null;
        }
        try {
            return this.objectMapper.writeValueAsString(user);
        } catch (JsonProcessingException e) {
            LOGGER.error("Could not convert user to json string.", e);
            return null;
        }
    }

    @Override
    public User convertToEntityAttribute(final String attribute) {
        if (Optional.ofNullable(attribute).isPresent()) {
            try {
                return this.objectMapper.readValue(attribute, User.class);
            } catch (IOException e) {
                LOGGER.error("Conversion error whilst trying to convert string(JSON) to user.", e);
            }
        }
        return new User();
    }
}
