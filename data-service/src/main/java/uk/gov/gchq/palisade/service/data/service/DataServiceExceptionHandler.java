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

package uk.gov.gchq.palisade.service.data.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import uk.gov.gchq.palisade.service.data.exception.ErrorDetails;
import uk.gov.gchq.palisade.service.data.exception.NoPolicyException;
import uk.gov.gchq.palisade.service.data.web.DataController;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.ZonedDateTime;

@ControllerAdvice(assignableTypes = DataController.class)
@RequestMapping(produces = "application/json")
public class DataServiceExceptionHandler extends ResponseEntityExceptionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataServiceExceptionHandler.class);
    private static final String MESSAGE = "The application encountered an issue while processing the request.";

    /**
     * Provides handling for errors encountered by the data-service.
     * @param ex the target exception
     * @param request the current request
     * @return a {@code ResponseEntity} instance
     */
    @ExceptionHandler({
            NoPolicyException.class,
            NullPointerException.class,
            FileNotFoundException.class,
            IOException.class,
            RuntimeException.class,
            Exception.class
    })
    public ResponseEntity<Object> handleCustomException(final Exception ex, final WebRequest request) {
        LOGGER.warn("Delegating exception handling for {} from request {}", ex, request);

        if (ex instanceof NoPolicyException) {
            return noPolicyExceptionHandler((NoPolicyException) ex, request);
        } else if (ex instanceof NullPointerException) {
            return nullPointerExceptionHandler((NullPointerException) ex, request);
        } else if (ex instanceof FileNotFoundException) {
            return fileNotFoundExceptionHandler((FileNotFoundException) ex, request);
        } else if (ex instanceof IOException) {
            return ioExceptionHandler((IOException) ex, request);
        } else if (ex instanceof RuntimeException) {
            return runtimeExceptionHandler((RuntimeException) ex, request);
        } else {
            return globalExceptionHandler(ex, request);
        }
    }

    /**
     * This method customizes the response for a NoPolicyException error.
     * @param ex the exception
     * @param request the current request
     * @return a {@code ResponseEntity} instance
     */
    private ResponseEntity<Object> noPolicyExceptionHandler(final NoPolicyException ex, final WebRequest request) {
        ErrorDetails details = new ErrorDetails(ZonedDateTime.now(), MESSAGE, ex.getMessage(), ex.getStackTrace());

        LOGGER.error("Error from data service, details: {}", details);
        return new ResponseEntity<>(details, HttpStatus.NOT_FOUND);
    }

    /**
     * This method customizes the response for a NullPointerException error.
     * @param ex the exception
     * @param request the current request
     * @return a {@code ResponseEntity} instance
     */
    private ResponseEntity<Object> nullPointerExceptionHandler(final NullPointerException ex, final WebRequest request) {
        ErrorDetails details = new ErrorDetails(ZonedDateTime.now(), MESSAGE, ex.getMessage(), ex.getStackTrace());

        LOGGER.error("Error from data service, details: {}", details);
        return new ResponseEntity<>(details, HttpStatus.BAD_REQUEST);
    }

    /**
     * This method customizes the response for a FileNotFoundException error.
     * @param ex the exception
     * @param request the current request
     * @return a {@code ResponseEntity} instance
     */
    private ResponseEntity<Object> fileNotFoundExceptionHandler(final FileNotFoundException ex, final WebRequest request) {
        ErrorDetails details = new ErrorDetails(ZonedDateTime.now(), MESSAGE, ex.getMessage(), ex.getStackTrace());

        LOGGER.error("Error from data service, details: {}", details);
        return new ResponseEntity<>(details, HttpStatus.NOT_FOUND);
    }

    /**
     * This method customizes the response for an IOException error.
     * @param ex the exception
     * @param request the current request
     * @return a {@code ResponseEntity} instance
     */
    private ResponseEntity<Object> ioExceptionHandler(final IOException ex, final WebRequest request) {
        ErrorDetails details = new ErrorDetails(ZonedDateTime.now(), MESSAGE, ex.getMessage(), ex.getStackTrace());

        LOGGER.error("Error from data service, details: {}", details);
        return new ResponseEntity<>(details, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * This method customizes the response for a RuntimeException error.
     * @param ex the exception
     * @param request the current request
     * @return a {@code ResponseEntity} instance
     */
    private ResponseEntity<Object> runtimeExceptionHandler(final RuntimeException ex, final WebRequest request) {
        ErrorDetails details = new ErrorDetails(ZonedDateTime.now(), MESSAGE, ex.getMessage(), ex.getStackTrace());

        LOGGER.error("Error from data service, details: {}", details);
        return new ResponseEntity<>(details, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * This method customizes the response for a general Exception error.
     * @param ex the exception
     * @param request the current request
     * @return a {@code ResponseEntity} instance
     */
    private ResponseEntity<Object> globalExceptionHandler(final Exception ex, final WebRequest request) {
        ErrorDetails details = new ErrorDetails(ZonedDateTime.now(), MESSAGE, ex.getMessage(), ex.getStackTrace());

        LOGGER.error("Error from data service, details: {}", details);
        return new ResponseEntity<>(details, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
