package uk.gov.gchq.palisade.service.audit.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.RequestId;
import uk.gov.gchq.palisade.UserId;
import uk.gov.gchq.palisade.service.Service;

import java.util.StringJoiner;

import static java.util.Objects.requireNonNull;

/**
 * This is one of the objects that is passed to the audit-service
 * to be able to store an audit record. This class extends {@link AuditRequest} This class
 * is used for the indication to the Audit logs that an exception has been received while processing the RegisterDataRequest
 * and which service it was that triggered the exception.
 */
public class RegisterRequestExceptionAuditRequest extends AuditRequest {

    public final UserId userId;
    public final String resourceId;
    public final Context context;
    public final Throwable exception;
    public final Class<? extends Service> serviceClass;

    @JsonCreator
    private RegisterRequestExceptionAuditRequest(@JsonProperty("id") final RequestId id, @JsonProperty("originalRequestId") final RequestId originalRequestId, @JsonProperty("userId") final UserId userId, @JsonProperty("resourceId") final String resourceId,
                                                 @JsonProperty("context") final Context context, @JsonProperty("exception") final Throwable exception, @JsonProperty("serviceClass") final Class<? extends Service> serviceClass) {
        super(originalRequestId);
        this.userId = requireNonNull(userId);
        this.resourceId = requireNonNull(resourceId);
        this.context = requireNonNull(context);
        this.exception = requireNonNull(exception);
        this.serviceClass = requireNonNull(serviceClass);
    }

    /**
     * Static factory method.
     *
     * @param original the original request id
     * @return the {@link RegisterRequestExceptionAuditRequest}
     */
    public static IUserId create(final RequestId original) {
        return user -> resourceId -> context -> exception -> serviceClass -> new RegisterRequestExceptionAuditRequest(null, original, user, resourceId, context, exception, serviceClass);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final RegisterRequestExceptionAuditRequest that = (RegisterRequestExceptionAuditRequest) o;
        return new EqualsBuilder()
                .appendSuper(super.equals(o))
                .append(userId, that.userId)
                .append(resourceId, that.resourceId)
                .append(context, that.context)
                .append(exception, that.exception)
                .append(serviceClass, that.serviceClass)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(27, 41)
                .appendSuper(super.hashCode())
                .append(userId)
                .append(resourceId)
                .append(context)
                .append(exception)
                .append(serviceClass)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", RegisterRequestExceptionAuditRequest.class.getSimpleName() + "[", "]")
                .add(super.toString())
                .add("userId=" + userId)
                .add("resourceId='" + resourceId + "'")
                .add("context=" + context)
                .add("exception=" + exception)
                .add("serviceClass=" + serviceClass)
                .toString();
    }

    public interface IUserId {
        /**
         * @param userId {@link UserId} is the user id provided in the register request
         * @return the {@link RegisterRequestExceptionAuditRequest}
         */
        IResourceId withUserId(final UserId userId);
    }

    public interface IResourceId {
        /**
         * @param resourceId {@link String} is the resource id provided in the register request
         * @return the {@link RegisterRequestExceptionAuditRequest}
         */
        IContext withResourceId(final String resourceId);
    }

    public interface IContext {
        /**
         * @param context the context that was passed by the client to the palisade service
         * @return the {@link RegisterRequestExceptionAuditRequest}
         */
        IException withContext(final Context context);
    }

    public interface IException {
        /**
         * @param exception {@link Throwable} is the type of the exception while processing
         * @return the {@link RegisterRequestExceptionAuditRequest}
         */
        IServiceClass withException(final Throwable exception);
    }

    public interface IServiceClass {
        /**
         * @param serviceClass {@link Class} is the palisade service that the exception was triggered by.
         * @return the {@link RegisterRequestExceptionAuditRequest}
         */
        RegisterRequestExceptionAuditRequest withServiceClass(final Class<? extends Service> serviceClass);
    }
}