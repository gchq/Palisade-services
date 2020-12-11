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

import uk.gov.gchq.palisade.service.filteredresource.model.AuditSuccessMessage;
import uk.gov.gchq.palisade.service.filteredresource.model.FilteredResourceRequest;

import java.util.Map;

/**
 * The class AuditEventService used to create {@link AuditSuccessMessage}s that can be sent to the AuditService
 */
public class AuditEventService {
    private final Map<String, String> additionalAttributes;

    /**
     * Instantiates a new Audit event service.
     *
     * @param additionalAttributes any additional attributes that can be added to the {@link AuditSuccessMessage}
     */
    public AuditEventService(final Map<String, String> additionalAttributes) {
        this.additionalAttributes = additionalAttributes;
    }

    /**
     * Convert between {@link FilteredResourceRequest} input messages and {@link AuditSuccessMessage} output messages,
     * using a common {@link Map} of additional attributes for each message.
     *
     * @param request the request to the service to be audited as a successfully-returned resource
     * @return an {@link AuditSuccessMessage} to be written to the "success" kafka topic
     */
    public AuditSuccessMessage createSuccessMessage(final FilteredResourceRequest request) {
        return AuditSuccessMessage.Builder.create(request, this.additionalAttributes);
    }

}
