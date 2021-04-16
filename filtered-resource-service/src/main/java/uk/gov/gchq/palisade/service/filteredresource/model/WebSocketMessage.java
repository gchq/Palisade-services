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

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.serializer.support.SerializationFailedException;

import uk.gov.gchq.palisade.service.filteredresource.common.Generated;
import uk.gov.gchq.palisade.service.filteredresource.config.ApplicationConfiguration;
import uk.gov.gchq.palisade.service.filteredresource.service.WebSocketEventService;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;

/**
 * WebSocketMessage contains the content of the message sent to the webSocket and is used by {@link WebSocketEventService}
 */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonTypeInfo(use = Id.NONE)
public final class WebSocketMessage {
    private static final ObjectMapper MAPPER = new ApplicationConfiguration().objectMapper();

    private final MessageType type;
    private final Map<String, String> headers;
    private final String body;

    @JsonCreator
    private WebSocketMessage(
            final @JsonProperty("type") MessageType type,
            final @JsonProperty("headers") Map<String, String> headers,
            final @JsonProperty("body") String body) {
        this.type = type;
        this.headers = headers;
        this.body = body;
    }

    /**
     * getType returns the type of Websocket message
     *
     * @return the type of websocket message
     */
    @Generated
    public MessageType getType() {
        return type;
    }

    /**
     * Gets headers of the websocket message.
     *
     * @return the headers of the websocket message
     */
    @Generated
    public Map<String, String> getHeaders() {
        return Optional.ofNullable(headers)
                .orElse(Collections.emptyMap());
    }

    /**
     * Gets the body of the websocket message
     *
     * @return the body of the websocket message
     */
    @Generated
    public String getBody() {
        return body;
    }

    /**
     * Gets the body as an object
     *
     * @param <T>   the type of Websocket Message
     * @param clazz the clazz used in deserializing
     * @return the body object
     */
    @JsonIgnore
    public <T> T getBodyObject(final Class<T> clazz) {
        try {
            return MAPPER.readValue(body, clazz);
        } catch (JsonProcessingException e) {
            throw new SerializationFailedException("Failed to deserialize message body as class " + clazz.getName(), e);
        }
    }

    @Override
    @Generated
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof WebSocketMessage)) {
            return false;
        }
        final WebSocketMessage that = (WebSocketMessage) o;
        return type == that.type &&
                Objects.equals(headers, that.headers) &&
                Objects.equals(body, that.body);
    }

    @Override
    @Generated
    public int hashCode() {
        return Objects.hash(type, headers, body);
    }

    @Override
    @Generated
    public String toString() {
        return new StringJoiner(", ", WebSocketMessage.class.getSimpleName() + "[", "]")
                .add("type=" + type)
                .add("headers=" + headers)
                .add("body=" + body)
                .toString();
    }

    /**
     * Builder class for the creation of instances of the WebSocketMessage.
     * This is a variant of the Fluent Builder which will use Java Objects or JsonNodes equivalents for the components in the build.
     */
    public static class Builder {

        /**
         * Starter method for the Builder class.  This method is called to start the process of creating the
         * WebSocketMessage class.
         *
         * @return public interface {@link IType} for the next step in the build.
         */
        public static IType create() {
            return type -> headers -> body -> new WebSocketMessage(type, headers, body);
        }

        /**
         * Adds the type information to the object.
         */
        public interface IType {
            /**
             * Adds the Type of WebSocketMessage
             *
             * @param type of WebSocketMessage
             * @return interface {@link IHeaders} for the next step in the build.
             */
            IHeaders withType(MessageType type);
        }

        /**
         * Adds the header information to the object.
         */
        public interface IHeaders {
            /**
             * Adds the headers to the WebSocketMessage
             *
             * @param headers for the WebSocketMessage
             * @return interface {@link IBody} for the next step in the build.
             */
            IBody withHeaders(Map<String, String> headers);

            /**
             * Default headers for the Websocket message
             *
             * @param key   the key, most often the token.HEADER
             * @param value the value, most often the token
             * @return interface {@link IBody} for the next step in the build.
             */
            default IHeaders withHeader(String key, String value) {
                return (Map<String, String> partial) -> {
                    Map<String, String> headers = new HashMap<>(partial);
                    headers.put(key, value);
                    return withHeaders(headers);
                };
            }

            /**
             * A Default noHeaders interface that adds an emptyMap of headers to the WebSocketMessage
             *
             * @return interface {@link IBody} for the next step in the build.
             */
            default IBody noHeaders() {
                return withHeaders(Collections.emptyMap());
            }
        }

        /**
         * Adds the body to the object.
         */
        public interface IBody {
            /**
             * Adds a serialisedBody to the WebSocketMessage
             *
             * @param serialisedBody to add to the WebSocketMessage
             * @return class {@link WebSocketMessage} for the completed class from the builder.
             */
            WebSocketMessage withSerialisedBody(String serialisedBody);

            /**
             * Adds an object body to the WebSocketMessage which is then seralised before adding to the class
             *
             * @param body the body
             * @return class {@link WebSocketMessage} for the completed class from the builder.
             */
            default WebSocketMessage withBody(Object body) {
                try {
                    return withSerialisedBody(MAPPER.writeValueAsString(body));
                } catch (JsonProcessingException e) {
                    throw new SerializationFailedException("Failed to serialize message body", e);
                }
            }

            /**
             * An interface used to add a null body to the WebSocketMessage
             *
             * @return class {@link WebSocketMessage} for the completed class from the builder.
             */
            default WebSocketMessage noBody() {
                return withSerialisedBody(null);
            }
        }
    }
}
