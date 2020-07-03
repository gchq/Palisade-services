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
package uk.gov.gchq.palisade.service.resource.request;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.util.Assert;

import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.Generated;
import uk.gov.gchq.palisade.service.resource.response.common.domain.User;

import java.util.Objects;
import java.util.StringJoiner;


/**
 * Represents the original data that has been sent from the client to Palisade Service for a request to access data.
 * This data will be forwarded to a set of services with each contributing to the processing of this request.
 * This version represents the input for resource-service where the resource is to be identified.
 * Next in the sequence will be the response for resource-service.
 * Note there are two classes that effectively represent the same data but represent a different stage of the process.
 * uk.gov.gchq.palisade.service.palisade.response.UserResponse is the response with the data from user-service included.
 * uk.gov.gchq.palisade.service.resource.request.ResourceRequest is the input for the resource-service.
 */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public final class ResourceRequest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public final String resourceId;  //Resource that that is being asked to access
    private final JsonNode context;  // Json Node representation of Context
    private final JsonNode user;  //Json Node representation of the User

    @JsonCreator
    private ResourceRequest(
            final @JsonProperty("resourceId") String resourceId,
            final @JsonProperty("context") JsonNode context,
            final @JsonProperty("user") JsonNode user) {

        Assert.notNull(resourceId, "Resource cannot be null");
        Assert.notNull(context, "Context cannot be null");
        Assert.notNull(user, "User cannot be null");

        this.resourceId = resourceId;
        this.context = context;
        this.user = user;
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
        if (!(o instanceof ResourceRequest)) {
            return false;
        }
        ResourceRequest that = (ResourceRequest) o;
        return resourceId.equals(that.resourceId) &&
                context.equals(that.context) &&
                user.equals(that.user);
    }

    @Override
    @Generated
    public int hashCode() {
        return Objects.hash(resourceId, context, user);
    }

    @Override
    @Generated
    public String toString() {
        return new StringJoiner(", ", ResourceRequest.class.getSimpleName() + "[", "]")
                .add("resourceId='" + resourceId + "'")
                .add("context=" + context)
                .add("user=" + user)
                .add(super.toString())
                .toString();
    }

    /**
     * Builder class for the creation of instances of the UserResponse.  This is a variant of the Fluent Builder
     * which will build the Java objects from Json string.
     */
    public static class Builder {
        private String resourceId;
        private JsonNode context;
        private JsonNode user;

        public static IResource create() {
            return resource -> context -> user ->
                    new ResourceRequest(resource, context, user);
        }

        interface IResource {
            IContext withResource(String resourceId);
        }

        interface IContext {
            default IUser withContext(Context context) {
                return withContextNode(MAPPER.valueToTree(context));
            }

            IUser withContextNode(JsonNode context);

        }

        interface IUser {
            default ResourceRequest withUser(User user) {
                return withUserNode(MAPPER.valueToTree(user));
            }

            ResourceRequest withUserNode(JsonNode context);
        }

    }
}

