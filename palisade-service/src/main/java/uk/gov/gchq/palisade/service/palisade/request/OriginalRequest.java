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
package uk.gov.gchq.palisade.service.palisade.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.util.Assert;

import uk.gov.gchq.palisade.Generated;


import java.util.Map;


/**
 * Represents the original data that has been sent from the client to Palisade Service for a request to access data.
 * This data will be forwarded to a set of services with each contributing to the processing of this request.
 * This version represents the original request.  Next in the sequence is User Service where this data will be the
 * input (or in other words a request) for a User.
 * Note there are two class that represent effectively the same data where each represents a different stage of the process.
 * uk.gov.gchq.palisade.service.palisade.request.OriginalRequest is the client request that has come into the Palisade Service.
 * uk.gov.gchq.palisade.service.user.request.UserRequest is the input for the User Service
 */

public final class OriginalRequest {

    public final String token; // Unique identifier for this specific request end-to-end
    private final String user;  //Unique identifier for the user
    private final String resource;  //Resource that that is being asked to access
    private final Map<String, String> context; //Relevant context information about the request.

    //?? should be able to set the mapper to include private methods
    // mapper.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);

    @JsonCreator
    private OriginalRequest(@JsonProperty("token") final String token, @JsonProperty("user") final String user, @JsonProperty("resource") final String resource, @JsonProperty("context") final Map<String, String> context) {

        Assert.notNull(token, "Token Id cannot be null");
        Assert.notNull(user, "User cannot be null");
        Assert.notNull(resource, "Resource cannot be null");
        Assert.notNull(context, "Context cannot be null");
        Assert.notEmpty(context, "Context cannot be empty");


        this.token = token;
        this.user = user;
        this.resource = resource;
        this.context = context;
    }

    @Generated
    public String getToken() {
        return token;
    }

    @Generated
    public String getUser() {
        return user;
    }

    @Generated
    public String getResource() {
        return resource;
    }

    @Generated
    public Map<String, String> getContext() {
        return context;
    }

    /**
     * Builder class for the creation of instances of the OriginalRequest.  This is a variant of the Fluent Builder
     * Pattern with the addition of the option for building with either Java objects or JSon strings.
     */
    public static class Builder {
        private String token;
        private String user;
        private String resource;
        private Map<String, String> context;

        public static IToken create() {
            return token -> user -> resource -> context ->
                    new OriginalRequest(token, user, resource, context);
        }


        interface IToken {
            /**
             * @param token {@link String} is the token provided in the original request
             * @return the {@link OriginalRequest}
             */
            IUser withToken(String token);
        }

        interface IUser {
            /**
             * @param user {@link String} is the user id provided in the original request
             * @return the {@link OriginalRequest}
             */
            IResource withUser(String user);


            interface IResource {
                /**
                 * @param resource {@link String} is the resource id provided in the register request
                 * @return the {@link OriginalRequest}
                 */
                IContext withResource(String resource);
            }
        }

        interface IContext {
            /**
             * @param context the context that was passed by the client to the palisade service
             * @return the {@link OriginalRequest}
             */
            OriginalRequest withContext(Map<String, String> context);
        }

    }

}
