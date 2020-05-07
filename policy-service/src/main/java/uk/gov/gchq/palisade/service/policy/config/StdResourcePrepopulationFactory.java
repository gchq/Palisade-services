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

package uk.gov.gchq.palisade.service.policy.config;

import uk.gov.gchq.palisade.Generated;
import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.resource.Resource;
import uk.gov.gchq.palisade.service.ConnectionDetail;
import uk.gov.gchq.palisade.service.ResourcePrepopulationFactory;
import uk.gov.gchq.palisade.util.ResourceBuilder;

import java.io.File;
import java.net.URI;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

public class StdResourcePrepopulationFactory implements ResourcePrepopulationFactory {
    private String resourceId = "";
    private String rootId = "";
    private String connectionDetail = "";
    private Map<String, String> attributes = Collections.emptyMap();

    public StdResourcePrepopulationFactory() {
        // empty constructor
    }

    public StdResourcePrepopulationFactory(final String resourceId, final String rootId, final String connectionDetail, final Map<String, String> attributes) {
        this.resourceId = resourceId;
        this.rootId = rootId;
        this.connectionDetail = connectionDetail;
        this.attributes = attributes;
    }

    @Generated
    public String getResourceId() {
        return resourceId;
    }

    @Generated
    public void setResourceId(final String resourceId) {
        requireNonNull(resourceId);
        this.resourceId = resourceId;
    }

    @Generated
    public String getRootId() {
        return rootId;
    }

    @Generated
    public void setRootId(final String rootId) {
        requireNonNull(rootId);
        this.rootId = rootId;
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
    public Entry<Resource, LeafResource> build(final Function<String, ConnectionDetail> connectionDetailMapper) {
        String type = requireNonNull(attributes.get("type"), "Attribute 'type' cannot be null");
        String serialisedFormat = requireNonNull(attributes.get("serialisedFormat"), "Attribute 'serialisedFormat' cannot be null");
        ConnectionDetail simpleConnectionDetail = connectionDetailMapper.apply(connectionDetail);
        Resource rootResource = ResourceBuilder.create(rootId);
        URI resourceURI = new File(resourceId).toURI();
        Entry<Resource, LeafResource> ret = new SimpleImmutableEntry<>(rootResource, ResourceBuilder.create(resourceURI, simpleConnectionDetail, type, serialisedFormat, attributes));
        return ret;
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
        return Objects.equals(resourceId, that.resourceId) &&
                Objects.equals(rootId, that.rootId) &&
                Objects.equals(connectionDetail, that.connectionDetail) &&
                Objects.equals(attributes, that.attributes);
    }

    @Override
    @Generated
    public int hashCode() {
        return Objects.hash(resourceId, rootId, connectionDetail, attributes);
    }
}
