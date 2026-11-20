package com.rosewood.payroll;

import java.util.List;

public class Demo {

    public static void main(String[] args) {
        EmployeeRepository repository = new InMemoryEmployeeRepository(List.of(
                new Employee("e1", "Efua Sarpong"),
                new Employee("e2", "Kwame Owusu")));
        PayrollCalculator calculator = new PayrollCalculator(repository,
                new GrossPayCalculator(), new TaxCalculator(StandardTaxBands.current()));

        show(calculator, "e1", 40, 20);
        show(calculator, "e2", 50, 30);
        show(calculator, "unknown", 40, 20);
    }

    private static void show(PayrollCalculator calculator, String id, double hours, double rate) {
        String label = id + ", " + hours + "h @ " + rate;
        try {
            System.out.println(label + " -> " + calculator.calculateNetPay(id, hours, rate));
        } catch (PayrollException e) {
            System.out.println(label + " -> FAILED: " + e.getMessage());
        }
    }
}
