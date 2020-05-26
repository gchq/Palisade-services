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
package uk.gov.gchq.palisade.service.resource.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.gov.gchq.palisade.resource.ChildResource;
import uk.gov.gchq.palisade.resource.Resource;

import javax.persistence.AttributeConverter;

import java.io.IOException;
import java.util.Objects;

public class ResourceConverter implements AttributeConverter<Resource, String> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceConverter.class);

    private final ObjectMapper resourceMapper;

    public ResourceConverter() {
        // Intentionally uses a different ObjectMapper to the one in ApplicationConfiguration because of this OrphanedChildMixin
        // This allows resources to be stored without parents, which would otherwise be needlessly duplicated
        this.resourceMapper = JsonMapper.builder()
                .addMixIn(ChildResource.class, OrphanedChildJsonMixin.class)
                .build();
    }

    @Override
    public String convertToDatabaseColumn(final Resource resource) {
        if (Objects.isNull(resource)) {
            return null;
        }
        try {
            return this.resourceMapper.writeValueAsString(resource);
        } catch (JsonProcessingException e) {
            LOGGER.error("Could not convert resource to json string.", e);
            return null;
        }
    }

    @Override
    public Resource convertToEntityAttribute(final String attribute) {
        if (Objects.isNull(attribute)) {
            return null;
        }
        try {
            return this.resourceMapper.readValue(attribute, Resource.class);
        } catch (IOException e) {
            LOGGER.error("Conversion error while trying to convert json string to resource.", e);
            return null;
        }
    }
}
