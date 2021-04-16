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

package uk.gov.gchq.palisade.service.data;

import uk.gov.gchq.palisade.service.data.common.Context;
import uk.gov.gchq.palisade.service.data.common.RegisterJsonSubType;
import uk.gov.gchq.palisade.service.data.common.rule.Rule;
import uk.gov.gchq.palisade.service.data.common.user.User;

import java.io.Serializable;

@RegisterJsonSubType(Rule.class)
public class PassThroughRule<T extends Serializable> implements Rule<T> {
    @Override
    public T apply(final T record, final User user, final Context context) {
        return record;
    }

    @Override
    public boolean isApplicable(final User user, final Context context) {
        return false; // rules are not applicable
    }
}
