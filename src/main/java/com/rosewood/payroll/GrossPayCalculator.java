package com.rosewood.payroll;

/** Works out gross pay: hours times rate, with overtime paid at time-and-a-half. */
public class GrossPayCalculator {

    private static final double STANDARD_HOURS = 40;
    private static final double OVERTIME_PREMIUM = 0.5;

    public double calculateGrossPay(double hoursWorked, double hourlyRate) {
        if (!isPayable(hoursWorked)) {
            throw new InvalidPayInputException("Hours worked", hoursWorked, "must be zero or more");
        }
        if (!isPayable(hourlyRate)) {
            throw new InvalidPayInputException("Hourly rate", hourlyRate, "must be zero or more");
        }

        double gross = hoursWorked * hourlyRate;

        if (hoursWorked > STANDARD_HOURS) {
            double overtimeHours = hoursWorked - STANDARD_HOURS;
            gross = gross + overtimeHours * hourlyRate * OVERTIME_PREMIUM;
        }
        return gross;
    }

    private static boolean isPayable(double value) {
        return value >= 0 && !Double.isInfinite(value);
    }
}
