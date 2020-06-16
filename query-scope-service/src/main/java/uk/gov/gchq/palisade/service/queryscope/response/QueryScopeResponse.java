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
package uk.gov.gchq.palisade.service.queryscope.response;

import org.springframework.util.Assert;

import uk.gov.gchq.palisade.Generated;
import uk.gov.gchq.palisade.service.queryscope.response.common.domain.ResourceMetadata;

import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;


/**
 * Represents the  data that has been sent from the client to Palisade Service for a request to access data.
 * The data will be forwarded to a set of services with each contributing to the processing of this request.
 * This class represents the response from the Query Scope Service
 * The next in the sequence will the request for the Results Service.
 * Note there are two class that represents the same data where each has a different purpose.
 * uk.gov.gchq.palisade.service.queryscope.response.QueryScopeResponse is the output from the Query Scope Service
 * uk.gov.gchq.palisade.service.results.request.ResultsRequest is the input for the Response Service
 * The resources in this class will be modified from the set in the initial request to Query Scope Service.  This set
 * will have been processed with the rules and context with the resources here representing the redacted version.
 */


public class QueryScopeResponse {

    private final String token; // Unique identifier for this specific request end-to-end
    private final Map<String, ResourceMetadata> resources; //masked resources related to this query

    private QueryScopeResponse(final String token, final Map<String, ResourceMetadata> resources) {
        this.token = token;
        this.resources = resources;
    }

    @Generated
    public String getToken() {
        return token;
    }

    @Generated
    public Map<String, ResourceMetadata> getResources() {
        return resources;
    }

    @Override
    @Generated
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof QueryScopeResponse)) {
            return false;
        }
        QueryScopeResponse that = (QueryScopeResponse) o;
        return token.equals(that.token) &&
                resources.equals(that.resources);
    }

    @Override
    @Generated
    public int hashCode() {
        return Objects.hash(token, resources);
    }

    @Override
    @Generated
    public String toString() {
        return new StringJoiner(", ", QueryScopeResponse.class.getSimpleName() + "[", "]")
                .add("token='" + token + "'")
                .add("resources=" + resources)
                .add(super.toString())
                .toString();
    }

    /**
     * Builder class for the creation of instances of the QueryScopeResponse.  The variant of the Builder Pattern is
     * meant to be used by first populating the Builder class and then us this to create the QueryScopeResponse class.
     */
    public static class Builder {
        private String token;
        private Map<String, ResourceMetadata> resources;

        public Builder token(final String token) {
            this.token = token;
            return this;
        }

        public Builder resource(final Map<String, ResourceMetadata> resources) {
            this.resources = resources;
            return this;
        }

        public QueryScopeResponse build() {
            Assert.notNull(token, "Token Id cannot be null");
            Assert.notNull(resources, "Resources cannot be null");
            Assert.notNull(resources, "Resources cannot be empty");
            return new QueryScopeResponse(token, resources);
        }
    }

}
