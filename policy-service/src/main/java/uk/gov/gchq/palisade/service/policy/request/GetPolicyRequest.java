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
import uk.gov.gchq.palisade.ToStringBuilder;
import uk.gov.gchq.palisade.User;
import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.service.request.Request;

import java.util.Collection;
import java.util.Objects;

import static java.util.Objects.requireNonNull;

/**
 * This class is used in the request to get the policies that apply to the
 * collection of {@link LeafResource}'s.
 */
public class GetPolicyRequest extends Request {
    private User user;
    private Context context;
    private Collection<LeafResource> resources;

    public GetPolicyRequest() {
        // no-args constructor needed for serialization only
    }

    /**
     * @param user the {@link User} wanting access to the resource
     * @return the {@link GetPolicyRequest}
     */
    public GetPolicyRequest user(final User user) {
        requireNonNull(user, "The user cannot be set to null.");
        this.user = user;
        return this;
    }

    /**
     * @param resources a collection of {@link LeafResource}'s to be accessed
     * @return the {@link GetPolicyRequest}
     */
    public GetPolicyRequest resources(final Collection<LeafResource> resources) {
        requireNonNull(resources, "The resources cannot be set to null.");
        this.resources = resources;
        return this;
    }

    public User getUser() {
        requireNonNull(user, "The user has not been set.");
        return user;
    }

    public void setUser(final User user) {
        user(user);
    }

    public Context getContext() {
        requireNonNull(context, "The context has not been set.");
        return context;
    }

    public GetPolicyRequest context(final Context context) {
        requireNonNull(context, "The context cannot be set to null.");
        this.context = context;
        return this;
    }

    public Collection<LeafResource> getResources() {
        requireNonNull(resources, "The resources have not been set.");
        return resources;
    }

    public void setResources(final Collection<LeafResource> resources) {
        resources(resources);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof GetPolicyRequest)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        final GetPolicyRequest that = (GetPolicyRequest) o;
        return Objects.equals(user, that.user) &&
                Objects.equals(context, that.context) &&
                Objects.equals(resources, that.resources);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), user, context, resources);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .appendSuper(super.toString())
                .append("user", user)
                .append("context", context)
                .append("resources", resources)
                .toString();
    }
}
