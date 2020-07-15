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
package uk.gov.gchq.palisade.service.results.request;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.util.Assert;

import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.Generated;

import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;


/**
 * Audit information for a request provided by each service. This is the version that will represent messages
 * that have been received by the Audit Service.
 * Error Audit messages will be sent from every service to both the Results and Audit Service.
 * Successful Audit will be sent from the results-service or the data-service.
 */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class AuditMessage {

    protected static final ObjectMapper MAPPER = new ObjectMapper();


    protected final String serviceName;  //service that sent the message
    protected final String userId; //Unique identifier for the user.
    protected final String resourceId;  //Resource that that is being asked to access.
    protected final JsonNode context;   //Relevant context information about the request.

    protected final String timestamp;  //when the message was created
    protected final String serverIP;  //the server IP address for the service
    protected final String serverHostname;  //the hostname of the server hosting the service

    protected final JsonNode attributes;  //Map<String, Object> holding optional extra information


    @JsonCreator
    protected AuditMessage(

            final  String userId,
            final  String resourceId,
            final  JsonNode context,
            final  String serviceName,
            final  String timestamp,
            final  String serverIP,
            final  String serverHostname,
            final  JsonNode attributes) {

        Assert.notNull(serviceName, "Service Name cannot be null");
        Assert.notNull(userId, "User cannot be null");
        Assert.notNull(resourceId, "Resource ID  cannot be null");
        Assert.notNull(context, "Context cannot be null");
        Assert.notNull(timestamp, "Timestamp cannot be null");
        Assert.notNull(serverIP, "Server IP address cannot be null");
        Assert.notNull(serverHostname, "Server Hostname cannot be null");
        Assert.notNull(attributes, "Attributes cannot be null");

        this.userId = userId;
        this.resourceId = resourceId;
        this.context = context;
        this.serviceName = serviceName;
        this.timestamp = timestamp;
        this.serverIP = serverIP;
        this.serverHostname = serverHostname;
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
    public Map getAttributes() throws JsonProcessingException {
        return MAPPER.treeToValue(attributes, Map.class);
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

