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
package uk.gov.gchq.palisade.service.queryscope.request;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.springframework.util.Assert;

import uk.gov.gchq.palisade.Generated;
import uk.gov.gchq.palisade.service.queryscope.response.common.domain.ResourceMetadata;
import uk.gov.gchq.palisade.service.queryscope.response.common.domain.Rule;
import uk.gov.gchq.palisade.service.queryscope.response.common.domain.User;

import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * Represents the  data that has been sent from the client to Palisade Service for a request to access data.
 * The data will be forwarded to a set of services with each contributing to the processing of this request.
 * This class represents the request for the Query Scope Service
 * The next in the sequence will the response for the Query Scope Service.
 * Note there are two class that represents the same data where each has a different purpose.
 * uk.gov.gchq.palisade.service.policy.response.PolicyResponse is the output from the Resource Service
 * uk.gov.gchq.palisade.service.queryscope.request.QueryScopeRequest is the input for the Query Scope Service
 */
@JsonDeserialize(builder = QueryScopeRequest.Builder.class)
public final class QueryScopeRequest {

    private final String token; // Unique identifier for this specific request end-to-end
    private final User user;  // User that is making the request
    private final Map<String, ResourceMetadata> resources; //map of resources related to this query
    private final Map<String, String> context;  // represents the context information
    private final Map<String, Rule> rules; // holds all of the rules applicable to this request


    private QueryScopeRequest(final String token, final User user, final Map<String, ResourceMetadata> resources, final Map<String, String> context, final Map<String, Rule> rules) {
        this.token = token;
        this.user = user;
        this.resources = resources;
        this.context = context;
        this.rules = rules;
    }

    @Generated
    public String getToken() {
        return token;
    }

    @Generated
    public User getUser() {
        return user;
    }

    @Generated
    public Map<String, ResourceMetadata> getResources() {
        return resources;
    }

    @Generated
    public Map<String, String> getContext() {
        return context;
    }

    @Generated
    public Map<String, Rule> getRules() {
        return rules;
    }

    @Override
    @Generated
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof QueryScopeRequest)) {
            return false;
        }
        QueryScopeRequest that = (QueryScopeRequest) o;
        return token.equals(that.token) &&
                user.equals(that.user) &&
                resources.equals(that.resources) &&
                context.equals(that.context) &&
                rules.equals(that.rules);
    }

    @Override
    @Generated
    public int hashCode() {
        return Objects.hash(token, user, resources, context, rules);
    }

    @Override
    @Generated
    public String toString() {
        return new StringJoiner(", ", QueryScopeRequest.class.getSimpleName() + "[", "]")
                .add("token='" + token + "'")
                .add("user=" + user)
                .add("resources=" + resources)
                .add("context=" + context)
                .add("rules=" + rules)
                .add(super.toString())
                .toString();
    }

    /**
     * Builder class for the creation of instances of the QueryScopeRequest.  The variant of the Builder Pattern is
     * meant to be used by first populating the Builder class and then us this to create the QueryScopeRequest class.
     */
    @JsonPOJOBuilder
    public static class Builder {
        private String token;
        private User user;
        private Map<String, ResourceMetadata> resources;
        private Map<String, String> context;
        private Map<String, Rule> rules;

        public Builder token(final String token) {
            this.token = token;
            return this;
        }

        public Builder userJson(final User user) {
            this.user = user;
            return this;
        }

        public Builder resource(final Map<String, ResourceMetadata> resources) {
            this.resources = resources;
            return this;
        }

        public Builder context(final Map<String, String> context) {
            this.context = context;
            return this;
        }

        public Builder rules(final Map<String, Rule> rules) {
            this.rules = rules;
            return this;
        }


        public QueryScopeRequest build() {
            Assert.notNull(token, "Token Id cannot be null");
            Assert.notNull(user, "User cannot be null");
            Assert.notNull(resources, "Resources cannot be null");
            Assert.notNull(resources, "Resources cannot be empty");
            Assert.notNull(context, "Context cannot be null");
            Assert.notEmpty(context, "Context cannot be empty");
            Assert.notNull(rules, "Context cannot be null");
            Assert.notEmpty(rules, "Context cannot be empty");
            return new QueryScopeRequest(token, user, resources, context, rules);
        }
    }

}
