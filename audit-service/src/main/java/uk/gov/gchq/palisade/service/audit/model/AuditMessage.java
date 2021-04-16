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
package uk.gov.gchq.palisade.service.audit.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import uk.gov.gchq.palisade.service.audit.common.Context;
import uk.gov.gchq.palisade.service.audit.common.Generated;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;

/**
 * This is the parent class for Audit information. It represents the common component of the data that has been
 * sent from each of the different services.
 */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class AuditMessage {

    @SuppressWarnings("java:S2039") // Suppress the 'Member variable visibility' warning
    static final ObjectMapper MAPPER = new ObjectMapper();

    private final String userId; // Unique identifier for the user.
    private final String resourceId; // Resource that that is being asked to access.
    private final JsonNode context; // Relevant context information about the request.
    private final String serviceName; // service that sent the message
    private final String timestamp; // when the message was created
    private final String serverIP; // the server IP address for the service
    private final String serverHostname; // the hostname of the server hosting the service
    private final JsonNode attributes; // JsonNode holding Map<String, Object> holding optional extra information

    /**
     * Creates and returns a new {@code AuditMessage}
     *
     * @param userId         the user id
     * @param resourceId     the resource id
     * @param context        the context
     * @param serviceName    the service name
     * @param timestamp      the timestamp
     * @param serverIP       the server IP
     * @param serverHostname the server hostname
     * @param attributes     the attributes
     */
    @JsonCreator
    protected AuditMessage(
            final @JsonProperty("userId") String userId,
            final @JsonProperty("resourceId") String resourceId,
            final @JsonProperty("context") JsonNode context,
            final @JsonProperty("serviceName") String serviceName,
            final @JsonProperty("timestamp") String timestamp,
            final @JsonProperty("serverIP") String serverIP,
            final @JsonProperty("serverHostname") String serverHostname,
            final @JsonProperty("attributes") JsonNode attributes) {

        this.userId = Optional.ofNullable(userId).orElseThrow(() -> new IllegalArgumentException("User ID cannot be null"));
        this.resourceId = Optional.ofNullable(resourceId).orElseThrow(() -> new IllegalArgumentException("Resource ID  cannot be null"));
        this.context = Optional.ofNullable(context).orElseThrow(() -> new IllegalArgumentException("Context cannot be null"));
        this.serviceName = Optional.ofNullable(serviceName).orElseThrow(() -> new IllegalArgumentException("Service Name cannot be null"));
        this.timestamp = Optional.ofNullable(timestamp).orElseThrow(() -> new IllegalArgumentException("Timestamp cannot be null"));
        this.serverIP = Optional.ofNullable(serverIP).orElseThrow(() -> new IllegalArgumentException("Server IP address cannot be null"));
        this.serverHostname = Optional.ofNullable(serverHostname).orElseThrow(() -> new IllegalArgumentException("Server Hostname cannot be null"));
        this.attributes = Optional.ofNullable(attributes).orElseThrow(() -> new IllegalArgumentException("Attributes cannot be null"));
    }

    /**
     * Returns the user id
     *
     * @return the user id
     */
    @Generated
    public String getUserId() {
        return userId;
    }

    /**
     * Returns the resource id
     *
     * @return the resource id
     */
    @Generated
    public String getResourceId() {
        return resourceId;
    }

    /**
     * Returns the context
     *
     * @return the context
     * @throws JsonProcessingException if an error occurs when generating the
     *                                 context
     */
    @Generated
    public Context getContext() throws JsonProcessingException {
        return MAPPER.treeToValue(context, Context.class);
    }

    /**
     * Returns the service name
     *
     * @return the service name
     */
    @Generated
    public String getServiceName() {
        return serviceName;
    }

    /**
     * Returns the timestamp
     *
     * @return the timestamp
     */
    @Generated
    public String getTimestamp() {
        return timestamp;
    }

    /**
     * Returns the server IP
     *
     * @return the server IP
     */
    @Generated
    public String getServerIP() {
        return serverIP;
    }

    /**
     * Returns the server host name
     *
     * @return the server host name
     */
    @Generated
    public String getServerHostname() {
        return serverHostname;
    }

    /**
     * Returns the attributes
     *
     * @return the attributes
     * @throws JsonProcessingException if an error occurs when generating the
     *                                 context
     */
    @Generated
    public Map<String, String> getAttributes() throws JsonProcessingException {
        return MAPPER.treeToValue(attributes, Map.class);
    }

    /**
     * Returns the attributes json node
     *
     * @return the attributes json node
     */
    @JsonIgnore
    @Generated
    public JsonNode getAttributesNode() {
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

