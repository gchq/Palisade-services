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
package uk.gov.gchq.palisade.service.palisade.domain;

import uk.gov.gchq.palisade.RequestId;
import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.rule.Rules;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.util.AbstractMap.SimpleImmutableEntry;

@Entity
@Table(name = "leaf_resource_rules",
        uniqueConstraints = {@UniqueConstraint(columnNames = "id")},
        indexes = {@Index(name = "request", columnList = "request_id, id"),
                @Index(name = "resource", columnList = "id, request_id")})
public class LeafResourceRulesEntity {

    @Id
    @Column(name = "id", columnDefinition = "varchar(255)")
    private String id;

    @Column(name = "request_id", columnDefinition = "varchar(255)")
    private String requestId;

    @Column(name = "leaf_resource", columnDefinition = "json")
    @Convert(attributeName = "leafResource", converter = LeafResourceConverter.class)
    private LeafResource leafResource;

    @Column(name = "rules", columnDefinition = "json")
    @Convert(attributeName = "rules", converter = RulesConverter.class)
    private Rules<?> rules;

    public LeafResourceRulesEntity() { }

    public LeafResourceRulesEntity(final RequestId request, final LeafResource leafResource, final Rules<?> rules) {
        this.requestId = request.getId();
        this.id = leafResource.getId();
        this.leafResource = leafResource;
        this.rules = rules;
    }

    public SimpleImmutableEntry<LeafResource, Rules<?>> leafResourceRules() {
        return new SimpleImmutableEntry<>(this.leafResource, this.rules);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public LeafResource getLeafResource() {
        return leafResource;
    }

    public void setLeafResource(LeafResource leafResource) {
        this.leafResource = leafResource;
    }

    public Rules<?> getRules() {
        return rules;
    }

    public void setRules(Rules<?> rules) {
        this.rules = rules;
    }
}
