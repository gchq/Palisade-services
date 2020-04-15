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

package uk.gov.gchq.palisade.service.resource.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;

import uk.gov.gchq.palisade.Generated;
import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.service.ResourcePrepopulationFactory;

import static java.util.Objects.requireNonNull;

@ConfigurationProperties
public class JsonResourcePrepopulationFactory implements ResourcePrepopulationFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(JsonResourcePrepopulationFactory.class);

    @Autowired
    private ObjectMapper objectMapper;

    private String tag;
    private String jsonBlob;

    @Generated
    public String getTag() {
        return tag;
    }

    @Generated
    public void setTag(final String tag) {
        requireNonNull(tag);
        this.tag = tag;
    }

    @Generated
    public String getJsonBlob() {
        return jsonBlob;
    }

    @Generated
    public void setJsonBlob(final String jsonBlob) {
        requireNonNull(jsonBlob);
        this.jsonBlob = jsonBlob;
    }

    @Override
    public LeafResource build() {
        try {
            return objectMapper.readValue(jsonBlob, LeafResource.class);
        } catch (JsonProcessingException e) {
            LOGGER.error("Resource tagged '{}' had malformed jsonBlob '{}'", tag, jsonBlob);
            LOGGER.error("JsonProcessingException was", e);
            throw new RuntimeException(e);
        }
    }

}
