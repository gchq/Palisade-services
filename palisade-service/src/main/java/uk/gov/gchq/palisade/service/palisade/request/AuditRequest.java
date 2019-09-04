/*
 * Copyright 2018 Crown Copyright
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
package uk.gov.gchq.palisade.service.palisade.request;

import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.User;
import uk.gov.gchq.palisade.UserId;
import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.service.request.Request;

import java.util.StringJoiner;

/**
 * This is the abstract class that is passed to the audit-service
 * to be able to store an audit record. The default information is what resources
 * was being accessed.
 */
public class AuditRequest extends Request {

    private AuditRequest() {
        // Forbid construction
    }

    public static class AuditRequestWithContext extends AuditRequest {
        public final Context context;
        public final UserId userId;
        public final String resourceId;

        private AuditRequestWithContext(final Context context, final UserId userId, final String resourceId) {
            this.context = context;
            this.userId = userId;
            this.resourceId = resourceId;
        }

        interface IContext {
            IUserId withContext(final Context context);
        }

        interface IUserId {
            IResourceId withUserId(final UserId userId);
        }

        interface IResourceId {
            AuditRequestWithContext withResourceId(final String resourceId);
        }

        public static IContext create() {
            return context -> userId -> resourceId -> new AuditRequestWithContext(context, userId, resourceId);
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", AuditRequestWithContext.class.getSimpleName() + "[", "]")
                    .add("context=" + context)
                    .add("userId=" + userId)
                    .add("resourceId='" + resourceId + "'")
                    .toString();
        }
    }

    public static class RequestReceivedAuditRequest extends AuditRequestWithContext {

        private RequestReceivedAuditRequest(final Context context, final UserId userId, final String resourceId) {
            super(context, userId, resourceId);
        }

        public static IContext create() {
            return context -> userId -> resourceId -> new RequestReceivedAuditRequest(context, userId, resourceId);
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", RequestReceivedAuditRequest.class.getSimpleName() + "[", "]")
                    .toString();
        }
    }

    public static class ProcessingCompleteAuditRequest extends AuditRequestWithContext {

        private ProcessingCompleteAuditRequest(final Context context, final UserId userId, final String resourceId) {
            super(context, userId, resourceId);
        }

        public static IContext create() {
            return context -> userId -> resourceId -> new ProcessingCompleteAuditRequest(context, userId, resourceId);
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", ProcessingCompleteAuditRequest.class.getSimpleName() + "[", "]")
                    .toString();
        }
    }

    public static class ProcessingStartedAuditRequest extends AuditRequestWithContext {

        public final User user;
        public final LeafResource leafResource;
        public final String howItWasProcessed;

        private ProcessingStartedAuditRequest(final Context context, final UserId userId, final String resourceId, final User user, final LeafResource leafResource, final String howItWasProcessed) {
            super(context, userId, resourceId);
            this.user = user;
            this.leafResource = leafResource;
            this.howItWasProcessed = howItWasProcessed;
        }
    }

}
