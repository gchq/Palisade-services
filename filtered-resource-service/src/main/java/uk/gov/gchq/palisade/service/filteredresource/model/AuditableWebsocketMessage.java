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

import akka.japi.Pair;
import akka.kafka.ConsumerMessage.CommittableOffset;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import uk.gov.gchq.palisade.Generated;

import java.util.Optional;

public class AuditableWebsocketMessage {
    private final WebsocketMessage websocketMessage;
    private final Pair<FilteredResourceRequest, CommittableOffset> auditSuccessPair;

    protected AuditableWebsocketMessage(
            final @NonNull WebsocketMessage websocketMessage,
            final @Nullable Pair<FilteredResourceRequest, CommittableOffset> auditSuccessPair
    ) {
        this.websocketMessage = websocketMessage;
        this.auditSuccessPair = auditSuccessPair;
    }

    @Generated
    public WebsocketMessage getWebsocketMessage() {
        return websocketMessage;
    }

    @Generated
    public Optional<Pair<FilteredResourceRequest, CommittableOffset>> getAuditSuccessPair() {
        return Optional.ofNullable(auditSuccessPair);
    }

    public static class Builder {
        public static IWebsocketMessage create() {
            return websocketMessage -> auditableRequest -> new AuditableWebsocketMessage(websocketMessage, auditableRequest);
        }

        public interface IWebsocketMessage {
            IAuditable withWebsocketMessage(@NonNull WebsocketMessage websocketMessage);
        }

        public interface IAuditable {
            AuditableWebsocketMessage withAuditablePair(@Nullable Pair<FilteredResourceRequest, CommittableOffset> auditableRequest);

            default AuditableWebsocketMessage withAuditable(final @NonNull FilteredResourceRequest request, final @NonNull CommittableOffset committableOffset) {
                return withAuditablePair(Pair.create(request, committableOffset));
            }

            default AuditableWebsocketMessage withoutAuditable() {
                return withAuditablePair(null);
            }
        }
    }
}
