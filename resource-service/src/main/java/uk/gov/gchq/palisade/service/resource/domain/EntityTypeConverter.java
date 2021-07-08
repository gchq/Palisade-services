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

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;

/**
 * Contains classes to help with converting {@link EntityType} objects to and from {@link Integer} values
 */
public final class EntityTypeConverter {

    private EntityTypeConverter() {
        // Utility class
    }

    /**
     * Converts the {@link Integer} value to an {@link EntityType}
     */
    @ReadingConverter
    public static class Reading implements Converter<Integer, EntityType> {
        @Override
        public EntityType convert(final Integer integer) {
            return EntityType.values()[integer];
        }
    }

    /**
     * Converts the {@link EntityType} value to an {@link Integer}
     */
    @WritingConverter
    public static class Writing implements Converter<EntityType, Integer> {
        @Override
        public Integer convert(final EntityType entityType) {
            return entityType.ordinal();
        }
    }
}
