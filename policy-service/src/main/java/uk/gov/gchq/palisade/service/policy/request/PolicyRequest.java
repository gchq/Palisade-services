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
package uk.gov.gchq.palisade.service.policy.request;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.util.Assert;

import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.Generated;
import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.resource.Resource;
import uk.gov.gchq.palisade.service.policy.response.common.domain.User;



/**
 * Represents the original data that has been sent from the client to Palisade Service for a request to access data.
 * This data will be forwarded to a set of services with each contributing to the processing of this request.
 * This version represents the input for policy-service where the related policies are requested.
 * Next in the sequence will be the output for policy-service with the policies added.
 * Note there are two classes that effectively represent the same data but represent a different stage of the process.
 * uk.gov.gchq.palisade.service.resource.response.ResourceResponse is the output from the resource-service.
 * uk.gov.gchq.palisade.service.policy.request.PolicyRequest is the input for the policy-service.
 */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public final class PolicyRequest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final JsonNode context;  // Json Node representation of the Context
    private final JsonNode user;  //Json Node representation of the User
    private final JsonNode resource; // Json Node representation of the Resources

    @JsonCreator
    private PolicyRequest(
            final @JsonProperty("context") JsonNode context,
            final @JsonProperty("user") JsonNode user,
            final @JsonProperty("resource") JsonNode resource) {

        Assert.notNull(context, "Context cannot be null");
        Assert.notNull(user, "Context cannot be null");
        Assert.notNull(resource, "Resource cannot be null");

        this.user = user;
        this.resource = resource;
        this.context = context;
    }

    @Generated
    public Context getContext() throws JsonProcessingException {
        return MAPPER.treeToValue(this.context, Context.class);
    }

    @Generated
    public User getUser() throws JsonProcessingException {
        return MAPPER.treeToValue(this.user, User.class);
    }

    @Generated
    public LeafResource getResource() throws JsonProcessingException {
        return MAPPER.treeToValue(this.resource, LeafResource.class);
    }


    /**
     * Builder class for the creation of instances of the PolicyRequest.  This is a variant of the Fluent Builder
     * which will use Java Objects or JsonNodes equivalents for the components in the build.
     */
    public static class Builder {
        private JsonNode context;
        private JsonNode user;
        private JsonNode resources;


        public static IContext create() {
            return context -> user -> resource ->
                    new PolicyRequest(context, user, resource);
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
            default PolicyRequest withResource(Resource resource) {
                return withResourceNode(MAPPER.valueToTree(resource));
            }

            PolicyRequest withResourceNode(JsonNode resource);
        }

    }

}


