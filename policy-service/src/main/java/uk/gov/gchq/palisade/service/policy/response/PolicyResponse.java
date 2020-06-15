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
package uk.gov.gchq.palisade.service.policy.response;



import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.springframework.util.Assert;
import uk.gov.gchq.palisade.Generated;
import uk.gov.gchq.palisade.service.policy.response.common.ResourceMetadata;
import uk.gov.gchq.palisade.service.policy.response.common.domain.Rule;
import uk.gov.gchq.palisade.service.policy.response.common.domain.User;

import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * This is the message that will be sent from the PolicyService to the QueryScopeService
 * It is, therefore a Response from the PolicyService and a Request into the QueryScopeService
 * Note there are two classes of this type:
 * uk.gov.gchq.palisade.service.policy.response.PolicyResponse
 * uk.gov.gchq.palisade.service.queryscope.request.PolicyResponse
 */
/**
 * Represents the  data that has been sent from the client to Palisade Service for a request to access data.
 * The data will be forwarded to a set of services with each contributing to the processing of this request.
 * This class represents the response from the Policy Service
 * The next in the sequence will the request for the Query Scope Service.
 * Note there are two class that represents the same data where each has a different purpose.
 * uk.gov.gchq.palisade.service.policy.response.PolicyResponse is the output from the Resource Service
 * uk.gov.gchq.palisade.service.queryscope.request.QueryScopeRequest is the input for the Query Scope Service
 */
@JsonDeserialize(builder = PolicyResponse.Builder.class)
public final class PolicyResponse {

    private final String token; // Unique identifier for this specific request end-to-end
    private final User user;  //
    private final Map<String, ResourceMetadata> resources; //map of resources related to this query
    private final Map<String, String> context;  // represents the context information
    private final Map<String, Rule> rules; // holds all of the rules applicable to this request


    private PolicyResponse(String token, User user, Map<String, ResourceMetadata> resources, Map<String, String> context,  Map<String, Rule> rules ) {
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
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PolicyResponse)) {
            return false;
        }
        PolicyResponse that = (PolicyResponse) o;
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
        return new StringJoiner(", ", PolicyResponse.class.getSimpleName() + "[", "]")
                .add("token='" + token + "'")
                .add("user=" + user)
                .add("resources=" + resources)
                .add("context=" + context)
                .add("rules=" + rules)
                .add(super.toString())
                .toString();
    }

    /**
     * Builder class for the creation of instances of the PolicyRequest.  The variant of the Builder Pattern is
     * meant to be used by first populating the Builder class and then us this to create the PolicyRequest class.
     */
    @JsonPOJOBuilder
    public static class Builder {
        private String token;
        private User user;
        private Map<String, ResourceMetadata> resources;
        private Map<String, String> context;
        private Map<String, Rule> rules;

        public Builder token(String token) {
            this.token = token;
            return this;
        }

        public Builder userJson(User user) {
            this.user = user;
            return this;
        }

        public Builder resource(Map<String, ResourceMetadata> resources) {
            this.resources = resources;
            return this;
        }

        public Builder context(Map<String, String> context) {
            this.context = context;
            return this;
        }

        public Builder rules(  Map<String, Rule> rules){
            this.rules = rules;
            return this;
        }


        public PolicyResponse build() {
            Assert.notNull(token, "Token Id cannot be null");
            Assert.notNull(user, "User cannot be null");
            Assert.notNull(resources, "Resource Id cannot be null");
            Assert.notNull(context, "Context  cannot be null");
            Assert.notEmpty(context, "Context  cannot be empty");
            Assert.notNull(rules, "Context  cannot be null");
            Assert.notEmpty(rules, "Context  cannot be empty");
            return new PolicyResponse(token, user, resources, context, rules);
        }
    }

}
