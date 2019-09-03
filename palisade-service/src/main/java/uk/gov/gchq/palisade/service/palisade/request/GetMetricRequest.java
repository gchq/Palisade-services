/*
 * Copyright 2018 Crown Copyright
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
package uk.gov.gchq.palisade.service.palisade.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import uk.gov.gchq.palisade.RequestId;
import uk.gov.gchq.palisade.exception.ForbiddenException;
import uk.gov.gchq.palisade.service.palisade.metrics.MetricsProviderUtil;
import uk.gov.gchq.palisade.service.request.Request;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

import static java.util.Objects.requireNonNull;

/**
 * A request sent to retrieve details on the Palisade system itself. The request should be sent along with a list of whitelist
 * filters which specify which metrics to fetch. Filters may use a simple wildcard facility where a single '*' character
 * may be present at either the start OR end of a filter. This filter will then match any metric name that starts or ends
 * with the rest of the filter as appropriate. There may be only one wildcard used per filter and it cannot occur in the middle
 * of a filter.
 * <p>
 * Examples:
 * <p>
 * "hello" matches "hello" alone
 * <p>
 * "hello*" matches "hello.metric1" and "hello.metric2"
 * <p>
 * "*hello" matches "server1.metric.hello" and "server2.metric.hello"
 *
 * @see uk.gov.gchq.palisade.service.palisade.metrics.CommonMetrics
 */
@JsonIgnoreProperties(value = {"originalRequestId"})
public class GetMetricRequest extends Request {
    private final List<String> patternFilter = new ArrayList<>();

    //no-arg constructor required
    public GetMetricRequest() {
    }

    /**
     * Add the given filter to the list of filters.
     *
     * @param filter the filter to add
     * @return this object
     * @throws IllegalArgumentException if the filter is invalid
     */
    public GetMetricRequest addFilter(final String filter) {
        requireNonNull(filter, "filter");
        MetricsProviderUtil.validateFilter(filter);
        patternFilter.add(filter);
        return this;
    }

    /**
     * Add all the strings in the given list to the request filters.
     *
     * @param filters the filters to add
     * @return this object
     * @throws IllegalArgumentException if a filter is invalid
     */
    public GetMetricRequest addFilter(final List<String> filters) {
        requireNonNull(filters, "filters");
        filters.forEach(this::addFilter);
        return this;
    }

    /**
     * Set the filter list to the ones given. The filter list is cleared and the items from the list added.
     *
     * @param filters the replacement filter list
     * @return this object
     * @throws IllegalArgumentException if a filter is invalid
     */
    public GetMetricRequest filters(final List<String> filters) {
        requireNonNull(filters, "filters");
        patternFilter.clear();
        addFilter(filters);
        return this;
    }

    /**
     * Set the filter list to the ones given. The filter list is cleared and the items from the list added.
     *
     * @param filters the replacement filter list
     * @throws IllegalArgumentException if a filter is invalid
     */
    public void setFilters(final List<String> filters) {
        filters(filters);
    }

    /**
     * Gets a copy of the filter list
     *
     * @return the filter list
     */
    public List<String> getFilters() {
        return new ArrayList<>(patternFilter);
    }

    @Override
    public void setOriginalRequestId(final RequestId originalRequestId) {
        throw new ForbiddenException("Should not call GetMetricRequest.setOriginalRequestId()");
    }

    @Override
    public RequestId getOriginalRequestId() {
        throw new ForbiddenException("Should not call GetMetricRequest.getOriginalRequestId()");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GetMetricRequest)) return false;
        if (!super.equals(o)) return false;
        GetMetricRequest that = (GetMetricRequest) o;
        return patternFilter.equals(that.patternFilter);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), patternFilter);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", GetMetricRequest.class.getSimpleName() + "[", "]")
                .add("patternFilter=" + patternFilter)
                .toString();
    }
}
