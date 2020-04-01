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

package uk.gov.gchq.palisade.service.policy.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;

import uk.gov.gchq.palisade.Generated;
import uk.gov.gchq.palisade.resource.Resource;
import uk.gov.gchq.palisade.rule.Rule;
import uk.gov.gchq.palisade.service.PolicyCacheWarmerFactory;
import uk.gov.gchq.palisade.service.UserCacheWarmerFactory;
import uk.gov.gchq.palisade.service.request.Policy;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;

import static java.util.Objects.requireNonNull;

@ConfigurationProperties
public class StdPolicyCacheWarmerFactory implements PolicyCacheWarmerFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(StdPolicyCacheWarmerFactory.class);

    private String type;
    private String owner;
    private Map<String, String> resourceRules;
    private Map<String, String> recordRules;

    public StdPolicyCacheWarmerFactory() {
    }

    public StdPolicyCacheWarmerFactory(final String type, final String owner, final Map<String, String> resourceRules, final Map<String, String> recordRules) {
        this.type = type;
        this.owner = owner;
        this.resourceRules = resourceRules;
        this.recordRules = recordRules;
    }

    @Generated
    public String getType() {
        return type;
    }

    @Generated
    public void setType(final String type) {
        requireNonNull(type);
        this.type = type;
    }

    @Generated
    public String getOwner() {
        return owner;
    }

    @Generated
    public void setOwner(final String owner) {
        requireNonNull(owner);
        this.owner = owner;
    }

    @Generated
    public Map<String, String> getResourceRules() {
        return resourceRules;
    }

    @Generated
    public void setResourceRules(final Map<String, String> resourceRules) {
        requireNonNull(resourceRules);
        this.resourceRules = resourceRules;
    }

    @Generated
    public Map<String, String> getRecordRules() {
        return recordRules;
    }

    @Generated
    public void setRecordRules(final Map<String, String> recordRules) {
        requireNonNull(recordRules);
        this.recordRules = recordRules;
    }


    @Override
    public Policy policyWarm(final List<? extends UserCacheWarmerFactory> users) {
        Policy<?> policy = new Policy<>();
        for (StdUserCacheWarmerFactory user : (List<StdUserCacheWarmerFactory>) users) {
            if (user.getUserId().equals(owner)) {
                policy.owner(user.userWarm());
            }
        }
        for (String key : resourceRules.keySet()) {
            try {
                policy.resourceLevelRule(key, (Rule<Resource>) createRule(resourceRules.get(key), "resource"));
            } catch (Exception ex) {
                LOGGER.error("Error creating resourceLevel Rule: {}", ex.getMessage());
            }
        }
        return policy;
    }

    private Rule<?> createRule(final String rule, final String ruleType) throws Exception {
        if ("resource".equals(ruleType)) {
            return (Rule<Resource>) Class.forName(rule).getConstructor().newInstance();
        } else {
            return null;
        }
    }

    @Override
    @Generated
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof StdPolicyCacheWarmerFactory)) {
            return false;
        }
        final StdPolicyCacheWarmerFactory that = (StdPolicyCacheWarmerFactory) o;
        return Objects.equals(type, that.type) &&
                Objects.equals(owner, that.owner) &&
                Objects.equals(resourceRules, that.resourceRules) &&
                Objects.equals(recordRules, that.recordRules);
    }

    @Override
    @Generated
    public int hashCode() {
        return Objects.hash(type, owner, resourceRules, recordRules);
    }

    @Override
    @Generated
    public String toString() {
        return new StringJoiner(", ", StdPolicyCacheWarmerFactory.class.getSimpleName() + "[", "]")
                .add("type='" + type + "'")
                .add("owner='" + owner + "'")
                .add("resourceRules=" + resourceRules)
                .add("recordRules=" + recordRules)
                .add(super.toString())
                .toString();
    }
}
