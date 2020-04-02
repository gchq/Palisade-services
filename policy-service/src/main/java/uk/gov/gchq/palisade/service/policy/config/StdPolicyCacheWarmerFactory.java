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

import org.apache.avro.reflect.MapEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;

import uk.gov.gchq.palisade.Generated;
import uk.gov.gchq.palisade.resource.ParentResource;
import uk.gov.gchq.palisade.resource.Resource;
import uk.gov.gchq.palisade.resource.impl.DirectoryResource;
import uk.gov.gchq.palisade.resource.impl.FileResource;
import uk.gov.gchq.palisade.resource.impl.SystemResource;
import uk.gov.gchq.palisade.rule.Rule;
import uk.gov.gchq.palisade.service.PolicyCacheWarmerFactory;
import uk.gov.gchq.palisade.service.UserCacheWarmerFactory;
import uk.gov.gchq.palisade.service.request.Policy;
import uk.gov.gchq.palisade.util.FileUtil;

import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.stream.StreamSupport;

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;

@ConfigurationProperties
public class StdPolicyCacheWarmerFactory implements PolicyCacheWarmerFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(StdPolicyCacheWarmerFactory.class);

    private String type;
    private String resource;
    private String owner;
    private Map<String, String> resourceRules;
    private Map<String, String> recordRules;

    public StdPolicyCacheWarmerFactory() {
    }

    public StdPolicyCacheWarmerFactory(final String type, final String resource, final String owner,
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
    public Entry<Resource, Policy> policyWarm(final List<? extends UserCacheWarmerFactory> users) {
        Policy<?> policy = new Policy<>();
        for (StdUserCacheWarmerFactory user : (List<StdUserCacheWarmerFactory>) users) {
            if (user.getUserId().equals(owner)) {
                policy.owner(user.userWarm());
            }
        }
        for (String key : resourceRules.keySet()) {
            try {
                policy.resourceLevelRule(key, createRule(resourceRules.get(key), "resource"));
            } catch (Exception ex) {
                LOGGER.error("Error creating resourceLevel Rule: {}", ex.getMessage());
            }
        }
        for (String key : recordRules.keySet()) {
            try {
                policy.recordLevelRule(key, createRule(recordRules.get(key), "record"));
            } catch (Exception ex) {
                LOGGER.error("Error creating recordLevel Rule: {}", ex.getMessage());
            }
        }
        return new MapEntry<>(createResource(), policy);
    }

    private <T> Rule<T> createRule(final String rule, final String ruleType) {
        if ("resource".equalsIgnoreCase(ruleType)) {
            try {
                LOGGER.debug("Adding rule {} for rule type {}", rule, ruleType);
                return (Rule<T>) Class.forName(rule).getConstructor().newInstance();
            } catch (ClassNotFoundException | NoSuchMethodException ex) {
                LOGGER.error("Error getting class: {}", ex.getMessage());
            } catch (IllegalAccessException e) {
                LOGGER.error("Error accessing constructor: {}", e.getMessage());
            } catch (InstantiationException e) {
                LOGGER.error("Error instantiating: {}", e.getMessage());
            } catch (InvocationTargetException e) {
                LOGGER.error("Invocation Target Exception: {}", e.getMessage());
            }
        }
        if ("record".equalsIgnoreCase(ruleType)) {
            try {
                LOGGER.debug("Adding rule {} for rule type {}", rule, ruleType);
                return (Rule<T>) Class.forName(rule).getConstructor().newInstance();
            } catch (ClassNotFoundException | NoSuchMethodException ex) {
                LOGGER.error("Error getting class: {}", ex.getMessage());
            } catch (IllegalAccessException e) {
                LOGGER.error("Error accessing constructor: {}", e.getMessage());
            } catch (InstantiationException e) {
                LOGGER.error("Error instantiating: {}", e.getMessage());
            } catch (InvocationTargetException e) {
                LOGGER.error("Invocation Target Exception: {}", e.getMessage());
            }
        }
        return null;
    }

    @Override
    public Resource createResource() {
        URI normalised = FileUtil.convertToFileURI(resource);
        String resource = normalised.toString();
        if (resource.endsWith(".avro")) {
            return new FileResource().id(resource).type(type).serialisedFormat("avro").parent(getParent(resource));
        } else {
            return new DirectoryResource().id(resource).parent(getParent(resource));
        }
    }

    public ParentResource getParent(final String fileURL) {
        URI normalised = FileUtil.convertToFileURI(fileURL);
        //this should only be applied to URLs that start with 'file://' not other types of URL
        if (normalised.getScheme().equals(FileSystems.getDefault().provider().getScheme())) {
            Path current = Paths.get(normalised);
            Path parent = current.getParent();
            //no parent can be found, must already be a directory tree root
            if (isNull(parent)) {
                throw new IllegalArgumentException(fileURL + " is already a directory tree root");
            } else if (isDirectoryRoot(parent)) {
                //else if this is a directory tree root
                return new SystemResource().id(parent.toUri().toString());
            } else {
                //else recurse up a level
                return new DirectoryResource().id(parent.toUri().toString()).parent(getParent(parent.toUri().toString()));
            }
        } else {
            //if this is another scheme then there is no definable parent
            return new SystemResource().id("");
        }
    }

    public boolean isDirectoryRoot(final Path path) {
        return StreamSupport
                .stream(FileSystems.getDefault()
                        .getRootDirectories()
                        .spliterator(), false)
                .anyMatch(path::equals);
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
        return new StringJoiner(", ", StdPolicyCacheWarmerFactory.class.getSimpleName() + "[", "]")
                .add("type='" + type + "'")
                .add("resource='" + resource + "'")
                .add("owner='" + owner + "'")
                .add("resourceRules=" + resourceRules)
                .add("recordRules=" + recordRules)
                .add(super.toString())
                .toString();
    }
}
