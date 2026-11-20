package com.rosewood.payroll;

/** Thrown when an input cannot be paid: a blank employee id, or invalid hours or rate. */
public class InvalidPayInputException extends PayrollException {

    private final String field;
    private final transient Object value;

    public InvalidPayInputException(String field, Object value, String rule) {
        super(field + " " + rule + " but was " + value);
        this.field = field;
        this.value = value;
    }

    /** Which input was rejected, for example "Hours worked". */
    public String getField() {
        return field;
    }

    /** The value that was rejected. */
    public Object getValue() {
        return value;
    }
}
