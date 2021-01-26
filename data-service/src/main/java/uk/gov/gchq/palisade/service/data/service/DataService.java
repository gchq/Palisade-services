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

package uk.gov.gchq.palisade.service.data.service;

import uk.gov.gchq.palisade.data.serialise.Serialiser;
import uk.gov.gchq.palisade.reader.common.DataFlavour;
import uk.gov.gchq.palisade.reader.common.DataReader;
import uk.gov.gchq.palisade.service.Service;
import uk.gov.gchq.palisade.service.data.model.DataReaderRequestModel;
import uk.gov.gchq.palisade.service.data.model.DataRequestModel;

import java.io.OutputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The core API for the data service.
 * The responsibility of the data service is to take the read request from the client,
 * request the trusted details about the request from persistence (what policies to apply, user details, etc).
 * The data service then loops over the list of resources passing the list of rules that need to be applied.
 * The {@link DataReader} will then connect to the resource and apply the rules before streaming the data back to
 * the {@link DataService} which forwards the data back to the client.
 */
public interface DataService extends Service {

    /**
     * Request the trusted details about a client's request from persistence (what policies to apply, user details, etc)
     * @param request the client's request for a leaf resource and their unique request token
     * @return asynchronous what rules apply when accessing the data, returned as a {@link DataReaderRequestModel} to pass to the data-reader
     */
    CompletableFuture<DataReaderRequestModel> authoriseRequest(final DataRequestModel request);

    /**
     * Read a resource and write each record to the given {@link OutputStream}.
     *
     * @param request the authorised request from persistence to pass to the data-reader
     * @param out an {@link OutputStream} to write the stream of resources to (after applying rules)
     * @param recordsProcessed number of records that have been processed
     * @param recordsReturned number of records that have been returned
     * @return boolean of true for a successful completion
     */
    CompletableFuture<Boolean> read(final DataReaderRequestModel request, final OutputStream out, final AtomicLong recordsProcessed, final AtomicLong recordsReturned);

    /**
     * Used to add a new serialiser to the data reader
     *
     * @param dataFlavour the {@link DataFlavour} to be added
     * @param serialiser  the {@link Serialiser} to be added
     * @return a {@link Boolean} true/false on success/failure
     */
    Boolean addSerialiser(final DataFlavour dataFlavour, final Serialiser<?> serialiser);

}
