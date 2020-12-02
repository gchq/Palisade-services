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

package uk.gov.gchq.palisade.service.filteredresource.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.serializer.support.SerializationFailedException;

import uk.gov.gchq.palisade.Generated;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;

public final class WebsocketMessage {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final MessageType type;
    private final Map<String, String> headers;
    private final String body;

    @JsonCreator
    private WebsocketMessage(
            final @JsonProperty("type") MessageType type,
            final @JsonProperty("headers") Map<String, String> headers,
            final @JsonProperty("body") String body) {
        this.type = type;
        this.headers = headers;
        this.body = body;
    }

    @Generated
    public MessageType getType() {
        return type;
    }

    @Generated
    public Map<String, String> getHeaders() {
        return Optional.ofNullable(headers)
                .orElse(Collections.emptyMap());
    }

    @Generated
    public String getBody() {
        return body;
    }

    @JsonIgnore
    public <T> T getBodyObject(final Class<T> clazz) {
        try {
            return MAPPER.readValue(body, clazz);
        } catch (JsonProcessingException e) {
            throw new SerializationFailedException("Failed to deserialize message body as class " + clazz.getName(), e);
        }
    }

    public static class Builder {

        public static IType create() {
            return type -> headers -> body -> new WebsocketMessage(type, headers, body);
        }

        public interface IType {
            IHeaders withType(MessageType type);
        }

        public interface IHeaders {
            IBody withHeaders(Map<String, String> headers);

            default IHeaders withHeader(String key, String value) {
                return partial -> {
                    Map<String, String> headers = new HashMap<>(partial);
                    headers.put(key, value);
                    return withHeaders(headers);
                };
            }

            default IBody noHeaders() {
                return withHeaders(Collections.emptyMap());
            }
        }

        public interface IBody {
            WebsocketMessage withSerialisedBody(String serialisedBody);

            default WebsocketMessage withBody(Object body) {
                try {
                    return withSerialisedBody(MAPPER.writeValueAsString(body));
                } catch (JsonProcessingException e) {
                    throw new SerializationFailedException("Failed to serialize message body", e);
                }
            }

            default WebsocketMessage noBody() {
                return withSerialisedBody(null);
            }
        }
    }

    @Override
    @Generated
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof WebsocketMessage)) {
            return false;
        }
        final WebsocketMessage that = (WebsocketMessage) o;
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
        return new StringJoiner(", ", WebsocketMessage.class.getSimpleName() + "[", "]")
                .add("type=" + type)
                .add("headers=" + headers)
                .add("body=" + body)
                .toString();
    }
}
