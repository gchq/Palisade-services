package uk.gov.gchq.palisade.service.palisade.repository;

import uk.gov.gchq.palisade.service.request.DataRequestConfig;

public interface PersistenceLayer {

    DataRequestConfig get(String requestId);

    void put(DataRequestConfig dataRequestConfig);

}
