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

package uk.gov.gchq.palisade.service.attributemask.common.resource;

import uk.gov.gchq.palisade.service.attributemask.common.Generated;
import uk.gov.gchq.palisade.service.attributemask.common.resource.impl.FileResource;
import uk.gov.gchq.palisade.service.attributemask.common.service.ConnectionDetail;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;

import static java.util.Objects.requireNonNull;

/**
 * This is a partial implementation of a LeafResource which provides basic member-variable storage to implement
 * methods required of a LeafResource. The only missing detail here is how resourceIds are managed.
 * See {@link FileResource} for a concrete implementation with an id.
 * This class is mostly used when deserialisation to a LeafResource is required, but the interface can't be used.
 */
public abstract class AbstractLeafResource extends AbstractResource implements LeafResource {

    private String type;
    private String serialisedFormat;
    private ConnectionDetail connectionDetail;
    private ParentResource parent;
    private HashMap<String, Serializable> attributes = new HashMap<>();

    public AbstractLeafResource() {
    }

    @Generated
    public AbstractLeafResource type(final String type) {
        this.setType(type);
        return this;
    }

    @Generated
    public AbstractLeafResource serialisedFormat(final String serialisedFormat) {
        this.setSerialisedFormat(serialisedFormat);
        return this;
    }

    @Generated
    public AbstractLeafResource connectionDetail(final ConnectionDetail connectionDetail) {
        this.setConnectionDetail(connectionDetail);
        return this;
    }

    /**
     * Sets the attributes for the AbstractLeafResource
     *
     * @param attributes a {@link Map} of {@link String} and {@link Serializable}.
     * @return a AbstractLeafResource object.
     */
    @Generated
    public AbstractLeafResource attributes(final Map<String, Serializable> attributes) {
        this.setAttributes(attributes);
        return this;
    }

    /**
     * Sets the attributes for the AbstractLeafResource
     *
     * @param attributeKey   a {@link String} value for the key.
     * @param attributeValue a {@link Serializable} value
     * @return the AbstractLeafResource object
     */
    @Generated
    public AbstractLeafResource attribute(final String attributeKey, final Serializable attributeValue) {
        this.setAttribute(attributeKey, attributeValue);
        return this;
    }

    @Generated
    public AbstractLeafResource parent(final ParentResource parent) {
        this.setParent(parent);
        return this;
    }

    @Override
    @Generated
    public String getType() {
        return type;
    }

    @Override
    @Generated
    public void setType(final String type) {
        requireNonNull(type);
        this.type = type;
    }

    @Override
    @Generated
    public String getSerialisedFormat() {
        return serialisedFormat;
    }

    @Override
    @Generated
    public void setSerialisedFormat(final String serialisedFormat) {
        requireNonNull(serialisedFormat);
        this.serialisedFormat = serialisedFormat;
    }

    @Override
    @Generated
    public ConnectionDetail getConnectionDetail() {
        return connectionDetail;
    }

    @Override
    @Generated
    public void setConnectionDetail(final ConnectionDetail connectionDetail) {
        requireNonNull(connectionDetail);
        this.connectionDetail = connectionDetail;
    }

    @Override
    @Generated
    public ParentResource getParent() {
        return parent;
    }

    @Override
    @Generated
    public void setParent(final ParentResource parent) {
        requireNonNull(parent);
        this.parent = parent;
    }

    @Generated
    public Map<String, Serializable> getAttributes() {
        return attributes;
    }

    @Generated
    public void setAttributes(final Map<String, Serializable> attributes) {
        requireNonNull(attributes);
        this.attributes = new HashMap<>(attributes);
    }

    @Generated
    public Object getAttribute(final String attributeKey) {
        return this.attributes.getOrDefault(attributeKey, null);
    }

    @Generated
    public Boolean isAttributeSet(final String attributeKey) {
        return this.attributes.containsKey(attributeKey);
    }

    /**
     * Sets the key and value of the attributes {@link Map} for the AbstractLeafResource
     *
     * @param attributeKey   a {@link String} value for the attribute key.
     * @param attributeValue a {@link Serializable} value for the attribute value.
     */
    @Generated
    public void setAttribute(final String attributeKey, final Serializable attributeValue) {
        requireNonNull(attributeKey, "The attributeKey cannot be set to null.");
        requireNonNull(attributeKey, "The attributeValue cannot be set to null.");
        this.attributes.put(attributeKey, attributeValue);

    }

    @Override
    @Generated
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AbstractLeafResource)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        final AbstractLeafResource that = (AbstractLeafResource) o;
        return Objects.equals(type, that.type) &&
                Objects.equals(serialisedFormat, that.serialisedFormat) &&
                Objects.equals(connectionDetail, that.connectionDetail) &&
                Objects.equals(parent, that.parent) &&
                Objects.equals(attributes, that.attributes);
    }

    @Override
    @Generated
    public int hashCode() {
        return Objects.hash(super.hashCode(), type, serialisedFormat, connectionDetail, parent, attributes);
    }

    @Override
    @Generated
    public String toString() {
        return new StringJoiner(", ", AbstractLeafResource.class.getSimpleName() + "[", "]")
                .add("type='" + type + "'")
                .add("serialisedFormat='" + serialisedFormat + "'")
                .add("connectionDetail=" + connectionDetail)
                .add("parent=" + parent)
                .add("attributes=" + attributes)
                .add(super.toString())
                .toString();
    }
}
