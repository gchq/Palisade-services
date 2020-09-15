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

package uk.gov.gchq.palisade.service.filteredresource.service;

/**
 * Interface for the topic token offset subsystem.
 * A thread will constantly monitor a kafka queue throughout the lifetime of the application.
 * This queue declares the commit-offsets for the starts of result sets for a given token.
 * When such a message is received, it will be persisted.
 * It will be later retrieved for a client's websocket.
 */
public interface TokenOffsetDaemon {

    void storeTokenOffset(final String token, final Long offset);

}
