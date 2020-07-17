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
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.Generated;
import uk.gov.gchq.palisade.RequestId;
import uk.gov.gchq.palisade.User;
import uk.gov.gchq.palisade.UserId;
import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.service.request.Request;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;

import static java.util.Objects.requireNonNull;

/**
 * This is the abstract class that is passed to the audit-service
 * to be able to store an audit record. The default information is
 * when was the audit record created and by what server.
 * <p>
 * The four immutable data subclasses below can be instantiated by static
 * {@code create(RequestId orig)} factory methods which chain construction by fluid interface definitions.
 */

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "class"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = AuditRequest.RegisterRequestCompleteAuditRequest.class),
        @JsonSubTypes.Type(value = AuditRequest.RegisterRequestExceptionAuditRequest.class)
})
public class AuditRequest extends Request {

    public final Date timestamp;
    public final String serverIp;
    public final String serverHostname;

    private AuditRequest() {
        this.timestamp = null;
        this.serverIp = null;
        this.serverHostname = null;
    }

    private AuditRequest(final RequestId originalRequestId) {
        super.setOriginalRequestId(requireNonNull(originalRequestId));

        this.timestamp = new Date();
        InetAddress inetAddress;
        try {
            inetAddress = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
        serverHostname = inetAddress.getHostName();
        serverIp = inetAddress.getHostAddress();
    }

    @Override
    @Generated
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AuditRequest)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        final AuditRequest that = (AuditRequest) o;
        return Objects.equals(timestamp, that.timestamp) &&
                Objects.equals(serverIp, that.serverIp) &&
                Objects.equals(serverHostname, that.serverHostname);
    }

    @Override
    @Generated
    public int hashCode() {
        return Objects.hash(super.hashCode(), timestamp, serverIp, serverHostname);
    }

    @Override
    @Generated
    public String toString() {
        return new StringJoiner(", ", AuditRequest.class.getSimpleName() + "[", "]")
                .add("timestamp=" + timestamp)
                .add("serverIp='" + serverIp + "'")
                .add("serverHostname='" + serverHostname + "'")
                .toString();
    }

    /**
     * This is one of the objects that is passed to the audit-service to be able to store an audit record. This class extends
     * {@link AuditRequest}. This class is used to indicate to the Audit logs that a RegisterDataRequest has been successfully
     * processed and these are the resources that this user is approved to read for this data access request.
     */
    public static final class RegisterRequestCompleteAuditRequest extends AuditRequest {

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
        @Generated
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof RegisterRequestCompleteAuditRequest)) {
                return false;
            }
            if (!super.equals(o)) {
                return false;
            }
            final RegisterRequestCompleteAuditRequest that = (RegisterRequestCompleteAuditRequest) o;
            return Objects.equals(user, that.user) &&
                    Objects.equals(leafResources, that.leafResources) &&
                    Objects.equals(context, that.context);
        }

        @Override
        @Generated
        public int hashCode() {
            return Objects.hash(super.hashCode(), user, leafResources, context);
        }

        @Override
        @Generated
        public String toString() {
            return new StringJoiner(", ", RegisterRequestCompleteAuditRequest.class.getSimpleName() + "[", "]")
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
            ILeafResources withUser(User user);
        }

        public interface ILeafResources {
            /**
             * @param leafResources a set of {@link LeafResource} which contains the relevant details about the resource being accessed
             * @return the {@link RegisterRequestCompleteAuditRequest}
             */
            IContext withLeafResources(Set<LeafResource> leafResources);
        }

        public interface IContext {
            /**
             * @param context the context that was passed by the client to the palisade service
             * @return the {@link RegisterRequestCompleteAuditRequest}
             */
            RegisterRequestCompleteAuditRequest withContext(Context context);
        }
    }

    /**
     * This is one of the objects that is passed to the audit-service
     * to be able to store an audit record. This class extends {@link AuditRequest} This class
     * is used for the indication to the Audit logs that an exception has been received while processing the RegisterDataRequest
     * and which service it was that triggered the exception.
     */
    public static final class RegisterRequestExceptionAuditRequest extends AuditRequest {

        public final UserId userId;
        public final String resourceId;
        public final Context context;
        public final Throwable exception;

        /**
         * The name of the service which caused the exception
         */
        public final String serviceName;

        @JsonCreator
        private RegisterRequestExceptionAuditRequest(
                @JsonProperty("id")                final RequestId id,
                @JsonProperty("originalRequestId") final RequestId originalRequestId,
                @JsonProperty("userId")            final UserId userId,
                @JsonProperty("resourceId")        final String resourceId,
                @JsonProperty("context")           final Context context,
                @JsonProperty("exception")         final Throwable exception,
                @JsonProperty("serviceName")       final String serviceName) {
            super(originalRequestId);
            this.userId = requireNonNull(userId);
            this.resourceId = requireNonNull(resourceId);
            this.context = requireNonNull(context);
            this.exception = requireNonNull(exception);
            this.serviceName = requireNonNull(serviceName);
        }

        /**
         * Static factory method.
         *
         * @param original the original request id
         * @return the {@link RegisterRequestExceptionAuditRequest}
         */
        public static IUserId create(final RequestId original) {
            return user -> resourceId -> context -> exception -> serviceName -> new RegisterRequestExceptionAuditRequest(null, original, user, resourceId, context, exception, serviceName);
        }

        @Override
        @Generated
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof RegisterRequestExceptionAuditRequest)) {
                return false;
            }
            if (!super.equals(o)) {
                return false;
            }
            final RegisterRequestExceptionAuditRequest that = (RegisterRequestExceptionAuditRequest) o;
            return Objects.equals(userId, that.userId) &&
                    Objects.equals(resourceId, that.resourceId) &&
                    Objects.equals(context, that.context) &&
                    Objects.equals(exception, that.exception) &&
                    Objects.equals(serviceName, that.serviceName);
        }

        @Override
        @Generated
        public int hashCode() {
            return Objects.hash(super.hashCode(), userId, resourceId, context, exception, serviceName);
        }

        @Override
        @Generated
        public String toString() {
            return new StringJoiner(", ", RegisterRequestExceptionAuditRequest.class.getSimpleName() + "[", "]")
                    .add("userId=" + userId)
                    .add("resourceId='" + resourceId + "'")
                    .add("context=" + context)
                    .add("exception=" + exception)
                    .add("serviceName=" + serviceName)
                    .toString();
        }

        /**
         * A fluent builder interface used to set the {@link IUserId}
         */
        public interface IUserId {
            /**
             * Sets the user id with the provided value
             * @param userId {@link UserId} is the user id provided in the register request
             * @return the {@link RegisterRequestExceptionAuditRequest}
             */
            IResourceId withUserId(UserId userId);
        }

        /**
         * A fluent builder interface used to set the resource id
         */
        public interface IResourceId {
            /**
             * Sets the resource id with the provided value
             * @param resourceId {@link String} is the resource id provided in the register request
             * @return the {@link IContext}
             */
            IContext withResourceId(String resourceId);
        }

        /**
         * A fluent builder interface used to set the context
         */
        public interface IContext {
            /**
             * Sets the context id with the provided value
             * @param context the context that was passed by the client to the palisade service
             * @return the {@link IException}
             */
            IException withContext(Context context);
        }

        /**
         * A fluent builder interface used to set the exception
         */
        public interface IException {
            /**
             * Sets the exception with the provided value
             * @param exception {@link Throwable} is the type of the exception while processing
             * @return the {@link IServiceName}
             */
            IServiceName withException(Throwable exception);
        }

        /**
         * A fluent builder interface used to set the service name
         */
        public interface IServiceName {
            /**
             * Sets the service name with the provided value
             * @param serviceName {@link String} is the name of the palisade service that the exception was triggered by.
             * @return the {@link RegisterRequestExceptionAuditRequest}
             */
            RegisterRequestExceptionAuditRequest withServiceName(String serviceName);
        }
    }
}
