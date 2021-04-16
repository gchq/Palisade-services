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

package uk.gov.gchq.palisade.service.audit.common.audit;

import uk.gov.gchq.palisade.service.audit.model.AuditMessage;

/**
 * The core API for the Audit Service. This service is responsible for logging audit messages, whether that is locally
 * or to a centralised repository. Implementations of the Audit Service may include proxies to forward the messages to
 * another Audit Service, aggregator's to reduce the volumes of logging to be stored as well as implementations that
 * actually write the logs to storage. By splitting the functionality of the audit components in this way, where they
 * all implement this interface but do some small processing before passing to the next component, for example proxy -
 * receiver - aggregator - storage. It means that if we don't want to aggregate audit records then we just remove the
 * aggregator implementation when building that micro-service.
 */
public interface AuditService {

    /**
     * This method applies the functionality that the implementation of the Audit Service needs to apply, whether that
     * is to forward to request somewhere else, put the request into cache, so it can be aggregated with other requests,
     * or to write it to storage.
     *
     * @param token   the token value for the request that is passing the message
     * @param request an {@link AuditMessage} object that contains the details required to create an audit log.
     * @return a {@link Boolean} value indicating the success of this audit request
     */
    Boolean audit(final String token, final AuditMessage request);

}
