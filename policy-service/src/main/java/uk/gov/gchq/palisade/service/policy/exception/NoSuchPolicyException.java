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
package uk.gov.gchq.palisade.service.policy.exception;

import uk.gov.gchq.palisade.service.policy.common.policy.PolicyService;
import uk.gov.gchq.palisade.service.policy.common.resource.Resource;
import uk.gov.gchq.palisade.service.policy.common.rule.Rule;

/**
 * A {@code NoSuchPolicyException} is a {@link RuntimeException} thrown by a {@link PolicyService} implementation to
 * indicate that the requested {@link Rule} doesnt exist for the requested {@link Resource} doesn't exist,
 * or is not known to that {@code Service} instance.
 */
public class NoSuchPolicyException extends RuntimeException {

    /**
     * Initialises this exception with the given message.
     *
     * @param message message for the exception
     */
    public NoSuchPolicyException(final String message) {
        super(message);
    }

}
