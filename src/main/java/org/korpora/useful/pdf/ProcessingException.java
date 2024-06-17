package org.korpora.useful.pdf;

import java.io.Serial;

/**
 * Exception in Processing documents
 *
 * @author Bernhard Fisseni
 */
public class ProcessingException extends Exception {

    /**
     * UID for serialisation
     */
    @Serial
    private static final long serialVersionUID = -3040849311600317575L;

    /**
     * a processing exception with a message only
     *
     * @param message the message
     */
    public ProcessingException(String message) {
        super(message);
    }

    /**
     * a processing exception that wraps another exception and a message
     *
     * @param message   the message
     * @param exception the exception
     */
    public ProcessingException(String message, Exception exception) {
        super(message, exception);
    }

    /**
     * a processing exception that wraps another exception
     *
     * @param exception the exception
     */
    public ProcessingException(Exception exception) {
        super(exception);
    }
}
