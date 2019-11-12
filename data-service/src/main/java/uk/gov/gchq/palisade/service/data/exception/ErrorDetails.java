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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static java.util.Objects.requireNonNull;

public class ErrorDetails {

    private Date date;
    private String message;
    private String details;
    private List<StackTraceElement> stackTrace = new ArrayList<>();

    public ErrorDetails() {

    }

    public ErrorDetails(final Date date, final String message, final String details, final StackTraceElement[]  trace) {
        requireNonNull(date, "Date cannot be null");
        requireNonNull(message, "Message cannot be null");
        requireNonNull(details, "Details cannot be null");
        requireNonNull(trace, "Trace cannot be null");
        this.date = date;
        this.message = message;
        this.details = details;
        this.stackTrace = Arrays.asList(trace);
    }

    public void setDate(final Date date) {
        requireNonNull(date, "Date cannot be null");
        this.date = date;
    }

    public void setMessage(final String message) {
        requireNonNull(message, "Message cannot be null");
        this.message = message;
    }

    public void setDetails(final String details) {
        requireNonNull(details, "Details cannot be null");
        this.details = details;
    }

    public void setStackTrace(final List<StackTraceElement> stackTrace) {
        requireNonNull(stackTrace, "Stack Trace cannot be null");
        this.stackTrace = stackTrace;
    }

    public Date getDate() {
        requireNonNull(date, "Date cannot be null");
        return date;
    }

    public String getMessage() {
        requireNonNull(message, "Message cannot be null");
        return message;
    }

    public String getDetails() {
        requireNonNull(details, "Details cannot be null");
        return details;
    }

    public List<StackTraceElement> getStackTrace() {
        requireNonNull(stackTrace, "Stack Trace cannot be null");
        return stackTrace;
    }
}
