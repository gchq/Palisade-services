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
package uk.gov.gchq.palisade.service.resource.response.common;

import java.net.URL;
import java.util.SortedSet;

/**
 * Contains the relevant information about a resource.
 */
public class ResourceMetadata {
    private String schema;  // The schema for this resource
    private String resourceId;   //the unique reference for this resource
    URL urlDataService;  //replaces the ConnectionDetail this specifies the connection for the resource.
    SortedSet<String> parent;  //contains the hierarchy of resources relevant to the this resource





}
