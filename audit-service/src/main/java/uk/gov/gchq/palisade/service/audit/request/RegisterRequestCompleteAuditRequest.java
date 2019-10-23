/*
 * Copyright 2019 Crown Copyright
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
package uk.gov.gchq.palisade.service.audit.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.RequestId;
import uk.gov.gchq.palisade.User;
import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.service.audit.service.AuditService;

import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;

import static java.util.Objects.requireNonNull;

/**
 * This is one of the objects that is passed to the {@link AuditService}
 * to be able to store an audit record. This class extends {@link AuditRequest} This class
 * is used for the indication to the Audit logs that a RegisterDataRequest has been successfully processed and these are
 * the resources that this user is approved to read for this data access request.
 */
public class RegisterRequestCompleteAuditRequest extends AuditRequest {

    public final User user;
    public final Set<LeafResource> leafResources;
    public final Context context;

    @JsonCreator
    private RegisterRequestCompleteAuditRequest(@JsonProperty("id") final RequestId id, @JsonProperty("originalRequestId") final RequestId originalRequestId, @JsonProperty("user") final User user,
                                                @JsonProperty("leafResources") final Set<LeafResource> leafResources, @JsonProperty("context") final Context context) {
        super(originalRequestId);
        this.user = requireNonNull(user);
        this.leafResources = requireNonNull(leafResources);
        this.context = requireNonNull(context);
    }

    /**
     * Static factory method.
     *
     * @param original the originating request Id
     * @return the {@link RegisterRequestCompleteAuditRequest}
     */
    public static IUser create(final RequestId original) {
        return user -> leafResources -> context -> new RegisterRequestCompleteAuditRequest(null, original, user, leafResources, context);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        final RegisterRequestCompleteAuditRequest that = (RegisterRequestCompleteAuditRequest) o;
        return user.equals(that.user) &&
                leafResources.equals(that.leafResources) &&
                context.equals(that.context);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), user, leafResources, context);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", RegisterRequestCompleteAuditRequest.class.getSimpleName() + "[", "]")
                .add(super.toString())
                .add("user=" + user)
                .add("leafResources=" + leafResources)
                .add("context=" + context)
                .toString();
    }

    public interface IUser {
        /**
         * @param user {@link User} is the user that made the initial registration request to access data
         * @return the {@link RegisterRequestCompleteAuditRequest}
         */
        ILeafResources withUser(final User user);
    }

    public interface ILeafResources {
        /**
         * @param leafResources a set of {@link LeafResource} which contains the relevant details about the resource being accessed
         * @return the {@link RegisterRequestCompleteAuditRequest}
         */
        IContext withLeafResources(final Set<LeafResource> leafResources);
    }

    public interface IContext {
        /**
         * @param context the context that was passed by the client to the palisade service
         * @return the {@link RegisterRequestCompleteAuditRequest}
         */
        RegisterRequestCompleteAuditRequest withContext(final Context context);
    }
}
