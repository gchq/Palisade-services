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
package uk.gov.gchq.palisade.service.user.request;


/**
 * Represents the original data that has been sent from the client to Palisade Service for a request to access data.
 * This data will be forwarded to a set of services with each contributing to the processing of this request.
 * The next in the sequence will the response from the User Service which will identify the user associated with
 * the user id given in this original request.
 * Note there are two class that represent effectively the same data where each has a different purpose.
 * uk.gov.gchq.palisade.service.palisade.request.OriginalRequest is the client request that came into Palisade Service.
 * uk.gov.gchq.palisade.service.user.request.UserRequest is the input for the User Service
 *
 */
public final class UserRequest {
    public final String token; // Unique identifier for this specific request end-to-end
    public final String user;  //Unique identifier for the user
    private final String resource;  //Unique identifier for the resource that that is being asked to access
    private final String context;  // represents the context information as a Json string of a Map<String, String>

    private UserRequest(final String token, final String user, final String resource, final String context) {
        this.token = token;
        this.user = user;
        this.resource = resource;
        this.context = context;
    }


    /**
     * Builder class for the creation of instances of the UserRequest.  This is a variant of the Fluent Builder
     * Pattern.
     */
    public static class Builder {

        public static IToken create() {
            return token -> user -> resource -> context ->
                    new UserRequest(token, user, resource, context);
        }

        interface IToken {

            IUser withToken(String token);
        }

        interface IUser {

            IResource withUser(String user);
        }

        interface IResource {

            IContext withResource(String resource);
        }

        interface IContext {

            UserRequest withContext(String context);
        }

    }
}