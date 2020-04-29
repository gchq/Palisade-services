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

import uk.gov.gchq.palisade.Generated;
import uk.gov.gchq.palisade.resource.Resource;
import uk.gov.gchq.palisade.resource.impl.FileResource;
import uk.gov.gchq.palisade.rule.Rule;
import uk.gov.gchq.palisade.service.PolicyPrepopulationFactory;
import uk.gov.gchq.palisade.service.UserPrepopulationFactory;
import uk.gov.gchq.palisade.service.request.Policy;
import uk.gov.gchq.palisade.util.ResourceBuilder;

import java.io.File;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.StringJoiner;

import static java.util.Objects.requireNonNull;

public class StdPolicyPrepopulationFactory implements PolicyPrepopulationFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(StdPolicyPrepopulationFactory.class);

    private String type;
    private String resource;
    private String owner;
    private Map<String, String> resourceRules;
    private Map<String, String> recordRules;

    public StdPolicyPrepopulationFactory() {
    }

    public StdPolicyPrepopulationFactory(final String type, final String resource, final String owner,
                                         final Map<String, String> resourceRules, final Map<String, String> recordRules) {
        this.type = type;
        this.resource = resource;
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
    public String getResource() {
        return resource;
    }

    @Generated
    public void setResource(final String resource) {
        requireNonNull(resource);
        this.resource = resource;
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
    public Entry<Resource, Policy> build(final List<? extends UserPrepopulationFactory> users) {
        Policy<?> policy = new Policy<>();
        for (StdUserPrepopulationFactory user : (List<StdUserPrepopulationFactory>) users) {
            if (user.getUserId().equals(owner)) {
                policy.setOwner(user.build());
            }
        }
        for (Entry<String, String> entry : resourceRules.entrySet()) {
            policy.resourceLevelRule(entry.getKey(), createRule(entry.getValue(), "resource"));
        }
        for (Entry<String, String> entry : recordRules.entrySet()) {
            policy.recordLevelRule(entry.getKey(), createRule(entry.getValue(), "record"));
        }
        return new SimpleImmutableEntry<>(createResource(), policy);
    }

    private <T> Rule<T> createRule(final String rule, final String ruleType) {
        if ("resource".equalsIgnoreCase(ruleType)) {
            try {
                LOGGER.debug("Adding rule {} for rule type {}", rule, ruleType);
                return (Rule<T>) Class.forName(rule).getConstructor().newInstance();
            } catch (Exception ex) {
                LOGGER.error("Error creating resourceLevel rule: {} - {}", ex.getMessage(), ex.getCause());
            }
        }
        if ("record".equalsIgnoreCase(ruleType)) {
            try {
                LOGGER.debug("Adding rule {} for rule type {}", rule, ruleType);
                return (Rule<T>) Class.forName(rule).getConstructor().newInstance();
            } catch (Exception ex) {
                LOGGER.error("Error creating recordLevel rule: {} - {}", ex.getMessage(), ex.getCause());
            }
        }
        return null;
    }

    @Override
    public Resource createResource() {
        File resourceFile = new File(resource);
        if (new File(resource).isFile()) {
            return ((FileResource) ResourceBuilder.create(resourceFile.toURI())).type(type).serialisedFormat("avro");
        } else {
            return ResourceBuilder.create(resourceFile.toURI());
        }
    }

    @Override
    @Generated
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof StdPolicyPrepopulationFactory)) {
            return false;
        }
        final StdPolicyPrepopulationFactory that = (StdPolicyPrepopulationFactory) o;
        return Objects.equals(type, that.type) &&
                Objects.equals(resource, that.resource) &&
                Objects.equals(owner, that.owner) &&
                Objects.equals(resourceRules, that.resourceRules) &&
                Objects.equals(recordRules, that.recordRules);
    }

    @Override
    @Generated
    public int hashCode() {
        return Objects.hash(type, resource, owner, resourceRules, recordRules);
    }

    @Override
    @Generated
    public String toString() {
        return new StringJoiner(", ", StdPolicyPrepopulationFactory.class.getSimpleName() + "[", "]")
                .add("type='" + type + "'")
                .add("resource='" + resource + "'")
                .add("owner='" + owner + "'")
                .add("resourceRules=" + resourceRules)
                .add("recordRules=" + recordRules)
                .add(super.toString())
                .toString();
    }
}
