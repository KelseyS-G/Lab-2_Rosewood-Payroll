package com.rosewood.payroll;

/** Thrown when hours or hourly rate cannot be paid: negative, NaN or infinite. */
public class InvalidPayInputException extends PayrollException {

    public InvalidPayInputException(String message) {
        super(message);
    }
}
