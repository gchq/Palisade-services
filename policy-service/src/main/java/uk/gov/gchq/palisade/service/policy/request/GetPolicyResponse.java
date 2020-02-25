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

package uk.gov.gchq.palisade.service.policy.request;

import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.rule.Rules;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static java.util.Objects.requireNonNull;

/**
 * This class contains the mapping of {@link LeafResource}'s to the applicable {@link Rules}
 */
public class GetPolicyResponse {
    private Map<LeafResource, Rules> recordRules;

    public GetPolicyResponse() {
        recordRules = new HashMap<>();
    }

    public GetPolicyResponse recordRules(final Map<LeafResource, Rules> recordRules) {
        this.setRecordRules(recordRules);
        return this;
    }

    public Map<LeafResource, Rules> getRecordRules() {
        return recordRules;
    }

    public void setRecordRules(final Map<LeafResource, Rules> recordRules) {
        requireNonNull(recordRules, "The record rules cannot be set to null.");
        this.recordRules = recordRules;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof GetPolicyResponse)) {
            return false;
        }
        final GetPolicyResponse that = (GetPolicyResponse) o;
        return Objects.equals(recordRules, that.recordRules);
    }

    @Override
    public int hashCode() {
        return Objects.hash(recordRules);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("GetPolicyResponse{");
        sb.append("recordRules=").append(recordRules);
        sb.append('}');
        return sb.toString();
    }
}
