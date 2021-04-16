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

package uk.gov.gchq.palisade.service.data.common.data.reader;

import uk.gov.gchq.palisade.service.data.common.Context;
import uk.gov.gchq.palisade.service.data.common.data.DataFlavour;
import uk.gov.gchq.palisade.service.data.common.data.seralise.Serialiser;
import uk.gov.gchq.palisade.service.data.common.rule.Rules;
import uk.gov.gchq.palisade.service.data.common.user.User;
import uk.gov.gchq.palisade.service.data.common.util.RulesUtil;
import uk.gov.gchq.palisade.service.data.exception.NoCapacityException;

import java.util.concurrent.atomic.AtomicLong;

/**
 * The core API for the data reader.
 * <p>
 * The responsibility of the data reader is to connect to the requested resource,
 * apply the rules, then passes back to the data service the stream of data in
 * the expected format.
 * <p>
 * There is a utility method {@link RulesUtil#applyRulesToStream(java.util.stream.Stream, User, Context, Rules, java.util.concurrent.atomic.AtomicLong, java.util.concurrent.atomic.AtomicLong)}
 * that does the part of applying the rules provided your input data is in the
 * format that the rules expect.
 */
public interface DataReader {

    /**
     * This method will read the data from a single resource and apply all the rules.
     * If the data reader is unable to serve the request due to not having the resources
     * to do so, or if it is currently serving too many requests then it may throw a
     * {@link NoCapacityException}.
     *
     * @param request          {@link DataReaderRequest} containing the resource to be
     *                         read, rules to be applied, the user requesting the data
     *                         and the purpose for accessing the data.
     * @param recordsProcessed the number of records processed (for auditing)
     *                         that will be effected by the data reader
     * @param recordsReturned  the number of records returned (for auditing)
     *                         that will be effected by the data reader
     * @return a {@link DataReaderRequest} that contains the stream of data
     * @throws NoCapacityException if the data reader is unable to serve this request due to
     *                             workload issues or lack of capacity
     */
    DataReaderResponse read(final DataReaderRequest request, AtomicLong recordsProcessed, AtomicLong recordsReturned) throws NoCapacityException;

    /**
     * Adds a serialiser with the {@link DataFlavour} and {@link Serialiser} values
     *
     * @param flavour    the {@link DataFlavour} value to be added
     * @param serialiser the {@link Serialiser} value to be added
     */
    void addSerialiser(final DataFlavour flavour, final Serialiser<?> serialiser);

}
