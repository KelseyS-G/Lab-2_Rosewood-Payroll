package com.rosewood.payroll;

import java.util.Objects;

/** Works out net pay by finding the employee, then applying gross pay and tax in turn. */
public class PayrollCalculator {

    private static final System.Logger LOGGER =
            System.getLogger(PayrollCalculator.class.getName());

    private final EmployeeRepository repository;
    private final GrossPayCalculator grossPayCalculator;
    private final TaxCalculator taxCalculator;

    public PayrollCalculator(EmployeeRepository repository,
            GrossPayCalculator grossPayCalculator, TaxCalculator taxCalculator) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.grossPayCalculator = Objects.requireNonNull(grossPayCalculator, "grossPayCalculator");
        this.taxCalculator = Objects.requireNonNull(taxCalculator, "taxCalculator");
    }

    /**
     * Calculates net pay. A failure is logged once here, at the edge of the service, and then
     * thrown again unchanged, so the caller still decides what to do about it.
     */
    public double calculateNetPay(String employeeId, double hoursWorked, double hourlyRate) {
        try {
            return netPayFor(employeeId, hoursWorked, hourlyRate);
        } catch (PayrollException e) {
            LOGGER.log(System.Logger.Level.WARNING, "Pay not calculated: " + e.getMessage());
            throw e;
        } catch (RuntimeException e) {
            LOGGER.log(System.Logger.Level.ERROR,
                    "Unexpected failure calculating pay for employee '" + employeeId + "'", e);
            throw e;
        }
    }

    private double netPayFor(String employeeId, double hoursWorked, double hourlyRate) {
        requireValidEmployeeId(employeeId);
        requireExistingEmployee(employeeId);

        double gross = grossPayCalculator.calculateGrossPay(hoursWorked, hourlyRate);
        return gross - taxCalculator.calculateTax(gross);
    }

    private static void requireValidEmployeeId(String employeeId) {
        if (employeeId == null || employeeId.isBlank()) {
            throw new InvalidPayInputException("Employee id", employeeId, "must not be blank");
        }
    }

    private void requireExistingEmployee(String employeeId) {
        if (repository.findById(employeeId).isEmpty()) {
            throw new EmployeeNotFoundException(employeeId);
        }
    }
}
