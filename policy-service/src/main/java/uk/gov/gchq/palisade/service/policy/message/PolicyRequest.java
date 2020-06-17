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

package uk.gov.gchq.palisade.service.policy.message;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.Generated;
import uk.gov.gchq.palisade.User;
import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.service.policy.message.PolicyResponse.Builder.IRules;

import java.util.StringJoiner;

public class PolicyRequest {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String context;
    public final String token;
    private final String user;
    private final String resource;

    private PolicyRequest(final String context, final String token, final String user, final String resource) {
        this.context = context;
        this.token = token;
        this.user = user;
        this.resource = resource;
    }

    public Context getContext() throws JsonProcessingException {
        return MAPPER.readValue(this.context, Context.class);
    }

    public User getUser() throws JsonProcessingException {
        return MAPPER.readValue(this.user, User.class);
    }

    public LeafResource getResource() throws JsonProcessingException {
        return MAPPER.readValue(this.resource, LeafResource.class);
    }

    public static class Builder {
        public static IContext create() {
            return context -> token -> user -> resource ->
                    new PolicyRequest(context, token, user, resource);
        }

        public static IRules createResponse(PolicyRequest request) {
            return PolicyResponse.Builder.create()
                    .withSerialisedContext(request.context)
                    .withToken(request.token)
                    .withSerialisedUser(request.user)
                    .withSerialisedResource(request.resource);
        }

        interface IContext {
            default IToken withContext(Context context) throws JsonProcessingException {
                return withSerialisedContext(MAPPER.writeValueAsString(context));
            }

            IToken withSerialisedContext(String context);
        }

        interface IToken {
            IUser withToken(String token);
        }

        interface IUser {
            default IResource withUser(User user) throws JsonProcessingException {
                return withSerialisedUser(MAPPER.writeValueAsString(user));
            }

            IResource withSerialisedUser(String user);
        }

        interface IResource {
            default PolicyRequest withResource(LeafResource resource) throws JsonProcessingException {
                return withSerialisedResource(MAPPER.writeValueAsString(resource));
            }

            PolicyRequest withSerialisedResource(String resource);
        }
    }


    @Override
    @Generated
    public String toString() {
        return new StringJoiner(", ", PolicyRequest.class.getSimpleName() + "[", "]")
                .add("context='" + context + "'")
                .add("token='" + token + "'")
                .add("user='" + user + "'")
                .add("resource='" + resource + "'")
                .add(super.toString())
                .toString();
    }
}
