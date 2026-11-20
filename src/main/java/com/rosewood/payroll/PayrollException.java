package com.rosewood.payroll;

/** Base type for every failure the payroll service reports on purpose. */
public class PayrollException extends RuntimeException {

    public PayrollException(String message) {
        super(message);
    }
}
