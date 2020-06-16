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
package uk.gov.gchq.palisade.service.results.request.common.domain;

/**
 * Contains the relevant information about a resource after the policy have been implemented
 * No information about the connector, resource hierarchy, the schema only show what has not been redacted
 */
public class ResourceMetadata {
    private String schema;  // This will be a redacted version of the schema for this resource
    private String resourceId;   //the unique reference for this resource

}
