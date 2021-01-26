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
package uk.gov.gchq.palisade.service.data.model;

import akka.japi.Pair;

public class TokenMessagePair  extends Pair<String, AuditMessage> {
    /**
     * The type Token request pair.
     */

        private static final long serialVersionUID = 1L;

        /**
         * Instantiates a new Token request pair.
         *
         * @param token           the token
         * @param auditMessage the original request
         */
        public TokenMessagePair(final String token, final AuditMessage auditMessage) {
            super(token, auditMessage);
        }
}
