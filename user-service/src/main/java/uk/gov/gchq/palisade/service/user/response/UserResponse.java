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

import org.springframework.util.Assert;

import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.Generated;
import uk.gov.gchq.palisade.RequestId;
import uk.gov.gchq.palisade.UserId;
import uk.gov.gchq.palisade.service.user.response.common.domain.User;

import java.util.Objects;
import java.util.StringJoiner;


/**
 * Represents the original data that has been sent from the client to Palisade Service for a request to access data.
 * This data will be forwarded to a set of services with each contributing to the processing of this request.
 * This version represents the response from the Use Service.
 * The next in the sequence will the request for Resource Service.
 * Note there are two class that represent effectively the same data where each has a different purpose.
 * uk.gov.gchq.palisade.service.palisade.response.UserResponse is the client request that came into Palisade Service.
 * uk.gov.gchq.palisade.service.resource.request.ResourceRequest is the input for the Resource Service
 */
public final class  UserResponse {

    private final String token; // Unique identifier for this specific request end-to-end
    private final User user;  //Representation of the User
    private final String resource;  //Resource that that is being asked to access
    private final String context;  // represents the context information as a Json string of a Map<String, String>


    private UserResponse(final String token, final User user, final String resource, final String contextJson) {
        this.token = token;
        this.user = user;
        this.resource = resource;
        this.context = contextJson;
    }




    /**
     * Builder class for the creation of instances of the UserResponse.  This is a variant of the Fluent Builder
     * Pattern with the addition of the option for building with either Java objects or JSon strings.
     */
    public static class Builder implements IToken, IUser, IResource, IContext {
        private String token;
        private User user;
        private String resourceId;
        private String context;


        public static UserResponse create() {
           // Assert.notNull(token, "Token Id cannot be null");
           // Assert.notNull(user, "User cannot be null");
           // Assert.notNull(resourceId, "Resource Id cannot be null");
           // Assert.notNull(context, "Context  cannot be null");

     //    return token -> user -> resourceId -> context -> exception -> serviceClass -> new UserResponse(token, user, resourceId, context) ;

           // return new UserResponse(token, user, resourceId, context);
            return null;
        }

        @Override
        public IUser withToken(final String token) {
            return this;
        }

        @Override
        public IResource withUser(final User user) {
            return this;
        }

        @Override
        public IContext withResourceId(final String resourceId) {
            return this;
        }

        @Override
        public Builder withContext(final String context) {
            return this;
        }
    }

    public interface IToken {
        /**
         * @param token {@link String} is the user id provided in the request
         * @return the {@link UserResponse}
         */
        IUser withToken(String token);
    }

    public interface IUser {
        /**
         * @param user {@link User} is the user id provided in the request
         * @return the {@link UserResponse}
         */
        IResource withUser(User user);
    }

    public interface IResource {
        /**
         * @param resourceId {@link String} is the resource id provided in the request
         * @return the {@link UserResponse}
         */
        IContext withResourceId(String resourceId);
    }

    public interface IContext {
        /**
         * @param context {@link String} is the context as a JSon string
         * @return the {@link UserResponse}
         */
        Builder withContext(String context);
    }
}