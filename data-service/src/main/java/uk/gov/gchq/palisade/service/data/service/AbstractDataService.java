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

package uk.gov.gchq.palisade.service.data.service;

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
import uk.gov.gchq.palisade.service.data.service.authorisation.AuditableAuthorisationService;
import uk.gov.gchq.palisade.service.data.service.reader.DataReader;
import uk.gov.gchq.palisade.user.User;

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
// Suppress usage of generic wildcard type, domain class isn't known until execution
@SuppressWarnings("java:S1452")
public abstract class AbstractDataService implements DataService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractDataService.class);
    // Optimisation to return raw data if rules applied to the resource would have no effect on the records
    // Disables auditing records processed and returned
    private static final boolean SHOULD_SKIP_SERDES_IF_NO_APPLICABLE_RULES = true;

    protected Collection<DataReader> readers;
    protected Map<String, Class<Serialiser<?>>> serialisers;
    protected AuditableAuthorisationService authorisationService;
    protected AuditMessageService auditService;

    protected AbstractDataService(
            final Collection<DataReader> readers,
            final Map<String, Class<Serialiser<?>>> serialisers,
            final AuditableAuthorisationService authorisationService,
            final AuditMessageService auditService) {
        this.readers = readers;
        this.serialisers = serialisers;
        this.authorisationService = authorisationService;
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
     * @return a flow from reader/serialiser/rule output bytes to the output of {@link AbstractDataService#defaultRunnableGraph(AuditableAuthorisedDataRequest)}
     * which allows for injecting 'interesting' behaviour before auditing.
     */
    protected abstract Flow<ByteString, ByteString, NotUsed> transformResponse();

    // Suppress warning for untyped lambda expressions, where the type is not known until execution time
    @SuppressWarnings("java:S2211")
    protected Source<ByteString, CompletionStage<Done>> defaultRunnableGraph(final AuditableAuthorisedDataRequest auditable) {
        AbstractDataService service = this;
        LOGGER.info("Selected service '{}' based on route path", service.getClass());

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
                    // Suppress cast Rules<?> to Rules<Serializable>
                    @SuppressWarnings("unchecked")
                    Rules<Serializable> rules = (Rules<Serializable>) authorised.getRules();
                    LOGGER.debug("User '{}' requested resource '{}' with context '{}' and got record-level rules '{}'", user, leafResource, context, rules);

                    DataReader reader = readers.stream()
                            .filter(r -> r.accepts(leafResource)).findAny()
                            .orElseThrow(() -> new ReaderNotFoundException("Could not find a reader that accepts " + leafResource.getId()));
                    LOGGER.info("Selected reader '{}' based on resource URI '{}'", reader.getClass(), leafResource.getId());

                    Class<Serialiser<?>> serialiserClass = Optional.ofNullable(serialisers.get(leafResource.getSerialisedFormat()))
                            .orElseThrow(() -> new SerialiserNotFoundException("Could not find a serialiser that accepts " + leafResource.getSerialisedFormat()));
                    LOGGER.info("Selected serialiser '{}' based on resource serialised format '{}'", serialiserClass, leafResource.getSerialisedFormat());

                    Serialiser<Serializable> serialiser = Serialiser.<Serializable>tryCreate(serialiserClass, leafResource.getType())
                            .orElseThrow(() -> new SerialiserInitialisationException("Failed to construct a serialiser using domain class " + leafResource.getType()));
                    LOGGER.info("Built serialiser instance '{}' (with domain class) based on resource type '{}'", serialiser, leafResource.getType());

                    var readerBytes = reader.readSource(leafResource);

                    boolean rulesAreApplicable = isAnyRuleApplicable(user, context, rules);

                    if (SHOULD_SKIP_SERDES_IF_NO_APPLICABLE_RULES && !rulesAreApplicable) {

                        // Cannot count records if skipping deserialisation
                        recordsProcessed.set(-1L);
                        recordsReturned.set(-1L);
                        // Read and return raw data as there were no rules to apply
                        return readerBytes;

                    } else {

                        return readerBytes
                                // Deserialise bytes into objects
                                .via(serialiser.deserialiseFlow())

                                // Apply rules, collecting audit data for total records processed (unredacted)
                                .map((Serializable record) -> {
                                    recordsProcessed.incrementAndGet();
                                    return record;
                                })

                                // Apply roles in a flow, taking advantage of multiple threads and backpressuring mechanisms
                                .via(applyRulesInFlow(user, context, rules))

                                // Unwrap optional, collecting audit data for total records returned (after redactions)
                                .map((Serializable record) -> {
                                    recordsReturned.incrementAndGet();
                                    return record;
                                })

                                // Serialise objects back to bytes
                                .via(serialiser.serialiseFlow());
                    }
                })

                // Flatten nested CompletionStage<CompletionStage<T>> to CompletionStage<T>
                .mapMaterializedValue(cs -> cs.thenCompose(Function.identity()))

                // Transform data before writing back to the client
                .via(service.transformResponse())

                // Catch errors and audit
                .watchTermination((CompletionStage<Done> prevMatValue, CompletionStage<Done> completion) -> {
                    // Either (done == something, ex == null) or (done == null, ex == something)
                    completion.whenComplete((Done done, Throwable ex) -> {
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

    /**
     * Convert a collection of {@link uk.gov.gchq.palisade.rule.Rule} objects into a single {@link Flow}
     * This applies the rules as separate flow stages, making rule application backpressure-aware and
     * short-circuiting rule application as soon as a record is totally redacted to 'null'.
     *
     * @param user the user reading the resource
     * @param context the context for the resource read request
     * @param rules the rules decided to be applied to each record of this resource
     * @param <T> the type of the records in the resource
     * @return a backpressure-aware {@link Flow}, applying each rule as a separate stream processor stage
     */
    private static <T extends Serializable> Flow<T, T, NotUsed> applyRulesInFlow(final User user, final Context context, final Rules<T> rules) {
        Flow<Optional<T>, Optional<T>, NotUsed> boxedRuleFlow = rules.getRules()
                .values()
                .stream()
                .reduce(Flow.create(),
                        // Nulls are wrapped in optionals and short-circuit rule application
                        (flow, rule) -> flow.map(optRecord -> optRecord.flatMap(record -> Optional.ofNullable(rule
                                .apply(record, user, context)))),
                        // If two partial Flows are generated, then join one after the other
                        Flow::via);

        return Flow.<T>create()
                // Records are boxed/unboxed in Optionals, as reactive-streams spec doesn't allow null elements
                .map(Optional::of)
                // Apply rules, mapping total redactions, or 'nulls', to Optional.empty()
                .via(boxedRuleFlow)
                // Filter out empty Optionals, or 'nulls' - i.e. total redaction of a record
                .filter(Optional::isPresent)
                // Unbox non-null record
                .map(Optional::get);
    }

    private static boolean isAnyRuleApplicable(final User user, final Context context, final Rules<?> rules) {
        return rules.getRules()
                .values()
                .stream()
                .map(rule -> rule.isApplicable(user, context))
                .filter(applicable -> applicable)
                .findAny()
                .orElse(false);
    }

    @Override
    @Generated
    public String toString() {
        return new StringJoiner(", ", AbstractDataService.class.getSimpleName() + "[", "]")
                .add("readers=" + readers)
                .add("serialisers=" + serialisers)
                .add("dataService=" + authorisationService)
                .add("auditService=" + auditService)
                .toString();
    }
}
