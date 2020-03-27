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
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.Generated;
import uk.gov.gchq.palisade.RequestId;
import uk.gov.gchq.palisade.User;
import uk.gov.gchq.palisade.UserId;
import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.rule.Rules;
import uk.gov.gchq.palisade.service.Service;
import uk.gov.gchq.palisade.service.request.Request;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.ZonedDateTime;
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
        @JsonSubTypes.Type(value = AuditRequest.RegisterRequestExceptionAuditRequest.class),
        @JsonSubTypes.Type(value = AuditRequest.ReadRequestCompleteAuditRequest.class),
        @JsonSubTypes.Type(value = AuditRequest.ReadRequestExceptionAuditRequest.class)
})
public class AuditRequest extends Request {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuditRequest.class);

    public final ZonedDateTime timestamp;
    public final String serverIp;
    public final String serverHostname;

    protected AuditRequest() {
        this.timestamp = null;
        this.serverIp = null;
        this.serverHostname = null;
    }

    protected AuditRequest(final RequestId originalRequestId) {
        super.setOriginalRequestId(requireNonNull(originalRequestId));
        LOGGER.debug("AuditRequest called passing in {}", originalRequestId);
        this.timestamp = ZonedDateTime.now();
        InetAddress inetAddress;
        try {
            inetAddress = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            LOGGER.error("AuditRequest UnknownHostException: {}", e);
            throw new RuntimeException(e);
        }
        serverHostname = inetAddress.getHostName();
        serverIp = inetAddress.getHostAddress();
        LOGGER.debug("AuditRequest instantiated and serverHostname is: {}, and serverIP is {}", serverHostname, serverIp);
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
                .add(super.toString())
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
                    .add(super.toString())
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
                    Objects.equals(serviceClass, that.serviceClass);
        }

        @Override
        @Generated
        public int hashCode() {
            return Objects.hash(super.hashCode(), userId, resourceId, context, exception, serviceClass);
        }

        @Override
        @Generated
        public String toString() {
            return new StringJoiner(", ", RegisterRequestExceptionAuditRequest.class.getSimpleName() + "[", "]")
                    .add("userId=" + userId)
                    .add("resourceId='" + resourceId + "'")
                    .add("context=" + context)
                    .add("exception=" + exception)
                    .add("serviceClass=" + serviceClass)
                    .add(super.toString())
                    .toString();
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
    }

    /**
     * This is one of the objects that is passed to the audit-service to be able to store an audit record. This class extends
     * {@link AuditRequest} This class is used for the indication to the Audit logs that processing has been completed.
     */
    public static final class ReadRequestCompleteAuditRequest extends AuditRequest {

        public final User user;
        public final LeafResource leafResource;
        public final Context context;
        public final Rules rulesApplied;
        public final long numberOfRecordsReturned;
        public final long numberOfRecordsProcessed;

        @JsonCreator
        private ReadRequestCompleteAuditRequest(@JsonProperty("id") final RequestId id, @JsonProperty("originalRequestId") final RequestId originalRequestId, @JsonProperty("user") final User user, @JsonProperty("leafResource") final LeafResource leafResource, @JsonProperty("context") final Context context,
                                                @JsonProperty("rulesApplied") final Rules rulesApplied, @JsonProperty("numberOfRecordsReturned") final long numberOfRecordsReturned, @JsonProperty("numberOfRecordsProcessed") final long numberOfRecordsProcessed) {
            super(originalRequestId);
            this.user = requireNonNull(user);
            this.leafResource = requireNonNull(leafResource);
            this.context = requireNonNull(context);
            this.rulesApplied = requireNonNull(rulesApplied);
            this.numberOfRecordsReturned = numberOfRecordsReturned;
            this.numberOfRecordsProcessed = numberOfRecordsProcessed;
        }

        /**
         * Static factory method.
         *
         * @param original the original request id
         * @return {@link ReadRequestCompleteAuditRequest}
         */
        public static IUser create(final RequestId original) {
            return user -> leafResource -> context -> rulesApplied -> numberOfRecordsReturned -> numberOfRecordsProcessed -> new ReadRequestCompleteAuditRequest(null, original, user, leafResource, context, rulesApplied, numberOfRecordsReturned, numberOfRecordsProcessed);
        }

        @Override
        @Generated
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof ReadRequestCompleteAuditRequest)) {
                return false;
            }
            if (!super.equals(o)) {
                return false;
            }
            final ReadRequestCompleteAuditRequest that = (ReadRequestCompleteAuditRequest) o;
            return numberOfRecordsReturned == that.numberOfRecordsReturned &&
                    numberOfRecordsProcessed == that.numberOfRecordsProcessed &&
                    Objects.equals(user, that.user) &&
                    Objects.equals(leafResource, that.leafResource) &&
                    Objects.equals(context, that.context) &&
                    Objects.equals(rulesApplied, that.rulesApplied);
        }

        @Override
        @Generated
        public int hashCode() {
            return Objects.hash(super.hashCode(), user, leafResource, context, rulesApplied, numberOfRecordsReturned, numberOfRecordsProcessed);
        }

        @Override
        @Generated
        public String toString() {
            return new StringJoiner(", ", ReadRequestCompleteAuditRequest.class.getSimpleName() + "[", "]")
                    .add("user=" + user)
                    .add("leafResource=" + leafResource)
                    .add("context=" + context)
                    .add("rulesApplied=" + rulesApplied)
                    .add("numberOfRecordsReturned=" + numberOfRecordsReturned)
                    .add("numberOfRecordsProcessed=" + numberOfRecordsProcessed)
                    .add(super.toString())
                    .toString();
        }

        public interface IUser {
            /**
             * @param user {@link User} is the user that made the initial registration request to access data
             * @return the {@link ReadRequestCompleteAuditRequest}
             */
            ILeafResource withUser(User user);
        }

        public interface ILeafResource {
            /**
             * @param leafResource the {@link LeafResource} which the data has just finished being read
             * @return the {@link ReadRequestCompleteAuditRequest}
             */
            IContext withLeafResource(LeafResource leafResource);
        }

        public interface IContext {
            /**
             * @param context the context that was passed by the client to the palisade service
             * @return the {@link ReadRequestCompleteAuditRequest}
             */
            IRulesApplied withContext(Context context);
        }

        public interface IRulesApplied {
            /**
             * @param rules {@link Rules} is the rules that are being applied to this resource for this request
             * @return the {@link ReadRequestCompleteAuditRequest}
             */
            INumberOfRecordsReturned withRulesApplied(Rules rules);
        }

        public interface INumberOfRecordsReturned {
            /**
             * @param numberOfRecordsReturned is the number of records that was returned to the user from this resource
             * @return the {@link ReadRequestCompleteAuditRequest}
             */
            INumberOfRecordsProcessed withNumberOfRecordsReturned(long numberOfRecordsReturned);
        }

        public interface INumberOfRecordsProcessed {
            /**
             * @param numberOfRecordsProcessed is the number of records that was processed from this resource
             * @return the {@link ReadRequestCompleteAuditRequest}
             */
            ReadRequestCompleteAuditRequest withNumberOfRecordsProcessed(long numberOfRecordsProcessed);
        }
    }

    /**
     * This is one of the objects that is passed to the audit-service
     * to be able to store an audit record. This class extends {@link Request} This class
     * is used for the indication to the Audit logs that an exception has been received.
     */
    public static final class ReadRequestExceptionAuditRequest extends AuditRequest {

        public final String token;
        public final LeafResource leafResource;
        public final Throwable exception;

        @JsonCreator
        private ReadRequestExceptionAuditRequest(@JsonProperty("id") final RequestId id, @JsonProperty("originalRequestId") final RequestId originalRequestId, @JsonProperty("token") final String token, @JsonProperty("leafResource") final LeafResource leafResource, @JsonProperty("exception") final Throwable exception) {
            super(originalRequestId);
            this.token = requireNonNull(token);
            this.leafResource = requireNonNull(leafResource);
            this.exception = requireNonNull(exception);
        }

        /**
         * Static factory method.
         *
         * @param original request id.
         * @return the {@link ReadRequestExceptionAuditRequest}
         */
        public static IToken create(final RequestId original) {
            return token -> leafResource -> exception -> new ReadRequestExceptionAuditRequest(null, original, token, leafResource, exception);
        }

        @Override
        @Generated
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof ReadRequestExceptionAuditRequest)) {
                return false;
            }
            if (!super.equals(o)) {
                return false;
            }
            final ReadRequestExceptionAuditRequest that = (ReadRequestExceptionAuditRequest) o;
            return Objects.equals(token, that.token) &&
                    Objects.equals(leafResource, that.leafResource) &&
                    Objects.equals(exception, that.exception);
        }

        @Override
        @Generated
        public int hashCode() {
            return Objects.hash(super.hashCode(), token, leafResource, exception);
        }

        @Override
        @Generated
        public String toString() {
            return new StringJoiner(", ", ReadRequestExceptionAuditRequest.class.getSimpleName() + "[", "]")
                    .add("token='" + token + "'")
                    .add("leafResource=" + leafResource)
                    .add("exception=" + exception)
                    .add(super.toString())
                    .toString();
        }

        public interface IToken {
            /**
             * @param token this is the token that is used to retrieve cached information from the palisade service
             * @return the {@link ReadRequestExceptionAuditRequest}
             */
            ILeafResource withToken(String token);
        }

        public interface ILeafResource {
            /**
             * @param leafResource {@link LeafResource} is the leafResource for the ReadRequest
             * @return the {@link ReadRequestExceptionAuditRequest}
             */
            IThrowable withLeafResource(LeafResource leafResource);
        }

        public interface IThrowable {
            /**
             * @param exception {@link Throwable} is the type of the exception while processing
             * @return the {@link ReadRequestExceptionAuditRequest}
             */
            ReadRequestExceptionAuditRequest withException(Throwable exception);
        }
    }
}
