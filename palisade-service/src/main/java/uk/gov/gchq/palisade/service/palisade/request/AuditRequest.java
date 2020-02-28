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
import uk.gov.gchq.palisade.RequestId;
import uk.gov.gchq.palisade.User;
import uk.gov.gchq.palisade.UserId;
import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.service.Service;
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
        public String toString() {
            return new StringJoiner(", ", RegisterRequestCompleteAuditRequest.class.getSimpleName() + "[", "]")
                    .add(super.toString())
                    .add("user=" + user)
                    .add("leafResources=" + leafResources)
                    .add("context=" + context)
                    .toString();
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
        public final Class<? extends Service> serviceClass;

        @JsonCreator
        private RegisterRequestExceptionAuditRequest(@JsonProperty("id") final RequestId id, @JsonProperty("originalRequestId") final RequestId originalRequestId, @JsonProperty("userId") final UserId userId, @JsonProperty("resourceId") final String resourceId,
                                                     @JsonProperty("context") final Context context, @JsonProperty("exception") final Throwable exception, @JsonProperty("serviceClass") final Class<? extends Service> serviceClass) {
            super(originalRequestId);
            this.userId = requireNonNull(userId);
            this.resourceId = requireNonNull(resourceId);
            this.context = requireNonNull(context);
            this.exception = requireNonNull(exception);
            this.serviceClass = requireNonNull(serviceClass);
        }

        public interface IUserId {
            /**
             * @param userId {@link UserId} is the user id provided in the register request
             * @return the {@link RegisterRequestExceptionAuditRequest}
             */
            IResourceId withUserId(UserId userId);
        }

        public interface IResourceId {
            /**
             * @param resourceId {@link String} is the resource id provided in the register request
             * @return the {@link RegisterRequestExceptionAuditRequest}
             */
            IContext withResourceId(String resourceId);
        }

        public interface IContext {
            /**
             * @param context the context that was passed by the client to the palisade service
             * @return the {@link RegisterRequestExceptionAuditRequest}
             */
            IException withContext(Context context);
        }

        public interface IException {
            /**
             * @param exception {@link Throwable} is the type of the exception while processing
             * @return the {@link RegisterRequestExceptionAuditRequest}
             */
            IServiceClass withException(Throwable exception);
        }

        public interface IServiceClass {
            /**
             * @param serviceClass {@link Class} is the palisade service that the exception was triggered by.
             * @return the {@link RegisterRequestExceptionAuditRequest}
             */
            RegisterRequestExceptionAuditRequest withServiceClass(Class<? extends Service> serviceClass);
        }

        /**
         * Static factory method.
         *
         * @param original the original request id
         * @return the {@link RegisterRequestExceptionAuditRequest}
         */
        public static IUserId create(final RequestId original) {
            return user -> resourceId -> context -> exception -> serviceClass -> new RegisterRequestExceptionAuditRequest(null, original, user, resourceId, context, exception, serviceClass);
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", RegisterRequestExceptionAuditRequest.class.getSimpleName() + "[", "]")
                    .add(super.toString())
                    .add("userId=" + userId)
                    .add("resourceId='" + resourceId + "'")
                    .add("context=" + context)
                    .add("exception=" + exception)
                    .add("serviceClass=" + serviceClass)
                    .toString();
        }
    }

    @Override
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
        AuditRequest that = (AuditRequest) o;
        return timestamp.equals(that.timestamp) &&
                serverIp.equals(that.serverIp) &&
                serverHostname.equals(that.serverHostname);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), timestamp, serverIp, serverHostname);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", AuditRequest.class.getSimpleName() + "[", "]")
                .add(super.toString())
                .add("timestamp=" + timestamp)
                .add("serverIp='" + serverIp + "'")
                .add("serverHostname='" + serverHostname + "'")
                .toString();
    }
}
