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

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.Generated;
import uk.gov.gchq.palisade.User;
import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.service.policy.message.PolicyResponse.Builder.IRules;

import java.util.Objects;
import java.util.StringJoiner;

@JsonFormat(shape= JsonFormat.Shape.ARRAY)
@JsonPropertyOrder(alphabetic=true)
@JsonAutoDetect(fieldVisibility = Visibility.ANY)
public class PolicyRequest {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final JsonNode context;
    public final String token;
    private final JsonNode user;
    private final JsonNode resource;

    @JsonCreator
    private PolicyRequest(
            final @JsonProperty("context") JsonNode context,
            final @JsonProperty("token") String token,
            final @JsonProperty("user") JsonNode user,
            final @JsonProperty("resource") JsonNode resource) {
        this.context = context;
        this.token = token;
        this.user = user;
        this.resource = resource;
    }

    public Context getContext() throws JsonProcessingException {
        return MAPPER.treeToValue(this.context, Context.class);
    }

    public User getUser() throws JsonProcessingException {
        return MAPPER.treeToValue(this.user, User.class);
    }

    public LeafResource getResource() throws JsonProcessingException {
        return MAPPER.treeToValue(this.resource, LeafResource.class);
    }

    public static class Builder {
        public static IContext create() {
            return context -> token -> user -> resource ->
                    new PolicyRequest(context, token, user, resource);
        }

        public static IRules createResponse(PolicyRequest request) {
            return PolicyResponse.Builder.create()
                    .withContextNode(request.context)
                    .withToken(request.token)
                    .withUserNode(request.user)
                    .withResourceNode(request.resource);
        }

        interface IContext {
            default IToken withContext(Context context) {
                return withContextNode(MAPPER.valueToTree(context));
            }

            IToken withContextNode(JsonNode context);
        }

        interface IToken {
            IUser withToken(String token);
        }

        interface IUser {
            default IResource withUser(User user) {
                return withUserNode(MAPPER.valueToTree(user));
            }

            IResource withUserNode(JsonNode user);
        }

        interface IResource {
            default PolicyRequest withResource(LeafResource resource) {
                return withResourceNode(MAPPER.valueToTree(resource));
            }

            PolicyRequest withResourceNode(JsonNode resource);
        }
    }

    @Override
    @Generated
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PolicyRequest)) {
            return false;
        }
        final PolicyRequest that = (PolicyRequest) o;
        return Objects.equals(context, that.context) &&
                Objects.equals(token, that.token) &&
                Objects.equals(user, that.user) &&
                Objects.equals(resource, that.resource);
    }

    @Override
    @Generated
    public int hashCode() {
        return Objects.hash(context, token, user, resource);
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
