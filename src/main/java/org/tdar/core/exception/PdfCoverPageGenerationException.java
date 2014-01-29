package org.tdar.core.exception;

public class PdfCoverPageGenerationException extends TdarRecoverableRuntimeException {

    private static final long serialVersionUID = 7411723303332607060L;

    public PdfCoverPageGenerationException() {
        super();
    }

    public PdfCoverPageGenerationException(String message) {
        super(message);
    }

    public PdfCoverPageGenerationException(String message, Throwable cause) {
        super(message, cause);
    }

}
