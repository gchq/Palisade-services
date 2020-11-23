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
package uk.gov.gchq.palisade.service.policy.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class AuditablePolicyResponse {
    @JsonProperty("policyResponse")
    private final PolicyResponse policyResponse;
    @JsonProperty("auditErrorMessage")
    private final AuditErrorMessage auditErrorMessage;

    @JsonCreator
    private AuditablePolicyResponse(
            final @JsonProperty("policyResponse") PolicyResponse policyResponse,
            final @JsonProperty("auditErrorMessage") AuditErrorMessage auditErrorMessage) {
        this.policyResponse = policyResponse;
        this.auditErrorMessage = auditErrorMessage;
    }



}
