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

package uk.gov.gchq.palisade.contract.policy.common;

import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.resource.Resource;
import uk.gov.gchq.palisade.rule.Rule;
import uk.gov.gchq.palisade.user.User;

import java.util.Objects;

public class SerialisedFormatRule implements Rule<Resource> {

    public SerialisedFormatRule() {
    }

    public Resource apply(final Resource resource, final User user, final Context context) {

        Objects.requireNonNull(user);
        Objects.requireNonNull(context);
        String fileId = resource.getId();
        String serialisedFormat = getExtension(fileId);

        if ("txt".equals(serialisedFormat)) {
            return null;
        } else {
            return resource;
        }
    }

    public String getExtension(final String filename) {
        return filename.substring(filename.lastIndexOf(".") + 1);
    }
}
