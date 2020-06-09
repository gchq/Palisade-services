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

package uk.gov.gchq.palisade.service.palisade.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import uk.gov.gchq.palisade.service.palisade.exception.ErrorDetails;
import uk.gov.gchq.palisade.service.palisade.exception.NoPolicyException;
import uk.gov.gchq.palisade.service.palisade.web.PalisadeController;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.ZonedDateTime;

@ControllerAdvice(assignableTypes = PalisadeController.class)
@RequestMapping(produces = "application/json")
public class PalisadeServiceExceptionHandler extends ResponseEntityExceptionHandler {
    private static final Logger DEBUG_LOGGER = LoggerFactory.getLogger(PalisadeServiceExceptionHandler.class);
    private static final String MESSAGE = "The application encountered an issue while processing the request.";


    /**
     * This method customizes the response for a NoPolicyException error.
     *
     * @param ex the exception
     * @return a {@code ResponseEntity} instance
     */
    @ExceptionHandler(NoPolicyException.class)
    private ResponseEntity<Object> noPolicyExceptionHandler(final NoPolicyException ex) {
        DEBUG_LOGGER.error("Handling exception", ex);
        ErrorDetails details = new ErrorDetails(ZonedDateTime.now(), MESSAGE, ex.getClass().toString(), ex.getStackTrace());

        return new ResponseEntity<>(details, HttpStatus.NOT_FOUND);
    }

    /**
     * This method customizes the response for a NullPointerException error.
     *
     * @param ex the exception
     * @return a {@code ResponseEntity} instance
     */
    @ExceptionHandler(NullPointerException.class)
    private ResponseEntity<Object> nullPointerExceptionHandler(final NullPointerException ex) {
        DEBUG_LOGGER.error("Handling exception", ex);
        ErrorDetails details = new ErrorDetails(ZonedDateTime.now(), MESSAGE, ex.getClass().toString(), ex.getStackTrace());

        return new ResponseEntity<>(details, HttpStatus.BAD_REQUEST);
    }

    /**
     * This method customizes the response for a FileNotFoundException error.
     *
     * @param ex the exception
     * @return a {@code ResponseEntity} instance
     */
    @ExceptionHandler(FileNotFoundException.class)
    private ResponseEntity<Object> fileNotFoundExceptionHandler(final FileNotFoundException ex) {
        DEBUG_LOGGER.error("Handling exception", ex);
        ErrorDetails details = new ErrorDetails(ZonedDateTime.now(), MESSAGE, ex.getClass().toString(), ex.getStackTrace());

        return new ResponseEntity<>(details, HttpStatus.NOT_FOUND);
    }

    /**
     * This method customizes the response for an IOException error.
     *
     * @param ex the exception
     * @return a {@code ResponseEntity} instance
     */
    @ExceptionHandler(IOException.class)
    private ResponseEntity<Object> ioExceptionHandler(final IOException ex) {
        DEBUG_LOGGER.error("Handling exception", ex);
        ErrorDetails details = new ErrorDetails(ZonedDateTime.now(), MESSAGE, ex.getClass().toString(), ex.getStackTrace());

        return new ResponseEntity<>(details, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * This method customizes the response for a RuntimeException error.
     *
     * @param ex the exception
     * @return a {@code ResponseEntity} instance
     */
    @ExceptionHandler(RuntimeException.class)
    private ResponseEntity<Object> runtimeExceptionHandler(final RuntimeException ex) {
        DEBUG_LOGGER.error("Handling exception", ex);
        ErrorDetails details = new ErrorDetails(ZonedDateTime.now(), MESSAGE, ex.getClass().toString(), ex.getStackTrace());

        return new ResponseEntity<>(details, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * This method customizes the response for a general Exception error.
     *
     * @param ex the exception
     * @return a {@code ResponseEntity} instance
     */
    @ExceptionHandler(Exception.class)
    private ResponseEntity<Object> globalExceptionHandler(final Exception ex) {
        DEBUG_LOGGER.error("Handling exception", ex);
        ErrorDetails details = new ErrorDetails(ZonedDateTime.now(), MESSAGE, ex.getClass().toString(), ex.getStackTrace());

        return new ResponseEntity<>(details, HttpStatus.INTERNAL_SERVER_ERROR);
    }

}
