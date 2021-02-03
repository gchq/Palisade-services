/*
 * Copyright 2018-2021 Crown Copyright
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
package uk.gov.gchq.palisade.service.data.model;

import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.Generated;
import uk.gov.gchq.palisade.User;
import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.rule.Rules;

import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;

/**
 * The AuthorisedData represents reference for the filtered resource that the user is authorised to retrieve.
 * This will be used to provide the link to the resource that will be forwarded to the user to be streamed.
 */
public final class AuthorisedData {

    private final LeafResource resource; //resource that is to be viewed
    private final User user; //user that is requesting the information
    private final Context context;  //context of the request
    private final Rules<?> rules;  //rules that apply to the resource

    private AuthorisedData(
            final LeafResource resource,
            final User user,
            final Context context,
            final Rules<?> rules) {

        this.resource = Optional.ofNullable(resource)
                .orElseThrow(() -> new IllegalArgumentException("resource cannot be null"));
        this.user = Optional.ofNullable(user)
                .orElseThrow(() -> new IllegalArgumentException("user cannot be null"));
        this.context = Optional.ofNullable(context)
                .orElseThrow(() -> new IllegalArgumentException("context cannot be null"));
        this.rules = Optional.ofNullable(rules)
                .orElseThrow(() -> new IllegalArgumentException("rules cannot be null"));
    }

    @Generated
    public LeafResource getResource() {
        return resource;
    }

    @Generated
    public User getUser() {
        return user;
    }

    @Generated
    public Context getContext() {
        return context;
    }

    @Generated
    public Rules<?> getRules() {
        return rules;
    }

    /**
     * Builder class for the creation of instances of the AuthorisedData.
     * This is a variant of the Fluent Builder which will use Java Objects for the components
     * in the build.
     */
    public static class Builder {
        /**
         * Starter method for the Builder class.
         * This method is called to start the process of creating the AuthorisedData class.
         *
         * @return interface {@link IResource} for the next step in the build.
         */
        public static IResource create() {
            return resource -> user -> context -> rules ->
                    new AuthorisedData(resource, user, context, rules);
        }

        /**
         * Adds the resource to the message
         */
        public interface IResource {
            /**
             * Adds the resource to the message
             *
             * @param resource that is being requested
             * @return interface {@link IUser} for the next step in the build.
             */
            IUser withResource(LeafResource resource);
        }

        /**
         * Adds the user requesting the resource
         */
        public interface IUser {
            /**
             * Adds the user requesting the resource
             *
             * @param user the client's unique user
             * @return interface {@link IContext} for the next step in the build.
             */
            IContext withUser(User user);
        }

        /**
         * Adds the context for the request
         */
        public interface IContext {
            /**
             * Adds the context for the request
             *
             * @param context the client's unique token
             * @return interface {@link IRules} for the next step in the build.
             */
            IRules withContext(Context context);
        }

        /**
         * Adds the rules to apply to the resource
         */
        public interface IRules {
            /**
             * Adds the rules to apply to the resource
             *
             * @param rules that are to apply to the resource
             * @return interface {@link AuthorisedData} for the completed class from the builder
             */
            AuthorisedData withRules(Rules<?> rules);

        }
    }

    @Override
    @Generated
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof AuthorisedData)) {
            return false;
        }
        AuthorisedData that = (AuthorisedData) o;
        return resource.equals(that.resource) &&
                user.equals(that.user) &&
                context.equals(that.context) &&
                rules.equals(that.rules);
    }

    @Override
    @Generated
    public int hashCode() {
        return Objects.hash(resource, user, context, rules);
    }

    @Override
    @Generated
    public String toString() {
        return new StringJoiner(", ", AuthorisedData.class.getSimpleName() + "[", "]")
                .add("resource" + resource)
                .add("user=" + user)
                .add("context=" + context)
                .add("rules=" + rules)
                .toString();
    }
}
