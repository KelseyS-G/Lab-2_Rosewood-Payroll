package com.rosewood.payroll;

/** Works out gross pay: hours times rate, with overtime paid at time-and-a-half. */
public class GrossPayCalculator {

    private static final double STANDARD_HOURS = 40;
    private static final double OVERTIME_PREMIUM = 0.5;

    public double calculateGrossPay(double hoursWorked, double hourlyRate) {
        double gross = hoursWorked * hourlyRate;

        if (hoursWorked > STANDARD_HOURS) {
            double overtimeHours = hoursWorked - STANDARD_HOURS;
            gross = gross + overtimeHours * hourlyRate * OVERTIME_PREMIUM;
        }
        return gross;
    }
}
