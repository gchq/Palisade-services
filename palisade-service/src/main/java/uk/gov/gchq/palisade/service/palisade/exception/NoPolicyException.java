package uk.gov.gchq.palisade.service.palisade.exception;

import uk.gov.gchq.palisade.exception.PalisadeRuntimeException;

public class NoPolicyException extends PalisadeRuntimeException {

    public NoPolicyException(final String e) {
        super(e);
    }

    public NoPolicyException(final Throwable cause) {
        super(cause);
    }

    public NoPolicyException(final String e, final Throwable cause) {
        super(e, cause);
    }
}
