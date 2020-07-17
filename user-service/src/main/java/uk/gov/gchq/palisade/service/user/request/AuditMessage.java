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
package uk.gov.gchq.palisade.service.user.request;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.util.Assert;

import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.Generated;
import uk.gov.gchq.palisade.exception.PalisadeRuntimeException;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;


/**
 * This is the parent class for Audit information.  It represents the common component of the data that is to be
 * sent to audit service.
 */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class AuditMessage {

    public static final String SERVICE_NAME = "resource-service";

    protected static final ObjectMapper MAPPER = new ObjectMapper();

    @JsonProperty("userId")
    protected final String userId; //Unique identifier for the user.

    @JsonProperty("resourceId")
    protected final String resourceId;  //Resource that that is being asked to access.

    @JsonProperty("context")
    protected final JsonNode context;   //Relevant context information about the request.

    @JsonProperty("serviceName")
    protected final String serviceName;  //service that sent the message

    @JsonProperty("timestamp")
    protected final String timestamp;  //when the message was created

    @JsonProperty("serverIP")
    protected final String serverIP;  //the server IP address for the service

    @JsonProperty("serverHostname")
    protected final String serverHostname;  //the hostname of the server hosting the service

    @JsonProperty("attributes")
    protected final Map<String, Object> attributes;  //Map<String, Object> holding optional extra information


    @JsonCreator
    protected AuditMessage(

            final String userId,
            final String resourceId,
            final JsonNode context,
            final Map<String, Object> attributes) {

        Assert.notNull(userId, "User cannot be null");
        Assert.notNull(resourceId, "Resource ID  cannot be null");
        Assert.notNull(context, "Context cannot be null");

        this.userId = userId;
        this.resourceId = resourceId;
        this.context = context;

        this.serviceName = SERVICE_NAME;
        this.timestamp = ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);

        try {
            InetAddress inetAddress = InetAddress.getLocalHost();
            this.serverHostname = inetAddress.getHostName();
            this.serverIP = inetAddress.getHostAddress();
        } catch (UnknownHostException e) {
            throw new PalisadeRuntimeException("Failed to get server host and IP address", e);
        }

        this.attributes = attributes;

    }

    @Generated
    public String getUserId() {
        return userId;
    }

    @Generated
    public String getResourceId() {
        return resourceId;
    }

    @Generated
    public Context getContext() throws JsonProcessingException {
        return MAPPER.treeToValue(context, Context.class);
    }

    @Generated
    public String getServiceName() {
        return serviceName;
    }

    @Generated
    public String getTimestamp() {
        return timestamp;
    }

    @Generated
    public String getServerIP() {
        return serverIP;
    }

    @Generated
    public String getServeHostName() {
        return serverHostname;
    }

    @Generated
    public Map getAttributes() {
        return attributes;
    }

    @Override
    @Generated
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AuditMessage)) {
            return false;
        }
        AuditMessage that = (AuditMessage) o;
        return serviceName.equals(that.serviceName) &&
                userId.equals(that.userId) &&
                resourceId.equals(that.resourceId) &&
                context.equals(that.context) &&
                timestamp.equals(that.timestamp) &&
                serverIP.equals(that.serverIP) &&
                serverHostname.equals(that.serverHostname) &&
                attributes.equals(that.attributes);
    }

    @Override
    @Generated
    public int hashCode() {
        return Objects.hash(serviceName, userId, resourceId, context, timestamp, serverIP, serverHostname, attributes);
    }

    @Override
    @Generated
    public String toString() {
        return new StringJoiner(", ", AuditMessage.class.getSimpleName() + "[", "]")
                .add("serviceName='" + serviceName + "'")
                .add("userId='" + userId + "'")
                .add("resourceId='" + resourceId + "'")
                .add("context=" + context)
                .add("timestamp='" + timestamp + "'")
                .add("serverIP='" + serverIP + "'")
                .add("serverHostname='" + serverHostname + "'")
                .add("attributes=" + attributes)
                .add(super.toString())
                .toString();
    }
}

