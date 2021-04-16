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

package uk.gov.gchq.palisade.service.data.common.data.reader;

import uk.gov.gchq.palisade.service.data.common.Context;
import uk.gov.gchq.palisade.service.data.common.Generated;
import uk.gov.gchq.palisade.service.data.common.resource.LeafResource;
import uk.gov.gchq.palisade.service.data.common.rule.Rules;
import uk.gov.gchq.palisade.service.data.common.user.User;

import java.util.Objects;
import java.util.StringJoiner;

import static java.util.Objects.requireNonNull;

/**
 * This class is used to request that the {@link DataReader}
 * read a resource and apply the necessary rules. The details for this request are persisted by
 * the attribute-masking-service and retrieved by the data-service.
 */
public class DataReaderRequest {
    private LeafResource resource;
    private User user;
    private Context context;
    private Rules<?> rules;

    public DataReaderRequest() {
        // no args constructor required
    }

    /**
     * Set the resource object for this {@link DataReaderRequest} object
     *
     * @param resource the resource to be accessed
     * @return the {@link DataReaderRequest}
     */
    @Generated
    public DataReaderRequest resource(final LeafResource resource) {
        this.setResource(resource);
        return this;
    }

    /**
     * Set the user object for this {@link DataReaderRequest} object
     *
     * @param user the user that requested the data
     * @return the {@link DataReaderRequest}
     */
    @Generated
    public DataReaderRequest user(final User user) {
        this.setUser(user);
        return this;
    }

    /**
     * Set the context object for this {@link DataReaderRequest} object
     *
     * @param context the Context that the user provided for why they want the data
     * @return the {@link DataReaderRequest}
     */
    @Generated
    public DataReaderRequest context(final Context context) {
        this.setContext(context);
        return this;
    }

    /**
     * Set the rules object for this {@link DataReaderRequest} object
     *
     * @param rules the list of rules to be applied to the data to ensure policy compliance
     * @return the {@link DataReaderRequest}
     */
    @Generated
    public DataReaderRequest rules(final Rules<?> rules) {
        this.setRules(rules);
        return this;
    }

    @Generated
    public Context getContext() {
        return context;
    }

    @Generated
    public void setContext(final Context context) {
        requireNonNull(context);
        this.context = context;
    }

    @Generated
    @SuppressWarnings("java:S1452")
    public Rules<?> getRules() {
        return rules;
    }

    @Generated
    public void setRules(final Rules<?> rules) {
        requireNonNull(rules);
        this.rules = rules;
    }

    @Generated
    public LeafResource getResource() {
        return resource;
    }

    @Generated
    public void setResource(final LeafResource resource) {
        requireNonNull(resource);
        this.resource = resource;
    }

    @Generated
    public User getUser() {
        return user;
    }

    @Generated
    public void setUser(final User user) {
        requireNonNull(user);
        this.user = user;
    }

    @Override
    @Generated
    public String toString() {
        return new StringJoiner(", ", DataReaderRequest.class.getSimpleName() + "[", "]")
                .add("resource=" + resource)
                .add("user=" + user)
                .add("context=" + context)
                .add("rules=" + rules)
                .toString();
    }

    @Override
    @Generated
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DataReaderRequest)) {
            return false;
        }
        final DataReaderRequest that = (DataReaderRequest) o;
        return Objects.equals(resource, that.resource) &&
                Objects.equals(user, that.user) &&
                Objects.equals(context, that.context) &&
                Objects.equals(rules, that.rules);
    }

    @Override
    @Generated
    public int hashCode() {
        return Objects.hash(super.hashCode(), resource, user, context, rules);
    }


}
