/*
 * Copyright 2019 Crown Copyright
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
package uk.gov.gchq.palisade.service.palisade.service;

import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.gchq.palisade.service.Service;
import uk.gov.gchq.palisade.service.palisade.request.AuditRequest;
import uk.gov.gchq.palisade.service.palisade.web.AuditClient;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;

public class AuditService implements Service {

    private final AuditClient client;
    private final Executor executor;

    public AuditService(final AuditClient auditClient, final Executor executor) {
        this.client = auditClient;
        this.executor = executor;
    }

    CompletionStage<Boolean> audit(final AuditRequest request) {
        return CompletableFuture.supplyAsync(() -> this.client.audit(request), this.executor);
    }

}
