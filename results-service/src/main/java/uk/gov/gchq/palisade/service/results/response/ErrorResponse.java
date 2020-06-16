/*
 * Copyright 2020 Crown Copyright
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
package uk.gov.gchq.palisade.service.results.response;

/**
 * Error message in human readable form.  This can be generated in any of the services.  Once an error occurs
 * in a service,  processing of the requests stops.  This messaging is constructed and forwarded to the Results
 * Service skipping any services that have not procesed the request.  Results services will forward this message back
 * to client who should be given enough information to correct the problem before tying again.
 */
public class ErrorResponse {

   // private final String token; // Unique identifier for this specific request end-to-end

  //  private String errorMessage;  //Detailed description of the error in english
}
