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
package uk.gov.gchq.palisade.service.results.request;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.util.Assert;

import uk.gov.gchq.palisade.Generated;
import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.resource.Resource;

import java.util.Objects;
import java.util.StringJoiner;

/**
 * Represents the original data that has been sent from the client to Palisade Service for a request to access data.
 * This data will be forwarded to a set of services with each contributing to the processing of this request.
 * This version represents the input for results-service to construct a response to the original request from the client.
 * Next in the sequence will be the output for the result-service which will be a response to the client.
 * Note there are two classes that effectively represent the same data but represent a different stage of the process.
 * uk.gov.gchq.palisade.service.queryscope.response.QueryScopeResponse is the output from the query-scope-service.
 * uk.gov.gchq.palisade.service.results.request.ResultsRequest is the input for the results-service.
 */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class ResultsRequest {

    private static final ObjectMapper MAPPER = new ObjectMapper();


    private final JsonNode resource; // Json Node representation of the Resources

    @JsonCreator
    private ResultsRequest(

            final @JsonProperty("resource") JsonNode resource) {

        Assert.notNull(resource, "Resources cannot be null");

        this.resource = resource;
    }



    @Generated
    public LeafResource getResource() throws JsonProcessingException {
        return MAPPER.treeToValue(this.resource, LeafResource.class);
    }

    @Override
    @Generated
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ResultsRequest)) {
            return false;
        }
        ResultsRequest that = (ResultsRequest) o;
        return resource.equals(that.resource);
    }

    @Override
    @Generated
    public int hashCode() {
        return Objects.hash(resource);
    }

    @Override
    @Generated
    public String toString() {
        return new StringJoiner(", ", ResultsRequest.class.getSimpleName() + "[", "]")
                .add("resource=" + resource)
                .add(super.toString())
                .toString();
    }

    /**
     * Builder class for the creation of instances of the ResultsRequest.  This is a variant of the Fluent Builder
     * which will use Java Objects or JsonNodes equivalents for the components in the build.
     */
    public static class Builder {

        private JsonNode resources;

        /**
         * Starter method for the Builder class.  This method is called to start the process of creating the
         * ResultsRequest class.
         *
         * @return interface  {@link IResource} for the next step in the build.
         */
        public static IResource create() {
            return ResultsRequest::new;
        }



        /**
         * Adds the resource to this message.
         */
        interface IResource {

            /**
             * Adds the resource that has been requested to access.
             *
             * @param resource that is requested to access.
             * @return class {@link ResultsRequest} for the completed class from the builder.
             */
            default ResultsRequest withResource(Resource resource) {
                return withResourceNode(MAPPER.valueToTree(resource));
            }

            /**
             * Adds the resource that has been requested to access.  Uses a JsonNode string form of the information.
             *
             * @param resource that is requested to access.
             * @return class {@link ResultsRequest} for the completed class from the builder.
             */
            ResultsRequest withResourceNode(JsonNode resource);
        }

    }

}
