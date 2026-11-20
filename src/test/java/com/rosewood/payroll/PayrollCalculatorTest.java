package com.rosewood.payroll;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class PayrollCalculatorTest {

    private static final double PRECISION = 0.000001;

    private final PayrollCalculator calculator = calculatorUsing(new InMemoryEmployeeRepository());

    @ParameterizedTest(name = "{0}")
    @CsvSource({
        "standard week in the LOW band,        e1, 40,   20,      760.0",
        "overtime pushes pay into MEDIUM,      e2, 50,   30,      1402.5",
        "one overtime hour is paid at 1.5x,    e1, 41,   20,      788.5",
        "no hours worked,                      e1, 0,    20,      0.0",
        "last pay in LOW (gross 999.99),       e1, 1,    999.99,  949.9905",
        "first pay in MEDIUM (gross 1000),     e1, 40,   25,      850.0",
        "last pay in MEDIUM (gross 4999.99),   e1, 1,    4999.99, 4249.9915",
        "first pay in HIGH (gross 5000),       e1, 1,    5000,    3750.0",
        "overtime in the HIGH band,            e1, 100,  100,     9750.0"
    })
    void calculatesNetPayFromHoursRateAndTaxBand(
            String scenario, String employeeId, double hours, double rate, double expectedNet) {
        double netPay = calculator.calculateNetPay(employeeId, hours, rate);

        assertEquals(expectedNet, netPay, PRECISION);
    }

    @Test
    void usesTheRepositoryItIsGivenNotAHardWiredOne() {
        EmployeeRepository repository = mock(EmployeeRepository.class);
        when(repository.findById("e9")).thenReturn(new Employee("e9", "Ama Mensah"));

        double netPay = calculatorUsing(repository).calculateNetPay("e9", 40, 20);

        assertEquals(760.0, netPay, PRECISION);
    }

    @Test
    void rejectsAnUnknownEmployeeInsteadOfPayingZero() {
        EmployeeNotFoundException failure = assertThrows(EmployeeNotFoundException.class,
                () -> calculator.calculateNetPay("unknown", 40, 20));

        assertTrue(failure.getMessage().contains("unknown"));
    }

    @Test
    void doesNotHideAFailureInTheRepository() {
        IllegalStateException databaseDown = new IllegalStateException("database is down");
        EmployeeRepository repository = mock(EmployeeRepository.class);
        when(repository.findById("e1")).thenThrow(databaseDown);

        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> calculatorUsing(repository).calculateNetPay("e1", 40, 20));

        assertSame(databaseDown, failure);
    }

    @ParameterizedTest
    @ValueSource(doubles = {-0.01, -5, Double.NaN, Double.POSITIVE_INFINITY})
    void rejectsInvalidHoursInsteadOfProducingAStrangeAmount(double badHours) {
        InvalidPayInputException failure = assertThrows(InvalidPayInputException.class,
                () -> calculator.calculateNetPay("e1", badHours, 20));

        assertTrue(failure.getMessage().contains("Hours worked"));
    }

    @ParameterizedTest
    @ValueSource(doubles = {-0.01, -20, Double.NaN, Double.POSITIVE_INFINITY})
    void rejectsAnInvalidHourlyRate(double badRate) {
        InvalidPayInputException failure = assertThrows(InvalidPayInputException.class,
                () -> calculator.calculateNetPay("e1", 40, badRate));

        assertTrue(failure.getMessage().contains("Hourly rate"));
    }

    @Test
    void netPayIsWhatIsLeftOfGrossPayAfterTheTaxCalculatorsTax() {
        GrossPayCalculator grossPayCalculator = mock(GrossPayCalculator.class);
        TaxCalculator taxCalculator = mock(TaxCalculator.class);
        when(grossPayCalculator.calculateGrossPay(40, 20)).thenReturn(1000.0);
        when(taxCalculator.calculateTax(1000.0)).thenReturn(123.0);
        PayrollCalculator calculatorWithFixedFigures = new PayrollCalculator(
                new InMemoryEmployeeRepository(), grossPayCalculator, taxCalculator);

        double netPay = calculatorWithFixedFigures.calculateNetPay("e1", 40, 20);

        assertEquals(877.0, netPay, PRECISION);
    }

    @Test
    void refusesToBeBuiltWithoutItsCollaborators() {
        EmployeeRepository repository = mock(EmployeeRepository.class);
        GrossPayCalculator grossPay = new GrossPayCalculator();
        TaxCalculator tax = TaxCalculator.withStandardBands();

        assertThrows(NullPointerException.class,
                () -> new PayrollCalculator(null, grossPay, tax));
        assertThrows(NullPointerException.class,
                () -> new PayrollCalculator(repository, null, tax));
        assertThrows(NullPointerException.class,
                () -> new PayrollCalculator(repository, grossPay, null));
    }

    private static PayrollCalculator calculatorUsing(EmployeeRepository repository) {
        return new PayrollCalculator(
                repository, new GrossPayCalculator(), TaxCalculator.withStandardBands());
    }
}
