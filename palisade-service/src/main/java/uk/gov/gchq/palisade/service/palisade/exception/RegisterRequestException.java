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

package uk.gov.gchq.palisade.service.palisade.exception;

import uk.gov.gchq.palisade.service.palisade.service.PalisadeService;

/**
 * {@link RuntimeException} to be thrown by a {@link PalisadeService} to indicate a failure while processing a request.
 * This failure should be caught and audited with the audit service.
 */
public class RegisterRequestException extends RuntimeException {

    public RegisterRequestException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public RegisterRequestException(final Throwable cause) {
        super(cause);
    }

}
