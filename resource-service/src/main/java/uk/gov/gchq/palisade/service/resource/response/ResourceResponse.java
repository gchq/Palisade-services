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
package uk.gov.gchq.palisade.service.resource.response;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.util.Assert;

import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.Generated;
import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.service.resource.response.common.domain.User;

import java.util.Objects;
import java.util.StringJoiner;


/**
 * Represents the original data that has been sent from the client to Palisade Service for a request to access data.
 * This data will be forwarded to a set of services with each contributing to the processing of this request.
 * This version represents the output for resource-service where the resource has been identified.
 * Next in the sequence will be the request for policy-service.
 * Note there are two classes that effectively represent the same data but represent a different stage of the process.
 * uk.gov.gchq.palisade.service.resource.response.ResourceResponse is the output from the resource-service.
 * uk.gov.gchq.palisade.service.policy.request.PolicyRequest is the input for the policy-service.
 */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public final class ResourceResponse {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final JsonNode context;  // Json Node representation of the Context
    private final JsonNode user;  //Json Node representation of the User
    public final LeafResource resource; // Resources related to this query

    private ResourceResponse(
            final  @JsonProperty("context") JsonNode context,
            final  @JsonProperty("user") JsonNode user,
            final  @JsonProperty("resource") LeafResource resource) {

        Assert.notNull(context, "Context cannot be null");
        Assert.notNull(user, "User cannot be null");
        Assert.notNull(resource, "User cannot be null");

        this.context = context;
        this.user = user;
        this.resource = resource;
    }

    @Generated
    public Context getContext() throws JsonProcessingException {
        return MAPPER.treeToValue(context, Context.class);
    }

    @Generated
    public User getUser() throws JsonProcessingException {
        return MAPPER.treeToValue(user, User.class);
    }


    @Override
    @Generated
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ResourceResponse)) {
            return false;
        }
        ResourceResponse that = (ResourceResponse) o;
        return context.equals(that.context) &&
                user.equals(that.user) &&
                resource.equals(that.resource);
    }

    @Override
    @Generated
    public int hashCode() {
        return Objects.hash(context, user, resource);
    }

    @Override
    @Generated
    public String toString() {
        return new StringJoiner(", ", ResourceResponse.class.getSimpleName() + "[", "]")
                .add("context=" + context)
                .add("user=" + user)
                .add("resources=" + resource)
                .add(super.toString())
                .toString();
    }

    /**
     * Builder class for the creation of instances of the UserResponse.  This is a variant of the Fluent Builder
     * which will build the Java objects from Json string.
     */
    public static class Builder {
        private JsonNode context;
        private JsonNode user;
        private LeafResource resource;

        public static IContext create() {
            return context -> user -> resource ->
                    new ResourceResponse(context, user, resource);
        }

        interface IContext {
            default IUser withContext(Context context) {
                return withContextNode(MAPPER.valueToTree(context));
            }

            IUser withContextNode(JsonNode context);
        }

        interface IUser {
            default IResource withUser(User user) {
                return withUserNode(MAPPER.valueToTree(user));
            }

            IResource withUserNode(JsonNode context);
        }

        interface IResource {
            ResourceResponse withResource(LeafResource resource);

        }


    }
}
