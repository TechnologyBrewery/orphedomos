package org.technologybrewery.orphedomos.util;

/**
 * Specific runtime exception used to indicate that this came from Orphedomos.  Other javadoc not overridden as this is
 * not needed unless adding to the original javadoc.
 */
public class OrphedomosException extends RuntimeException {

    public OrphedomosException() {
        super();
    }

    public OrphedomosException(String message, Throwable cause) {
        super(message, cause);
    }

    public OrphedomosException(String message) {
        super(message);
    }

    public OrphedomosException(Throwable cause) {
        super(cause);
    }
}
