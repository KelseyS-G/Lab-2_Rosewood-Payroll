package com.rosewood.payroll;

import java.util.Objects;

/** Works out net pay by finding the employee, then applying gross pay and tax in turn. */
public class PayrollCalculator {

    private final EmployeeRepository repository;
    private final GrossPayCalculator grossPayCalculator;
    private final TaxCalculator taxCalculator;

    public PayrollCalculator(EmployeeRepository repository,
            GrossPayCalculator grossPayCalculator, TaxCalculator taxCalculator) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.grossPayCalculator = Objects.requireNonNull(grossPayCalculator, "grossPayCalculator");
        this.taxCalculator = Objects.requireNonNull(taxCalculator, "taxCalculator");
    }

    public double calculateNetPay(String employeeId, double hoursWorked, double hourlyRate) {
        requireValid("Hours worked", hoursWorked);
        requireValid("Hourly rate", hourlyRate);
        requireExistingEmployee(employeeId);

        double gross = grossPayCalculator.calculateGrossPay(hoursWorked, hourlyRate);
        return gross - taxCalculator.calculateTax(gross);
    }

    private static void requireValid(String label, double value) {
        if (Double.isNaN(value) || Double.isInfinite(value) || value < 0) {
            throw new InvalidPayInputException(label + " must be zero or more but was " + value);
        }
    }

    private void requireExistingEmployee(String employeeId) {
        if (repository.findById(employeeId) == null) {
            throw new EmployeeNotFoundException(employeeId);
        }
    }
}
