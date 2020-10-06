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

import java.util.concurrent.CompletableFuture;

/**
 * The {@link FilteredResourceService} listens for incoming websocket connections, then sets up
 * the {@link WebsocketEventService} to handle the rest of the transaction between client and service.
 *
 * Upon receiving a connection for a websocket with a given token, spawn a process to return results
 * for this token, as well as reporting any errors that may have occurred during processing.
 */
public interface FilteredResourceService {

    CompletableFuture<Void> spawnProcessorForToken(final String token);

}
