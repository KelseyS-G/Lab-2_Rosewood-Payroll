package com.rosewood.payroll;

/** Thrown when pay is requested for an employee id the repository does not know. */
public class EmployeeNotFoundException extends PayrollException {

    private final String employeeId;

    public EmployeeNotFoundException(String employeeId) {
        super("No employee found with id '" + employeeId + "'");
        this.employeeId = employeeId;
    }

    public String getEmployeeId() {
        return employeeId;
    }
}
