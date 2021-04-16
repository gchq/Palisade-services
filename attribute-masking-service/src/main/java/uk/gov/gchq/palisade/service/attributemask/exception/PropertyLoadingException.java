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

package uk.gov.gchq.palisade.service.attributemask.exception;

import uk.gov.gchq.palisade.service.attributemask.stream.PropertiesConfigurer;

/**
 * Elevate thrown IOExceptions when loading spring/akka config to RuntimeExceptions.
 * Should only be thrown by a {@link PropertiesConfigurer}
 * when loading Spring YAML config and converting to Akka HOCON config
 */
public class PropertyLoadingException extends RuntimeException {

    /**
     * Elevate thrown IOExceptions when loading spring/akka config to RuntimeExceptions.
     *
     * @param message message describing what action caused the exception
     * @param cause   the thrown (non-Runtime) Exception
     */
    public PropertyLoadingException(final String message, final Throwable cause) {
        super(message, cause);
    }

}
