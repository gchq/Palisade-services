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

package uk.gov.gchq.palisade.service.audit.service;

/**
 * Enum providing type safety for service names
 *
 * @since 0.5.0
 */
public enum ServiceName {

    /**
     * Name of the Attribute Masking Service
     */
    ATTRIBUTE_MASKING_SERVICE("attribute-masking-service"),

    /**
     * Name of the Data Service
     */
    DATA_SERVICE("data-service"),

    /**
     * Name of the Filtered Resource Service
     */
    FILTERED_RESOURCE_SERVICE("filtered-resource-service"),

    /**
     * Name of the Palisade Service
     */
    PALISADE_SERVICE("palisade-service"),

    /**
     * Name of the Policy Service
     */
    POLICY_SERVICE("policy-service"),

    /**
     * Name of the Resource Service
     */
    RESOURCE_SERVICE("resource-service"),

    /**
     * name of the Topic Offset Service
     */
    TOPIC_OFFSET_SERVICE("topic-offset-service"),

    /**
     * Name of the User Service
     */
    USER_SERVICE("user-service");

    /**
     * The service name value used by the {@link uk.gov.gchq.palisade.service.audit.model.AuditMessage} types
     */
    public final String value;

    ServiceName(final String value) {
        this.value = value;
    }

}
