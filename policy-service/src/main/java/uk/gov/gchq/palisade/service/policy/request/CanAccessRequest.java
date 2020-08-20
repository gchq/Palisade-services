/*
 * Copyright 2018 Crown Copyright
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

import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.Generated;
import uk.gov.gchq.palisade.User;
import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.service.request.Request;

import java.util.Collection;
import java.util.Objects;
import java.util.StringJoiner;

import static java.util.Objects.requireNonNull;

/**
 * This class is used to request whether a user can access a resource for a given context.
 */
public class CanAccessRequest extends Request {
    private User user;
    private Collection<LeafResource> resources;
    private Context context;

    public CanAccessRequest() {
        // no-args constructor needed for serialization only
    }

    /**
     * @param resources the collection of {@link LeafResource}s to be accessed
     * @return the {@link CanAccessRequest}
     */
    @Generated
    public CanAccessRequest resources(final Collection<LeafResource> resources) {
        requireNonNull(resources, "The resources cannot be set to null.");
        this.setResources(resources);
        return this;
    }

    /**
     * @param user the {@link User} wanting access to the resource
     * @return the {@link CanAccessRequest}
     */
    @Generated
    public CanAccessRequest user(final User user) {
        requireNonNull(user, "The user cannot be set to null.");
        this.setUser(user);
        return this;
    }

    /**
     * @param context containing contextual information such as purpose or environmental data that can influence policies
     * @return the {@link CanAccessRequest}
     */
    @Generated
    public CanAccessRequest context(final Context context) {
        requireNonNull(context, "The context cannot be set to null.");
        this.setContext(context);
        return this;
    }

    @Generated
    public User getUser() {
        return user;
    }

    @Generated
    public void setUser(final User user) {
        requireNonNull(user);
        this.user = user;
    }

    @Generated
    public Collection<LeafResource> getResources() {
        return resources;
    }

    @Generated
    public void setResources(final Collection<LeafResource> resources) {
        requireNonNull(resources);
        this.resources = resources;
    }

    @Generated
    public Context getContext() {
        return context;
    }

    @Generated
    public void setContext(final Context context) {
        requireNonNull(context);
        this.context = context;
    }

    @Override
    @Generated
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CanAccessRequest)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        final CanAccessRequest that = (CanAccessRequest) o;
        return Objects.equals(user, that.user) &&
                Objects.equals(resources, that.resources) &&
                Objects.equals(context, that.context);
    }

    @Override
    @Generated
    public int hashCode() {
        return Objects.hash(super.hashCode(), user, resources, context);
    }

    @Override
    @Generated
    public String toString() {
        return new StringJoiner(", ", CanAccessRequest.class.getSimpleName() + "[", "]")
                .add("user=" + user)
                .add("resources=" + resources)
                .add("context=" + context)
                .toString();
    }
}
