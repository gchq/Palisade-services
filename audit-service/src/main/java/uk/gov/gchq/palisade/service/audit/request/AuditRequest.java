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

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.gov.gchq.palisade.RequestId;
import uk.gov.gchq.palisade.ToStringBuilder;
import uk.gov.gchq.palisade.service.request.Request;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.ZonedDateTime;
import java.util.Objects;

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
        @JsonSubTypes.Type(value = RegisterRequestCompleteAuditRequest.class),
        @JsonSubTypes.Type(value = RegisterRequestExceptionAuditRequest.class),
        @JsonSubTypes.Type(value = ReadRequestCompleteAuditRequest.class),
        @JsonSubTypes.Type(value = ReadRequestExceptionAuditRequest.class)
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
        final AuditRequest that = (AuditRequest) o;
        return Objects.equals(timestamp, that.timestamp) &&
                Objects.equals(serverIp, that.serverIp) &&
                Objects.equals(serverHostname, that.serverHostname);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), timestamp, serverIp, serverHostname);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .appendSuper(super.toString())
                .append("timestamp", timestamp)
                .append("serverIp", serverIp)
                .append("serverHostname", serverHostname)
                .toString();
    }
}
