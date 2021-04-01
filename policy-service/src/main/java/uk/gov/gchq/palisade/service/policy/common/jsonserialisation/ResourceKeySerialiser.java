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

package uk.gov.gchq.palisade.service.policy.common.jsonserialisation;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import uk.gov.gchq.palisade.service.policy.common.resource.LeafResource;
import uk.gov.gchq.palisade.service.policy.common.resource.Resource;

import java.io.IOException;

class ResourceKeySerialiser extends StdSerializer<Resource> {
    private static final long serialVersionUID = 1L;

    ResourceKeySerialiser() {
        super(Resource.class);
    }

    public static SimpleModule getModule() {
        final SimpleModule module = new SimpleModule();
        module.addKeyDeserializer(LeafResource.class, new ResourceKeyDeserialiser());
        module.addKeySerializer(LeafResource.class, new ResourceKeySerialiser());
        return module;
    }


    @Override
    public void serialize(final Resource value, final JsonGenerator g, final SerializerProvider provider) throws IOException {
        g.writeFieldName(new String(JSONSerialiser.serialise(value)));
    }
}
