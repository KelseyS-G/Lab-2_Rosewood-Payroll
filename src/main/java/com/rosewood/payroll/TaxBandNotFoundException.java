package com.rosewood.payroll;

/** Thrown when no tax band covers a gross pay, which means the tax table is incomplete. */
public class TaxBandNotFoundException extends PayrollException {

    private final double grossPay;

    public TaxBandNotFoundException(double grossPay) {
        super("No tax band covers a gross pay of " + grossPay);
        this.grossPay = grossPay;
    }

    public double getGrossPay() {
        return grossPay;
    }
}
