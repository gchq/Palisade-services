<<<<<<< Updated upstream
/*
 * Copyright 2019 Crown Copyright
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
package uk.gov.gchq.palisade.service.policy.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.gov.gchq.palisade.exception.PalisadeRuntimeException;

public class NoPolicyException extends PalisadeRuntimeException {
    private static final Logger LOGGER = LoggerFactory.getLogger(NoPolicyException.class);


    public NoPolicyException(final String e) {
        super(e);
        LOGGER.info("NoPolicyException thrown with message: {}", e);
    }

    public NoPolicyException(final Throwable cause) {
        super(cause);
        LOGGER.info("NoPolicyException thrown with throwable: {}", cause);
    }

    public NoPolicyException(final String e, final Throwable cause) {
        super(e, cause);
        LOGGER.info("NoPolicyException thrown with message {}, and throwable: {}", e, cause);

    }
}
=======
/*
 * Copyright 2019 Crown Copyright
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
package uk.gov.gchq.palisade.service.policy.exception;

import uk.gov.gchq.palisade.exception.PalisadeRuntimeException;

public class NoPolicyException extends PalisadeRuntimeException {

    public NoPolicyException(final String e) {
        super(e);
    }

    public NoPolicyException(final Throwable cause) {
        super(cause);
    }

    public NoPolicyException(final String e, final Throwable cause) {
        super(e, cause);
    }
}
>>>>>>> Stashed changes
