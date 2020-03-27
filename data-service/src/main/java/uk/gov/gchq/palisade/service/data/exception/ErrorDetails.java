/*
 * Copyright 2019 Crown Copyright
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

package uk.gov.gchq.palisade.service.data.exception;

import uk.gov.gchq.palisade.Generated;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;

import static java.util.Objects.requireNonNull;

public class ErrorDetails {

    private ZonedDateTime date;
    private String message;
    private Optional<String> details;
    private List<StackTraceElement> stackTrace;

    private ErrorDetails() {
    }

    public ErrorDetails(final ZonedDateTime date, final String message, final String details, final StackTraceElement[] trace) {
        requireNonNull(date, "Date cannot be null");
        requireNonNull(message, "Message cannot be null");
        requireNonNull(trace, "Trace cannot be null");
        this.date = date;
        this.message = message;
        this.details = Optional.ofNullable(details);
        this.stackTrace = Arrays.asList(trace);
    }

    @Generated
    public ZonedDateTime getDate() {
        return date;
    }

    @Generated
    public void setDate(final ZonedDateTime date) {
        requireNonNull(date);
        this.date = date;
    }

    @Generated
    public String getMessage() {
        return message;
    }

    @Generated
    public void setMessage(final String message) {
        requireNonNull(message);
        this.message = message;
    }

    @Generated
    public Optional<String> getDetails() {
        return details;
    }

    @Generated
    public void setDetails(final Optional<String> details) {
        requireNonNull(details);
        this.details = details;
    }

    @Generated
    public List<StackTraceElement> getStackTrace() {
        return stackTrace;
    }

    @Generated
    public void setStackTrace(final List<StackTraceElement> stackTrace) {
        requireNonNull(stackTrace);
        this.stackTrace = stackTrace;
    }

    @Override
    @Generated
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ErrorDetails)) {
            return false;
        }
        final ErrorDetails that = (ErrorDetails) o;
        return Objects.equals(date, that.date) &&
                Objects.equals(message, that.message) &&
                Objects.equals(details, that.details) &&
                Objects.equals(stackTrace, that.stackTrace);
    }

    @Override
    @Generated
    public int hashCode() {
        return Objects.hash(date, message, details, stackTrace);
    }

    @Override
    @Generated
    public String toString() {
        return new StringJoiner(", ", ErrorDetails.class.getSimpleName() + "[", "]")
                .add("date=" + date)
                .add("message='" + message + "'")
                .add("details=" + details)
                .add("stackTrace=" + stackTrace)
                .toString();
    }
}
