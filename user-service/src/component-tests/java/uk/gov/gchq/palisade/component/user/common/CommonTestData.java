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

package uk.gov.gchq.palisade.component.user.common;

import uk.gov.gchq.palisade.service.user.common.Context;
import uk.gov.gchq.palisade.service.user.common.user.UserId;
import uk.gov.gchq.palisade.service.user.model.UserRequest;

/**
 * Common test data for all classes
 */
public class CommonTestData {

    public static final String REQUEST_TOKEN = "test-request-token";
    public static final UserId USER_ID = new UserId().id("test-user-id");
    public static final String RESOURCE_ID = "/test/resourceId";
    public static final String PURPOSE = "test-purpose";
    public static final Context CONTEXT = new Context().purpose(PURPOSE);

    public static final UserRequest USER_REQUEST = UserRequest.Builder.create()
            .withUserId(USER_ID.getId())
            .withResourceId(RESOURCE_ID)
            .withContext(CONTEXT);
}
