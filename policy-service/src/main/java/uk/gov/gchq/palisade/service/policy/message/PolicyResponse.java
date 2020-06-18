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
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.Generated;
import uk.gov.gchq.palisade.User;
import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.rule.Rules;

import java.util.Objects;
import java.util.StringJoiner;

@JsonAutoDetect(fieldVisibility = Visibility.ANY)
public class PolicyResponse {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final JsonNode context;
    public final String token;
    private final JsonNode user;
    private final JsonNode resource;
    public final Rules rules;

    @JsonCreator
    private PolicyResponse(
            final @JsonProperty("context") JsonNode context,
            final @JsonProperty("token") String token,
            final @JsonProperty("user") JsonNode user,
            final @JsonProperty("resource") JsonNode resource,
            final @JsonProperty("rules") Rules rules) {
        this.context = context;
        this.token = token;
        this.user = user;
        this.resource = resource;
        this.rules = rules;
    }

    public static class Builder {
        public static IContext create() {
            return context -> token -> user -> resource -> rules ->
                    new PolicyResponse(context, token, user, resource, rules);
        }

        public static IRules create(PolicyRequest request) {
            return PolicyRequest.Builder.createResponse(request);
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
            default IRules withResource(LeafResource resource) {
                return withResourceNode(MAPPER.valueToTree(resource));
            }

            IRules withResourceNode(JsonNode resource);
        }

        interface IRules {
            PolicyResponse withRules(Rules rules);
        }
    }

    @Override
    @Generated
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PolicyResponse)) {
            return false;
        }
        final PolicyResponse response = (PolicyResponse) o;
        return Objects.equals(context, response.context) &&
                Objects.equals(token, response.token) &&
                Objects.equals(user, response.user) &&
                Objects.equals(resource, response.resource) &&
                Objects.equals(rules, response.rules);
    }

    @Override
    @Generated
    public int hashCode() {
        return Objects.hash(context, token, user, resource, rules);
    }

    @Override
    @Generated
    public String toString() {
        return new StringJoiner(", ", PolicyResponse.class.getSimpleName() + "[", "]")
                .add("context='" + context + "'")
                .add("token='" + token + "'")
                .add("user='" + user + "'")
                .add("resource='" + resource + "'")
                .add("rules=" + rules)
                .add(super.toString())
                .toString();
    }
}
