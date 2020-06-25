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
package uk.gov.gchq.palisade.service.user.response;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.util.Assert;


import uk.gov.gchq.palisade.service.user.response.common.domain.User;

import java.util.HashMap;
import java.util.Map;


/**
 * Represents the original data that has been sent from the client to Palisade Service for a request to access data.
 * This data will be forwarded to a set of services with each contributing to the processing of this request.
 * This version represents the response from the Use Service.
 * The next in the sequence will the request for Resource Service.
 * Note there are two class that represent effectively the same data where each has a different purpose.
 * uk.gov.gchq.palisade.service.palisade.response.UserResponse is the client request that came into Palisade Service.
 * uk.gov.gchq.palisade.service.resource.request.ResourceRequest is the input for the Resource Service
 */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public final class UserResponse {

    //want to be @Autowired but has to be static to be used in the default method
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String resourceId;  //Resource that that is being asked to access
    private final JsonNode context;  // represents the context information as a Json string of a Map<String, String>
    public final User user;  //Representation of the User


    @JsonCreator
    private UserResponse(
            final @JsonProperty("resourceId") String resourceId,
            final @JsonProperty("context") JsonNode context,
            final @JsonProperty("user") User user) {

        Assert.notNull(resourceId, "Resource cannot be null");
        Assert.notNull(context, "Context cannot be null");
        Assert.notNull(user, "User cannot be null");

        this.resourceId = resourceId;
        this.context = context;
        this.user = user;

    }

    public String getResourceId() {
        return resourceId;
    }

    public Map<String, String> getContext() throws JsonProcessingException {
        return MAPPER.treeToValue(context, HashMap.class);
    }


    /**
     * Builder class for the creation of instances of the UserResponse.  This is a variant of the Fluent Builder
     * which will build the Java objects from Json string.
     */
    public static class Builder {
        private String resourceId;
        private JsonNode context;
        private User user;


        public static IResource create() {
            return resource -> context -> user ->
                    new UserResponse(resource, context, user);
        }

        interface IResource {
            /**
             * @param resourceId {@link String} is the resource id provided in the register request
             * @return the {@link UserResponse}
             */
            IContext withResource(String resourceId);
        }
    }

    interface IContext {
        /**
         * @param context the context that was passed by the client to the palisade service
         * @return the {@link UserResponse}
         */
        default IUser withContext(Map<String, String> context) {
            return withContextNode(MAPPER.valueToTree(context));
        }

        IUser withContextNode(JsonNode context);

    }


    interface IUser {
        /**
         * @param user the context that was passed by the client to the palisade service
         * @return the {@link UserResponse}
         */
        UserResponse withUser(User user);

    }


}