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

package uk.gov.gchq.palisade.service.resource.common.resource.impl;

import uk.gov.gchq.palisade.service.resource.common.Generated;
import uk.gov.gchq.palisade.service.resource.common.RegisterJsonSubType;
import uk.gov.gchq.palisade.service.resource.common.resource.ConnectionDetail;

import java.util.Objects;
import java.util.StringJoiner;

import static java.util.Objects.requireNonNull;

/**
 * A simple implementation of the {@link ConnectionDetail} that points to the correct Data Service
 */
@RegisterJsonSubType(ConnectionDetail.class)
public class SimpleConnectionDetail implements ConnectionDetail {
    private static final long serialVersionUID = 1L;

    private String serviceName;

    public SimpleConnectionDetail() {
        //no-args constructor needed for serialization only
    }

    @Generated
    public SimpleConnectionDetail serviceName(final String serviceName) {
        this.setServiceName(serviceName);
        return this;
    }

    @Generated
    public String getServiceName() {
        return serviceName;
    }

    @Generated
    public void setServiceName(final String serviceName) {
        requireNonNull(serviceName);
        this.serviceName = serviceName;
    }

    @Override
    @Generated
    public String createConnection() {
        return getServiceName();
    }

    @Override
    @Generated
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SimpleConnectionDetail)) {
            return false;
        }
        SimpleConnectionDetail that = (SimpleConnectionDetail) o;
        return serviceName.equals(that.serviceName);
    }

    @Override
    @Generated
    public int hashCode() {
        return Objects.hash(serviceName);
    }

    @Override
    @Generated
    public String toString() {
        return new StringJoiner(", ", SimpleConnectionDetail.class.getSimpleName() + "[", "]")
                .add("serviceName='" + serviceName + "'")
                .add(super.toString())
                .toString();
    }
}
