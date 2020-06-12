package uk.gov.gchq.palisade.service.results.response;


/**
 * The Results Service will  process the data provided by the Query Scope Service by constructing this message that will be
 * provided to the client.  It will contain the necessary information for the client to access the data.
 */
public class ResultsResponse {

    private String token; // unique identifier for this specific request end-to-end.  Was a RequestId object now a String.
    // RequestId  represents the  Token shown in the diagram Logical view of Palisade.
    // This information will also be in the header.  This might be removed later if not required in services.
    //the concept of a unique identifier for each transaction is to pulled from the header

   private String pointer; //  Pointer reference for the accessing the data.
}
