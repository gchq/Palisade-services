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

package uk.gov.gchq.palisade.service.palisade.model;

import akka.japi.Pair;

/**
 * A Pair containing the unique token, and the AuditablePalisadeSystemResponse associated with it.
 */
public class TokenRequestPair extends Pair<String, AuditablePalisadeSystemResponse> {
    private static final long serialVersionUID = 1L;

    /**
     * Instantiates a new Token request pair.
     *
     * @param token          the unique token
     * @param systemResponse the original request from the client enriched into a response object to be sent by kafka to the User Service
     */
    public TokenRequestPair(final String token, final AuditablePalisadeSystemResponse systemResponse) {
        super(token, systemResponse);
    }
}
