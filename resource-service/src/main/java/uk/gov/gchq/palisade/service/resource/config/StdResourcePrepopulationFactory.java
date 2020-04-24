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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import uk.gov.gchq.palisade.Generated;
import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.service.ResourcePrepopulationFactory;
import uk.gov.gchq.palisade.service.SimpleConnectionDetail;
import uk.gov.gchq.palisade.util.ResourceBuilder;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static java.util.Objects.requireNonNull;

public class StdResourcePrepopulationFactory implements ResourcePrepopulationFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(StdResourcePrepopulationFactory.class);

    @Autowired
    private ObjectMapper objectMapper;

    private URI resourceId;
    private String connectionDetail;
    private Map<String, String> attributes = new HashMap<>();

    @Generated
    public URI getResourceId() {
        return resourceId;
    }

    @Generated
    public void setResourceId(final URI resourceId) {
        requireNonNull(resourceId);
        this.resourceId = resourceId;
    }

    @Generated
    public String getConnectionDetail() {
        return connectionDetail;
    }

    @Generated
    public void setConnectionDetail(final String connectionDetail) {
        requireNonNull(connectionDetail);
        this.connectionDetail = connectionDetail;
    }

    @Generated
    public Map<String, String> getAttributes() {
        return attributes;
    }

    @Generated
    public void setAttributes(final Map<String, String> attributes) {
        requireNonNull(attributes);
        this.attributes = attributes;
    }

    @Override
    public LeafResource build() {
        String type = requireNonNull(attributes.get("type"), "Attribute 'type' cannot be null");
        String serialisedfFormat = requireNonNull(attributes.get("serialisedFormat"), "Attribute 'serialisedFormat' cannot be null");
        return ResourceBuilder.create(resourceId, new SimpleConnectionDetail().uri(connectionDetail), type, serialisedfFormat, attributes);
    }

    @Override
    @Generated
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof StdResourcePrepopulationFactory)) {
            return false;
        }
        final StdResourcePrepopulationFactory that = (StdResourcePrepopulationFactory) o;
        return resourceId.equals(that.resourceId) &&
                connectionDetail.equals(that.connectionDetail) &&
                attributes.equals(that.attributes);
    }

    @Override
    @Generated
    public int hashCode() {
        return Objects.hash(resourceId, connectionDetail, attributes);
    }
}
