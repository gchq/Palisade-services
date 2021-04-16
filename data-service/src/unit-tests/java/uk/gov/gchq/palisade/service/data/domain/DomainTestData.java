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

package uk.gov.gchq.palisade.service.data.domain;

import uk.gov.gchq.palisade.service.data.PassThroughRule;
import uk.gov.gchq.palisade.service.data.common.Context;
import uk.gov.gchq.palisade.service.data.common.resource.LeafResource;
import uk.gov.gchq.palisade.service.data.common.resource.impl.FileResource;
import uk.gov.gchq.palisade.service.data.common.resource.impl.SimpleConnectionDetail;
import uk.gov.gchq.palisade.service.data.common.resource.impl.SystemResource;
import uk.gov.gchq.palisade.service.data.common.rule.Rules;
import uk.gov.gchq.palisade.service.data.common.user.User;

import java.io.Serializable;

class DomainTestData {
    /**
     * Common test data
     */

    private DomainTestData() {
        // hide the constructor, this is just a collection of static objects
    }

    public static final User USER = new User().userId("test-user-id");

    public static final LeafResource LEAF_RESOURCE = new FileResource()
            .id("/test/resourceId")
            .type("uk.gov.gchq.palisade.test.TestType")
            .serialisedFormat("avro")
            .connectionDetail(new SimpleConnectionDetail().serviceName("test-data-service"))
            .parent(new SystemResource().id("/test"));

    public static final Context CONTEXT = new Context().purpose("test-purpose");
    public static final String RULE_MESSAGE = "test-rule";

    public static final Rules<Serializable> RULES = new Rules<>().addRule(RULE_MESSAGE, new PassThroughRule<>());
}
