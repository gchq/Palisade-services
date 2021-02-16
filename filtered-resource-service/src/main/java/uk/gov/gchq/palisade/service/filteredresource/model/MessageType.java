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

/**
 * Type of message (and thus expected headers/body content) sent by either the client or the server.
 * <p>
 * The client is expected to only send:
 * <ul>
 * <li> {@link MessageType#PING} - is the server alive? reply with a {@link MessageType#PONG}
 * <li> {@link MessageType#CTS} - clear to send next {@link MessageType#RESOURCE}, or {@link MessageType#COMPLETE}
 * </ul>
 * The server is expected to only send:
 * <ul>
 * <li> {@link MessageType#PONG} - the server is alive
 * <li> {@link MessageType#RESOURCE} - the next available resource for the client
 * <li> {@link MessageType#ERROR} - an error occurred while processing the client's request
 * <li> {@link MessageType#COMPLETE} - there is nothing more to return to the client
 * </ul>
 */
public enum MessageType {

    // Client
    PING, // -> PONG
    CTS, // -> RESOURCE|COMPLETE

    // Server
    PONG,
    RESOURCE,
    ERROR,
    COMPLETE
}
