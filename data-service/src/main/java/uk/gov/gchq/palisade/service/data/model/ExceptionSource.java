package uk.gov.gchq.palisade.service.data.model;

/**
 * An ExceptionSource enum used to add attributes to the {@link AuditErrorMessage}
 * when reporting an error within the Data Service
 */
public enum ExceptionSource {

    /**
     * If an exception is thrown when requesting the authorisation.
     */
    AUTHORISED_REQUEST,

    /**
     * If an exception is thrown in reading the resource.
     */
    READ;

    public static final String ATTRIBUTE_KEY = "METHOD";

    }
