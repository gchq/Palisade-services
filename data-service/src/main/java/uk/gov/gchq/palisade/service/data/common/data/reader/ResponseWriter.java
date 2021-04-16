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

import java.io.IOException;
import java.io.OutputStream;

/**
 * This interface is used by DataService implementations to write the Palisade redacted data to be written to an {@link OutputStream}.
 */
public interface ResponseWriter extends AutoCloseable {

    /**
     * Instruct the {@link DataReader} that created this object to begin the process
     * of reading the data from the backing store, performing any necessary rules applications to it and then writing
     * it to the {@link OutputStream} provided. This is a serial operation that will block the current thread until writing
     * is complete. The provided stream will NOT be closed once the data has been written. It is the responsibility of the code
     * that created {@code output} to close it.
     * <p>
     * Once a data stream has been written to, then further attempts to call this method may result in an exception. Implementations
     * are NOT required to support multiple writes.
     *
     * @param output the stream to write to
     * @return this object
     * @throws IOException if the data could not be written
     */
    ResponseWriter write(final OutputStream output) throws IOException;

}
