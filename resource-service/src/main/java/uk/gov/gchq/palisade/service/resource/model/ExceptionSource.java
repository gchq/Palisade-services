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

package uk.gov.gchq.palisade.service.resource.model;

/**
 * An ExceptionSource enum used to add attributes to the {@link AuditErrorMessage}
 * when reporting an error within the Resource Service
 */
public enum ExceptionSource {

    /**
     * If an exception is thrown by the implemented service.
     */
    SERVICE,
    /**
     * If an exception is thrown by the persistence store
     */
    PERSISTENCE,
    /**
     * If an exception is thrown in the request.
     */
    REQUEST;

    public static final String ATTRIBUTE_KEY = "ExceptionSource";

}
