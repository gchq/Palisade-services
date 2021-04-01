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

package uk.gov.gchq.palisade.service.user.service;

import uk.gov.gchq.palisade.service.user.common.User;
import uk.gov.gchq.palisade.service.user.common.service.Service;
import uk.gov.gchq.palisade.service.user.exception.NoSuchUserIdException;

/**
 * <p> The core API for the user service. </p> <p> The responsibility of the user service is to maintain the mapping
 * between currently active user IDs, and the users they correspond to. Each user has a given ID which can be added to
 * the user service and retrieved later by the client. </p> <p> <strong>Please note that it is not the responsibility of
 * any {@link UserService} implementation to provide the authentication of individual users, or to maintain a database
 * of 'Palisade' users.</strong> The actual authentication of users should be provided by an external service outside of
 * Palisade. For example, this could be via a centralized PKI service or by a SASL/Kerberos implementation. </p>
 */
public interface UserService extends Service {
    /**
     * Look up a user by their ID. The request contains the {@link String} to lookup from the
     * {@link UserService}. If the requested {@link String} doesn't exist in this {@link
     * UserService} then an exception will be thrown.
     *
     * @param userId the request received by the user-service
     * @return a {@link User} with the user details
     * @throws NoSuchUserIdException if the {@link User} could not be found
     */
    User getUser(final String userId);

    /**
     * Adds the user to the {@link UserService}. The {@link User} should be fully populated with all the necessary
     * attributes about the user such as roles and authorizations.
     *
     * @param user the user with details to add
     * @return the instance of {@link User} which has been added (for caching reasons)
     */
    User addUser(final User user);
}
