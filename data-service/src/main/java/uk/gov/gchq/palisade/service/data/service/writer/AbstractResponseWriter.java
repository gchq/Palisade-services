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

package uk.gov.gchq.palisade.service.data.service.writer;

import akka.Done;
import akka.NotUsed;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Source;
import akka.util.ByteString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.Generated;
import uk.gov.gchq.palisade.data.serialise.Serialiser;
import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.rule.Rules;
import uk.gov.gchq.palisade.service.data.exception.ForbiddenException;
import uk.gov.gchq.palisade.service.data.exception.ReadException;
import uk.gov.gchq.palisade.service.data.exception.ReaderNotFoundException;
import uk.gov.gchq.palisade.service.data.exception.SerialiserInitialisationException;
import uk.gov.gchq.palisade.service.data.exception.SerialiserNotFoundException;
import uk.gov.gchq.palisade.service.data.model.AuditErrorMessage;
import uk.gov.gchq.palisade.service.data.model.AuditMessage;
import uk.gov.gchq.palisade.service.data.model.AuditSuccessMessage;
import uk.gov.gchq.palisade.service.data.model.AuditableAuthorisedDataRequest;
import uk.gov.gchq.palisade.service.data.model.AuthorisedDataRequest;
import uk.gov.gchq.palisade.service.data.model.ExceptionSource;
import uk.gov.gchq.palisade.service.data.model.TokenMessagePair;
import uk.gov.gchq.palisade.service.data.service.AuditMessageService;
import uk.gov.gchq.palisade.service.data.service.AuditableDataService;
import uk.gov.gchq.palisade.service.data.service.reader.DataReader;
import uk.gov.gchq.palisade.user.User;
import uk.gov.gchq.palisade.util.RulesUtil;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

/**
 * Handles a number of common functions of a ResponseWriter
 */
