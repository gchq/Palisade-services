/*
 * Copyright 2019 Crown Copyright
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
package uk.gov.gchq.palisade.service.resource.request;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import uk.gov.gchq.palisade.ToStringBuilder;
import uk.gov.gchq.palisade.service.request.Request;

import java.util.Objects;
import java.util.StringJoiner;

import static java.util.Objects.requireNonNull;

/**
 * This class is used to request a list of {@link uk.gov.gchq.palisade.resource.Resource}'s
 * from the resource-service based on the serialisedFormat of those resources.
 * For example getting a list of all {@link uk.gov.gchq.palisade.resource.Resource}'s where the serialisedFormat is CSV.
 */
public class GetResourcesBySerialisedFormatRequest extends Request {
    private String serialisedFormat;

    // no-args constructor required
    public GetResourcesBySerialisedFormatRequest() {
    }

    /**
     *
     * @param serialisedFormat the serialisedFormat of the {@link uk.gov.gchq.palisade.resource.Resource}'s that you want to know about
     * @return the {@link GetResourcesBySerialisedFormatRequest}
     */
    public GetResourcesBySerialisedFormatRequest serialisedFormat(final String serialisedFormat) {
        requireNonNull(serialisedFormat, "The serialised format cannot be set to null.");
        this.serialisedFormat = serialisedFormat;
        return this;
    }

    public String getSerialisedFormat() {
        requireNonNull(serialisedFormat, "The serialised format has not been set.");
        return serialisedFormat;
    }

    public void setSerialisedFormat(final String serialisedFormat) {
        serialisedFormat(serialisedFormat);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        final GetResourcesBySerialisedFormatRequest that = (GetResourcesBySerialisedFormatRequest) o;
        return Objects.equals(serialisedFormat, that.serialisedFormat);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), serialisedFormat);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .appendSuper(super.toString())
                .append("serialisedFormat", serialisedFormat)
                .toString();
    }
}
