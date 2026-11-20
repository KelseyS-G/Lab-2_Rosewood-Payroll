package com.rosewood.payroll;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Keeps employees in memory. The caller supplies them, so no sample data lives here. */
public class InMemoryEmployeeRepository implements EmployeeRepository {

    private final Map<String, Employee> employeesById = new HashMap<>();

    public InMemoryEmployeeRepository(Collection<Employee> employees) {
        for (Employee employee : employees) {
            Employee previous = employeesById.putIfAbsent(employee.getId(), employee);
            if (previous != null) {
                throw new IllegalArgumentException(
                        "Duplicate employee id '" + employee.getId() + "'");
            }
        }
    }

    @Override
    public Optional<Employee> findById(String id) {
        return Optional.ofNullable(employeesById.get(id));
    }
}
