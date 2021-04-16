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
package uk.gov.gchq.palisade.service.resource.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.lang.NonNull;

import uk.gov.gchq.palisade.service.resource.common.resource.ChildResource;
import uk.gov.gchq.palisade.service.resource.common.resource.Resource;
import uk.gov.gchq.palisade.service.resource.config.ApplicationConfiguration;

import java.io.IOException;

/**
 * Contains classes to help with converting {@link Resource} objects to and from {@link String} values
 */
public final class ResourceConverter {
    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceConverter.class);
    public static final ObjectMapper RESOURCE_MAPPER;

    static {
        // Intentionally uses a different ObjectMapper to the one in ApplicationConfiguration because of this OrphanedChildMixin
        // This allows resources to be stored without parents, which would otherwise be needlessly duplicated
        // Note the copy, this modified mapper is still based on the original
        RESOURCE_MAPPER = new ApplicationConfiguration().objectMapper().copy()
                .addMixIn(ChildResource.class, OrphanedChildJsonMixin.class);
    }

    private ResourceConverter() {
        // Utility class
    }

    /**
     * Converts the {@link String} value to a {@link Resource}
     */
    @ReadingConverter
    public static class Reading implements Converter<String, Resource> {
        @Override
        public Resource convert(final @NonNull String json) {
            try {
                return RESOURCE_MAPPER.readValue(json, Resource.class);
            } catch (IOException e) {
                LOGGER.error("Conversion error while trying to convert json string to resource.", e);
                return null;
            }
        }
    }

    /**
     * Converts the {@link Resource} value to a {@link String}
     */
    @WritingConverter
    public static class Writing implements Converter<Resource, String> {
        @Override
        public String convert(final @NonNull Resource resource) {
            try {
                return RESOURCE_MAPPER.writeValueAsString(resource);
            } catch (
                    JsonProcessingException e) {
                LOGGER.error("Could not convert resource to json string.", e);
                return null;
            }
        }
    }
}
