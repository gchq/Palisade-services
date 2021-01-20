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
package uk.gov.gchq.palisade.service.data.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.Generated;
import uk.gov.gchq.palisade.exception.PalisadeRuntimeException;


import javax.annotation.Nullable;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * This is the parent class for Audit information.  It represents the common component of the data that is to be
 * sent to audit service.  Note this version of {@code AuditMessage} is unique in comparison to the {@code AuditMessage}
 * from the other service in that it will include the {@code leafResourceId} and {@code token} plus the {@code userID},
 * {@code resourceID} and {@code context} can be null.  This can occur when this message represents an error for a
 * request that is not authorised to access the data.
 */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class AuditMessage {

    public static final String SERVICE_NAME = "data-service";

    protected static final ObjectMapper MAPPER = new ObjectMapper();

    @JsonProperty("leafResourceId")
    private final String leafResourceId;  //leafResource Id requested

    @JsonProperty("token")
    private final String token;  //token for this request

    @JsonProperty("userId")
    protected final @Nullable String userId; //Unique identifier for the user.

    @JsonProperty("resourceId")
    protected final @Nullable String resourceId;  //Resource that that is being asked to access.

    @JsonProperty("context")
    protected final @Nullable
    Context context;   //Relevant context information about the request.

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

    @JsonCreator
    protected AuditMessage(
            final @JsonProperty("leafResourceId") String leafResourceId,
            final @JsonProperty("token") String token,
            final @JsonProperty("userId") @Nullable String userId,
            final @JsonProperty("resourceId") @Nullable String resourceId,
            final @JsonProperty("context") @Nullable Context context,
            final @JsonProperty("attributes") Map<String, Object> attributes) {

        this.leafResourceId = Optional.ofNullable(leafResourceId).orElseThrow(() -> new IllegalArgumentException("leafResourceId" + " cannot be null"));
        this.token = Optional.ofNullable(token).orElseThrow(() -> new IllegalArgumentException("token" + " cannot be null"));

        //these can be null under certain error related conditions
        this.userId = userId;
        this.resourceId = resourceId;
        this.context = context;

        //
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
    public String getLeafResourceId() {
        return leafResourceId;
    }

    @Generated
    public String getToken() {
        return token;
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
    public String getServerHostName() {
        return serverHostname;
    }

    @Generated
    public Map<String, Object> getAttributes() {
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
        return leafResourceId.equals(that.leafResourceId) &&
                token.equals(that.token) &&
                Objects.equals(userId, that.userId) &&
                Objects.equals(resourceId, that.resourceId) &&
                Objects.equals(context, that.context) &&
                serviceName.equals(that.serviceName) &&
                timestamp.equals(that.timestamp) &&
                serverIP.equals(that.serverIP) &&
                serverHostname.equals(that.serverHostname) &&
                attributes.equals(that.attributes);
    }

    @Override
    @Generated
    public int hashCode() {
        return Objects.hash(leafResourceId, token, userId, resourceId, context, serviceName, timestamp, serverIP, serverHostname, attributes);
    }

}

