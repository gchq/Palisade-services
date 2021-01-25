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
package uk.gov.gchq.palisade.service.palisade.service;

import akka.stream.Materializer;

import uk.gov.gchq.palisade.service.palisade.model.PalisadeRequest;

import java.util.UUID;

/**
 * A UUID Palisade service which extends {@link PalisadeService}, used to create the uuid token.
 */
public class UUIDPalisadeService extends PalisadeService {

    /**
     * Instantiates a new Palisade service.
     *
     * @param materializer the materializer
     */
    public UUIDPalisadeService(final Materializer materializer) {
        super(materializer);
    }

    @Override
    public String createToken(final PalisadeRequest palisadeRequest) {
        return UUID.randomUUID().toString();
    }
}