public abstract class AbstractResponseWriter implements ResponseWriter {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractResponseWriter.class);

    protected Collection<DataReader> readers;
    protected Map<String, Class<Serialiser<?>>> serialisers;
    protected AuditableDataService dataService;
    protected AuditMessageService auditService;

    protected AbstractResponseWriter(
            final Collection<DataReader> readers,
            final Map<String, Class<Serialiser<?>>> serialisers,
            final AuditableDataService dataService,
            final AuditMessageService auditService) {
        this.readers = readers;
        this.serialisers = serialisers;
        this.dataService = dataService;
        this.auditService = auditService;

        LOGGER.debug("Initialised {} with readers {} and serialisers {}", this.getClass(), this.readers, this.serialisers);
    }

    @Generated
    public Collection<DataReader> getReaders() {
        return readers;
    }

    @Generated
    public void setReaders(final Collection<DataReader> readers) {
        this.readers = Optional.ofNullable(readers)
                .orElseThrow(() -> new IllegalArgumentException("readers cannot be null"));
    }

    @Generated
    public Map<String, Class<Serialiser<?>>> getSerialisers() {
        return serialisers;
    }

    @Generated
    public void setSerialisers(final Map<String, Class<Serialiser<?>>> serialisers) {
        this.serialisers = Optional.ofNullable(serialisers)
                .orElseThrow(() -> new IllegalArgumentException("serialisers cannot be null"));
    }

    /**
     * Implement a transformation from reader/serialiser/rule output as a callback for the last point of auditing.
     *
     * @return a flow from reader/serialiser/rule output bytes to the output of {@link AbstractResponseWriter#defaultRunnableGraph(AuditableAuthorisedDataRequest)}
     * which allows for injecting 'interesting' behaviour before auditing.
     */
    protected abstract Flow<ByteString, ByteString, NotUsed> transformResponse();

    protected Source<ByteString, CompletionStage<Done>> defaultRunnableGraph(final AuditableAuthorisedDataRequest auditable) {
        AbstractResponseWriter writer = this;
        LOGGER.info("Selected writer '{}' based on route path", writer.getClass());

        final AtomicLong recordsProcessed = new AtomicLong(0);
        final AtomicLong recordsReturned = new AtomicLong(0);

        var authorisation = Optional.ofNullable(auditable.getAuthorisedDataRequest());
        LOGGER.debug("Authorisation was {}, but effect will be delegated until stream materialisation", authorisation.isPresent());

        var responseSource = Source
                // Construct lazily so errors in construction fall through with errors in reading
                .lazySource(() -> {
                    AuthorisedDataRequest authorised = authorisation
                            .orElseThrow(() -> new ForbiddenException("Authorisation denied for request " + auditable.getDataRequest(), auditable.getAuditErrorMessage().getError()));
                    LOGGER.debug("Request '{}' was authorised", auditable.getDataRequest());

                    User user = authorised.getUser();
                    LeafResource leafResource = authorised.getResource();
                    Context context = authorised.getContext();
                    @SuppressWarnings("unchecked") Rules<Serializable> rules = (Rules<Serializable>) authorised.getRules();
                    LOGGER.debug("User '{}' requested resource '{}' with context '{}' and got record-level rules '{}'", user, leafResource, context, rules);

                    DataReader reader = readers.stream().filter(r -> r.accepts(leafResource)).findAny()
                            .orElseThrow(() -> new ReaderNotFoundException("Could not find a reader that accepts " + leafResource.getId()));
                    LOGGER.info("Selected reader '{}' based on resource URI '{}'", reader.getClass(), leafResource.getId());

                    Class<Serialiser<?>> serialiserClass = Optional.ofNullable(serialisers.get(leafResource.getSerialisedFormat()))
                            .orElseThrow(() -> new SerialiserNotFoundException("Could not find a serialiser that accepts " + leafResource.getSerialisedFormat()));
                    LOGGER.info("Selected serialiser '{}' based on resource serialised format '{}'", serialiserClass, leafResource.getSerialisedFormat());

                    Serialiser<Serializable> serialiser = Serialiser.<Serializable>tryCreate(serialiserClass, leafResource.getType())
                            .orElseThrow(() -> new SerialiserInitialisationException("Failed to construct a serialiser using domain class " + leafResource.getType()));
                    LOGGER.info("Built serialiser instance '{}' (with domain class) based on resource type '{}'", serialiser, leafResource.getType());

                    // Read raw data
                    return reader.readSource(leafResource)

                            // Deserialise bytes into objects
                            .via(serialiser.deserialiseFlow())

                            // Apply rules, collecting audit data for total records processed (unredacted)
                            .map(record -> {
                                recordsProcessed.incrementAndGet();
                                return Optional.ofNullable(RulesUtil.applyRulesToItem(record, user, context, rules));
                            })

                            // Nulls are wrapped in optionals as the reactive-streams spec doesn't allow null elements
                            // Filter out 'nulls' - i.e. total redaction of a record
                            .filter(Optional::isPresent)

                            // Unwrap optional, collecting audit data for total records returned (after redactions)
                            .map(record -> {
                                recordsReturned.incrementAndGet();
                                // Appease any linters and codestyle checkers, although this should never fail as empties have been filtered
                                assert record.isPresent();
                                return record.get();
                            })

                            // Serialise objects back to bytes
                            .via(serialiser.serialiseFlow());
                })

                // Flatten nested CompletionStage<CompletionStage<T>> to CompletionStage<T>
                .mapMaterializedValue(cs -> cs.thenCompose(Function.identity()))

                // Transform data before writing back to the client
                .via(writer.transformResponse())

                // Catch errors and audit
                .watchTermination((prevMatValue, completion) -> {
                    completion.whenComplete((done, ex) -> {
                        AuditMessage auditMessage;
                        if (done != null) {
                            LOGGER.debug("Auditing success on termination of stream");
                            // Success - audit records processed and returned
                            auditMessage = AuditSuccessMessage.Builder.create(auditable)
                                    .withRecordsProcessedAndReturned(recordsProcessed.get(), recordsReturned.get());
                        } else if (ex != null) {
                            LOGGER.debug("Auditing error on termination of stream '{}'", ex.getMessage());
                            LOGGER.trace("Exception was", ex);
                            // Error - first establish where the error occurred
                            if (authorisation.isPresent()) {
                                // Error occurred while reading/deserialising/applying rules
                                auditMessage = AuditErrorMessage.Builder.create(auditable.getDataRequest(), auditable.getAuthorisedDataRequest())
                                        .withAttributes(Map.of(ExceptionSource.ATTRIBUTE_KEY, ExceptionSource.READ))
                                        .withError(ex);
                            } else {
                                // Error occurred because client is unauthorised to read the requested resource with the given token
                                auditMessage = AuditErrorMessage.Builder.create(auditable.getDataRequest())
                                        .withAttributes(Map.of(ExceptionSource.ATTRIBUTE_KEY, ExceptionSource.AUTHORISED_REQUEST))
                                        .withError(ex);
                            }
                        } else {
                            // Shouldn't be reachable at runtime except by an Akka bug
                            throw new ReadException("Akka stream terminated with neither success nor error, this may be an Akka bug");
                        }

                        // Send audit message to kafka topic
                        var tokenMessagePair = TokenMessagePair.Builder.create()
                                .withToken(auditable.getDataRequest().getToken())
                                .withAuditMessage(auditMessage);
                        LOGGER.debug("Sending audit message '{}'", tokenMessagePair);
                        auditService.auditMessage(tokenMessagePair);
                    });
                    return prevMatValue;
                });

        LOGGER.debug("Returning default response source");
        return responseSource;
    }

    @Override
    @Generated
    public String toString() {
        return new StringJoiner(", ", AbstractResponseWriter.class.getSimpleName() + "[", "]")
                .add("readers=" + readers)
                .add("serialisers=" + serialisers)
                .add("dataService=" + dataService)
                .add("auditService=" + auditService)
                .toString();
    }
}
