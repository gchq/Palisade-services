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
package uk.gov.gchq.palisade.service.data.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.lang.Nullable;

import uk.gov.gchq.palisade.reader.common.Context;
import uk.gov.gchq.palisade.reader.exception.PalisadeRuntimeException;
import uk.gov.gchq.palisade.service.data.common.Generated;

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
 * sent to Audit Service.  Note this version of {@code AuditMessage} is unique in comparison to the {@code AuditMessage}
 * from the other service in that it will include the {@code leafResourceId}.  In addition the {@code userID},
 * {@code resourceID} and {@code context} can be null.  This can occur when this message represents an error for a
 * request that is not authorised to access the data.
 */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class AuditMessage {

    public static final String SERVICE_NAME = "data-service";

    protected static final ObjectMapper MAPPER = new ObjectMapper();

    @JsonProperty("userId")
    @Nullable
    protected final String userId; // Unique identifier for the user.

    @JsonProperty("resourceId")
    @Nullable
    protected final String resourceId;  //Resource that that is being asked to access.

    @JsonProperty("context")
    @Nullable
    protected final Context context;   //Relevant context information about the request.

    @JsonProperty("serviceName")
    protected String serviceName = SERVICE_NAME;  //service that sent the message

    @JsonProperty("timestamp")
    protected final String timestamp;  //when the message was created

    @JsonProperty("serverIP")
    protected final String serverIP;  //the server IP address for the service

    @JsonProperty("serverHostname")
    protected final String serverHostname;  //the hostname of the server hosting the service

    @JsonProperty("attributes")
    protected final Map<String, Object> attributes;  //Map<String, Object> holding optional extra information

    @JsonProperty("leafResourceId")
    private final String leafResourceId;  //leafResource ID for the resource

    @JsonCreator
    protected AuditMessage(
            @Nullable final @JsonProperty("userId") String userId,
            @Nullable final @JsonProperty("resourceId") String resourceId,
            @Nullable final @JsonProperty("context") Context context,
            final @JsonProperty("attributes") Map<String, Object> attributes,
            final @JsonProperty("leafResourceId") String leafResourceId) {


        //these can be null under certain error related conditions
        this.userId = userId;
        this.resourceId = resourceId;
        this.context = context;

        this.attributes = Optional.ofNullable(attributes).orElseGet(HashMap::new);
        this.leafResourceId = Optional.ofNullable(leafResourceId).orElseThrow(() -> new IllegalArgumentException("leafResourceId" + " cannot be null"));
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
    @Nullable
    public String getUserId() {
        return userId;
    }

    @Generated
    @Nullable
    public String getResourceId() {
        return resourceId;
    }

    @Generated
    @Nullable
    public Context getContext() {
        return context;
    }

    @Generated
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Generated
    public String getLeafResourceId() {
        return leafResourceId;
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
        return leafResourceId.equals(that.leafResourceId) &&
                Objects.equals(userId, that.userId) &&
                Objects.equals(resourceId, that.resourceId) &&
                Objects.equals(context, that.context) &&
                serviceName.equals(that.serviceName) &&
                timestamp.equals(that.timestamp) &&
                serverIP.equals(that.serverIP) &&
                serverHostname.equals(that.serverHostname);
    }

    @Override
    @Generated
    public int hashCode() {
        return Objects.hash(userId, resourceId, context, attributes, leafResourceId, serviceName, timestamp, serverIP, serverHostname);
    }

    @Override
    @Generated
    public String toString() {
        return new StringJoiner(", ", AuditMessage.class.getSimpleName() + "[", "]")
                .add("userId='" + userId + "'")
                .add("resourceId='" + resourceId + "'")
                .add("context=" + context)
                .add("serviceName='" + serviceName + "'")
                .add("timestamp='" + timestamp + "'")
                .add("serverIP='" + serverIP + "'")
                .add("serverHostname='" + serverHostname + "'")
                .add("attributes=" + attributes)
                .add("leafResourceId='" + leafResourceId + "'")
                .toString();
    }
}
