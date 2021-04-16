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
package uk.gov.gchq.palisade.service.filteredresource.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.serializer.support.SerializationFailedException;

import uk.gov.gchq.palisade.service.filteredresource.common.Context;
import uk.gov.gchq.palisade.service.filteredresource.common.Generated;
import uk.gov.gchq.palisade.service.filteredresource.config.ApplicationConfiguration;
import uk.gov.gchq.palisade.service.filteredresource.exception.PalisadeRuntimeException;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;

/**
 * This is the parent class for Audit information.  It represents the common component of the data that is to be
 * sent to audit service.
 */
public class AuditMessage {

    public static final String SERVICE_NAME = "filtered-resource-service";

    protected static final ObjectMapper MAPPER = new ApplicationConfiguration().objectMapper();

    @JsonProperty("userId")
    protected final String userId; //Unique identifier for the user.

    @JsonProperty("resourceId")
    protected final String resourceId;  //Resource that that is being asked to access.

    @JsonProperty("context")
    protected final JsonNode context;   //Relevant context information about the request.

    @JsonProperty("serviceName")
    protected String serviceName = SERVICE_NAME;  //service that sent the message

    @JsonProperty("timestamp")
    protected final String timestamp;  //when the message was created

    @JsonProperty("serverIP")
    protected final String serverIP;  //the server IP address for the service

    @JsonProperty("serverHostname")
    protected final String serverHostname;  //the hostname of the server hosting the service

    @JsonProperty("attributes")
    protected final Map<String, String> attributes;  //Map<String, Object> holding optional extra information

    @JsonCreator
    protected AuditMessage(
            final @JsonProperty("userId") String userId,
            final @JsonProperty("resourceId") String resourceId,
            final @JsonProperty("context") JsonNode context,
            final @JsonProperty("attributes") Map<String, String> attributes) {

        this.userId = Optional.ofNullable(userId).orElseThrow(() -> new IllegalArgumentException("User ID cannot be null"));
        this.resourceId = Optional.ofNullable(resourceId).orElseThrow(() -> new IllegalArgumentException("Resource ID  cannot be null"));
        this.context = Optional.ofNullable(context).orElseThrow(() -> new IllegalArgumentException("Context cannot be null"));
        this.attributes = Optional.ofNullable(attributes).orElseGet(HashMap::new);

        this.timestamp = ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);

        try {
            InetAddress inetAddress = InetAddress.getLocalHost();
            this.serverHostname = inetAddress.getHostName();
            this.serverIP = inetAddress.getHostAddress();
        } catch (UnknownHostException e) {
            throw new PalisadeRuntimeException("Failed to get server host and IP address", e);
        }

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
    public Context getContext() {
        try {
            return MAPPER.treeToValue(this.context, Context.class);
        } catch (JsonProcessingException e) {
            throw new SerializationFailedException("Failed to get Context", e);
        }
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
    public String getServerHostname() {
        return serverHostname;
    }

    @Generated
    public Map<String, String> getAttributes() {
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
