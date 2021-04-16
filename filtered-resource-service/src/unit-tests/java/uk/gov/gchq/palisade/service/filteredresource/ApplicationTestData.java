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

package uk.gov.gchq.palisade.service.filteredresource;

import uk.gov.gchq.palisade.service.filteredresource.common.Context;
import uk.gov.gchq.palisade.service.filteredresource.common.resource.LeafResource;
import uk.gov.gchq.palisade.service.filteredresource.common.resource.impl.FileResource;
import uk.gov.gchq.palisade.service.filteredresource.common.resource.impl.SimpleConnectionDetail;
import uk.gov.gchq.palisade.service.filteredresource.common.resource.impl.SystemResource;
import uk.gov.gchq.palisade.service.filteredresource.model.FilteredResourceRequest;
import uk.gov.gchq.palisade.service.filteredresource.model.MessageType;
import uk.gov.gchq.palisade.service.filteredresource.model.TopicOffsetMessage;
import uk.gov.gchq.palisade.service.filteredresource.model.WebSocketMessage;

public final class ApplicationTestData {
    /**
     * Common test data for all classes
     */

    private ApplicationTestData() {
        // hide the constructor, this is just a collection of static objects
    }

    public static final String REQUEST_TOKEN = "test-request-token";
    public static final LeafResource LEAF_RESOURCE = new FileResource()
            .id("/test/resourceId")
            .type("uk.gov.gchq.palisade.test.TestType")
            .serialisedFormat("avro")
            .connectionDetail(new SimpleConnectionDetail().serviceName("test-data-service"))
            .parent(new SystemResource().id("/test"));

    public static final FilteredResourceRequest REQUEST = FilteredResourceRequest.Builder.create()
            .withUserId("test-user-id")
            .withResourceId("/test/resourceId")
            .withContext(new Context().purpose("test-purpose"))
            .withResource(LEAF_RESOURCE);

    public static final Long OFFSET = 1L;
    public static final TopicOffsetMessage OFFSET_MESSAGE = TopicOffsetMessage.Builder.create()
            .withCommitOffset(OFFSET);

    public static final WebSocketMessage WS_MESSAGE = WebSocketMessage.Builder.create()
            .withType(MessageType.RESOURCE)
            .noHeaders()
            .withBody(LEAF_RESOURCE);

}
