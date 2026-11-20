package com.rosewood.payroll;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class InMemoryEmployeeRepositoryTest {

    private static final Employee EFUA = new Employee("e1", "Efua Sarpong");
    private static final Employee KWAME = new Employee("e2", "Kwame Owusu");

    @Test
    void findsAnEmployeeById() {
        InMemoryEmployeeRepository repository =
                new InMemoryEmployeeRepository(List.of(EFUA, KWAME));

        assertEquals(Optional.of(KWAME), repository.findById("e2"));
    }

    @Test
    void returnsEmptyForAnUnknownId() {
        InMemoryEmployeeRepository repository = new InMemoryEmployeeRepository(List.of(EFUA));

        assertTrue(repository.findById("e2").isEmpty());
    }

    @Test
    void returnsEmptyForANullId() {
        InMemoryEmployeeRepository repository = new InMemoryEmployeeRepository(List.of(EFUA));

        assertTrue(repository.findById(null).isEmpty());
    }

    @Test
    void rejectsTwoEmployeesWithTheSameId() {
        Employee impostor = new Employee("e1", "Someone Else");

        assertThrows(IllegalArgumentException.class,
                () -> new InMemoryEmployeeRepository(List.of(EFUA, impostor)));
    }

    @Test
    void isNotAffectedByLaterChangesToTheListItWasBuiltFrom() {
        List<Employee> source = new ArrayList<>(List.of(EFUA));
        InMemoryEmployeeRepository repository = new InMemoryEmployeeRepository(source);

        source.add(KWAME);

        assertTrue(repository.findById("e2").isEmpty());
    }
}
