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

import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.Generated;
import uk.gov.gchq.palisade.service.user.response.common.domain.User;



/**
 * Represents the original data that has been sent from the client to Palisade Service for a request to access data.
 * This data will be forwarded to a set of services with each contributing to the processing of this request.
 * This version represents the output for user-service where the User has been identified.
 * Next in the sequence will be the request for resource-service.
 * Note there are two classes that effectively represent the same data but represent a different stage of the process.
 * uk.gov.gchq.palisade.service.palisade.response.UserResponse is the response with the data from user-service included.
 * uk.gov.gchq.palisade.service.resource.request.ResourceRequest is the input for the resource-service.
 */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public final class UserResponse {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Resource that that is being asked to access
     */
    private final String resourceId;

    /**
     * Represents the context information as a Json string.
     * {@link java.util.Map} of type {@link String}, {@link String}
     */
    private final JsonNode context;

    /**
     * The user that has made the request
     */
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

    @Generated
    public String getResourceId() {
        return resourceId;
    }

    @Generated
    public Context getContext() throws JsonProcessingException {
        return MAPPER.treeToValue(context, Context.class);
    }


    /**
     * Builder class for the creation of instances of the UserResponse.  This is a variant of the Fluent Builder
     * which will use Java Objects or JsonNodes equivalents for the components in the build.
     */
    public static class Builder {
        private String resourceId;
        private JsonNode context;
        private User user;


        /**
         * Starter method for the Builder class.  This method is called to start the process of creating the
         * UserRequest class.
         *
         * @return interface  {@link IResource} for the next step in the build.
         */
        public static IResource create() {
            return resource -> context -> user ->
                    new UserResponse(resource, context, user);
        }

        /**
         * Adds the resource ID information to the message.
         */
        interface IResource {
            /**
             * Adds the resource ID.
             *
             * @param resourceId resource ID for the request.
             * @return interface {@link IContext} for the next step in the build.
             */
            IContext withResource(String resourceId);
        }

        /**
         * Adds the user context information to the message.
         */
        interface IContext {

            /**
             * Adds the user context information.
             *
             * @param context user context for the request.
             * @return interface {@link IUser} for the next step in the build.
             */
            default IUser withContext(Context context) {
                return withContextNode(MAPPER.valueToTree(context));
            }

            /**
             * Adds the user context information. Uses a JsonNode string form of the information.
             *
             * @param context user context information for the request.
             * @return interface {@link IUser} for the next step in the build.
             */
            IUser withContextNode(JsonNode context);

        }

        /**
         * Adds the user information to the message.
         */
        interface IUser {
            /**
             * Adds the user to this message.
             *
             * @param user for the request.
             * @return interface {@link UserResponse} for the completed class from the builder.
             */
            UserResponse withUser(User user);
        }
    }

}
