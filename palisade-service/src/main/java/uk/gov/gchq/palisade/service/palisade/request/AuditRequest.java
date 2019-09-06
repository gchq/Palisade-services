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
package uk.gov.gchq.palisade.service.palisade.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.RequestId;
import uk.gov.gchq.palisade.User;
import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.rule.Rules;

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
 */

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "class"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = AuditRequest.RegisterRequestCompleteAuditRequest.class)
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

    private AuditRequest(final RequestId id, final RequestId originalRequestId) {
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

    public static class RegisterRequestCompleteAuditRequest extends AuditRequest {

        public final User user;
        public final Set<LeafResource> leafResources;
        public final Context context;

        @JsonCreator
        private RegisterRequestCompleteAuditRequest (@JsonProperty("id") final RequestId id, @JsonProperty("originalRequestId") final RequestId originalRequestId, @JsonProperty("user") final User user,
                                                     @JsonProperty("leafResources") final Set<LeafResource> leafResources, @JsonProperty("context") final Context context) {
            super(id, originalRequestId);
            this.user = requireNonNull(user);
            this.leafResources = requireNonNull(leafResources);
            this.context = requireNonNull(context);
        }

        interface IUser {
            ILeafResources withUser(final User user);
        }

        interface ILeafResources {
            IContext withLeafResources(final Set<LeafResource> leafResources);
        }

        interface IContext {
            RegisterRequestCompleteAuditRequest withContext(final Context context);
        }

        public static IUser create(final RequestId request, final RequestId original) {
            return user -> leafResources -> context -> new RegisterRequestCompleteAuditRequest(request, original, user, leafResources, context);
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

    public static class ReadRequestCompleteAuditRequest extends AuditRequest {

        public final User user;
        public final LeafResource leafResource;
        public final Context context;
        public final Rules rulesApplied;
        public final long numberOfRecordsReturned;
        public final long numberOfRecordsProcessed;

        @JsonCreator
        private ReadRequestCompleteAuditRequest (@JsonProperty("id") final RequestId id, @JsonProperty("originalRequestId") final RequestId originalRequestId, @JsonProperty("user") final User user, @JsonProperty("leafResource") final LeafResource leafResource, @JsonProperty("context") final Context context,
                                                 @JsonProperty("rulesApplied") final Rules rulesApplied, @JsonProperty("numberOfRecordsReturned") final long numberOfRecordsReturned, @JsonProperty("numberOfRecordsProcessed") final long numberOfRecordsProcessed) {
            super(id, originalRequestId);
            this.user = requireNonNull(user);
            this.leafResource = requireNonNull(leafResource);
            this.context = requireNonNull(context);
            this.rulesApplied = requireNonNull(rulesApplied);
            this.numberOfRecordsReturned = requireNonNull(numberOfRecordsReturned);
            this.numberOfRecordsProcessed = requireNonNull(numberOfRecordsProcessed);
        }

        interface IUser {
            ILeafResource withUser(final User user);
        }

        interface ILeafResource {
            IContext withLeafResource(final LeafResource leafResource);
        }

        interface IContext {
            IRulesApplied withContext(final Context context);
        }

        interface IRulesApplied {
            INumberOfRecordsReturned withRulesApplied(final Rules rules);
        }

        interface INumberOfRecordsReturned {
            INumberOfRecordsProcessed withNumberOfRecordsReturned(final long numberOfRecordsReturned);
        }

        interface INumberOfRecordsProcessed {
            ReadRequestCompleteAuditRequest withNumberOfRecordsProcessed(final long numberOfRecordsProcessed);
        }

        public static IUser create(final RequestId request, final RequestId original) {
            return user -> leafResource -> context -> rulesApplied -> numberOfRecordsReturned -> numberOfRecordsProcessed -> new ReadRequestCompleteAuditRequest(request, original, user, leafResource, context, rulesApplied, numberOfRecordsReturned, numberOfRecordsProcessed);
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", ReadRequestCompleteAuditRequest.class.getSimpleName() + "[", "]")
                    .add(super.toString())
                    .add("user=" + user)
                    .add("leafResource=" + leafResource)
                    .add("context=" + context)
                    .add("rulesApplied=" + rulesApplied)
                    .add("numberOfRecordsReturned=" + numberOfRecordsReturned)
                    .add("numberOfRecordsProcessed=" + numberOfRecordsProcessed)
                    .toString();
        }
    }

    public static class ReadRequestExceptionAuditRequest extends AuditRequest {

        public final String token;
        public final LeafResource leafResource;
        public final Throwable exception;

        @JsonCreator
        private ReadRequestExceptionAuditRequest(@JsonProperty("id") final RequestId id, @JsonProperty("originalRequestId") final RequestId originalRequestId, @JsonProperty("token") final String token, @JsonProperty("leafResource") final LeafResource leafResource, @JsonProperty("exception") final Throwable exception) {
            super(id, originalRequestId);
            this.token = requireNonNull(token);
            this.leafResource = requireNonNull(leafResource);
            this.exception = requireNonNull(exception);
        }

        interface IToken {
            ILeafResource withToken(final String token);
        }

        interface ILeafResource {
            IThrowable withLeafResource(final LeafResource leafResource);
        }

        interface IThrowable {
            ReadRequestExceptionAuditRequest withException(final Throwable exception);
        }

        public static IToken create(final RequestId request, final RequestId original) {
            return token -> leafResource -> exception -> new ReadRequestExceptionAuditRequest(request, original, token, leafResource, exception);
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", ReadRequestExceptionAuditRequest.class.getSimpleName() + "[", "]")
                    .add(super.toString())
                    .add("token='" + token + "'")
                    .add("leafResource=" + leafResource)
                    .add("exception=" + exception)
                    .toString();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AuditRequest)) return false;
        if (!super.equals(o)) return false;
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
