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

import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.RequestId;
import uk.gov.gchq.palisade.User;
import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.rule.Rules;
import uk.gov.gchq.palisade.service.request.DataRequestConfig;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

@Entity
@Table(name = "data_request",
        uniqueConstraints = {@UniqueConstraint(columnNames = "request_id")},
        indexes = {@Index(name = "request", columnList = "request_id")})
public class DataRequestEntity {

    @Id
    @Column(name = "request_id", columnDefinition = "varchar(255)")
    private String requestId;

    @Column(name = "user", columnDefinition = "clob")
    @Convert(converter = UserConverter.class)
    private User user;

    @Column(name = "context", columnDefinition = "clob")
    @Convert(converter = ContextConverter.class)
    private Context context;

    @Transient
    private Map<LeafResource, Rules> leafResourceMap;

    public DataRequestEntity() {
    }

    public DataRequestEntity(final DataRequestConfig config) {
        this.requestId = requireNonNull(config, "DataRequestConfig").getOriginalRequestId().getId();
        this.context = config.getContext();
        this.user = config.getUser();
        this.leafResourceMap = Optional.ofNullable(config.getRules()).orElseGet(Collections::emptyMap);
    }

    public DataRequestConfig dataRequestConfig() {
        final DataRequestConfig request = new DataRequestConfig().user(this.user).context(this.context).rules(this.leafResourceMap);
        request.setOriginalRequestId(new RequestId().id(this.requestId));
        return request;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(final String requestId) {
        this.requestId = requireNonNull(requestId, "requestId");
    }

    public User getUser() {
        return user;
    }

    public void setUser(final User user) {
        this.user = requireNonNull(user, "user");
    }

    public Context getContext() {
        return context;
    }

    public void setContext(final Context context) {
        this.context = requireNonNull(context, "context");
    }

    public Map<LeafResource, Rules> getLeafResourceMap() {
        return Optional.ofNullable(leafResourceMap).orElseGet(Collections::emptyMap);
    }

    public void setLeafResourceMap(final Map<LeafResource, Rules> leafResourceMap) {
        this.leafResourceMap = Optional.ofNullable(leafResourceMap).orElseGet(Collections::emptyMap);
    }
}
