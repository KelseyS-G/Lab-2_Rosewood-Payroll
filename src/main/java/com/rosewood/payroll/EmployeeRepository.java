package com.rosewood.payroll;

import java.util.Optional;

/** Source of employee records. */
public interface EmployeeRepository {

    /** Returns the employee with this id, or an empty result when there is none. */
    Optional<Employee> findById(String id);
}
