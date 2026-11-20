package com.rosewood.payroll;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class PayrollCalculatorTest {

    private static final double PRECISION = 0.000001;
    private static final Employee EFUA = new Employee("e1", "Efua Sarpong");
    private static final Employee KWAME = new Employee("e2", "Kwame Owusu");

    private final PayrollCalculator calculator =
            calculatorUsing(new InMemoryEmployeeRepository(List.of(EFUA, KWAME)));

    private final Logger payrollLogger = Logger.getLogger(PayrollCalculator.class.getName());
    private final List<LogRecord> logged = new ArrayList<>();
    private final Handler logCapture = new Handler() {
        @Override
        public void publish(LogRecord record) {
            logged.add(record);
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }
    };

    @BeforeEach
    void startCapturingLogs() {
        payrollLogger.setUseParentHandlers(false);
        payrollLogger.setLevel(Level.ALL);
        payrollLogger.addHandler(logCapture);
    }

    @AfterEach
    void stopCapturingLogs() {
        payrollLogger.removeHandler(logCapture);
        payrollLogger.setUseParentHandlers(true);
    }

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
        when(repository.findById("e9")).thenReturn(Optional.of(new Employee("e9", "Ama Mensah")));

        double netPay = calculatorUsing(repository).calculateNetPay("e9", 40, 20);

        assertEquals(760.0, netPay, PRECISION);
    }

    @Test
    void rejectsAnUnknownEmployeeInsteadOfPayingZero() {
        EmployeeNotFoundException failure = assertThrows(EmployeeNotFoundException.class,
                () -> calculator.calculateNetPay("unknown", 40, 20));

        assertEquals("unknown", failure.getEmployeeId());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    void rejectsABlankEmployeeIdAsBadInputRatherThanAnUnknownEmployee(String blankId) {
        InvalidPayInputException failure = assertThrows(InvalidPayInputException.class,
                () -> calculator.calculateNetPay(blankId, 40, 20));

        assertEquals("Employee id", failure.getField());
        assertEquals(blankId, failure.getValue());
    }

    @ParameterizedTest
    @ValueSource(doubles = {-5, Double.NaN})
    void rejectsHoursThatCannotBePaidInsteadOfProducingAStrangeAmount(double badHours) {
        InvalidPayInputException failure = assertThrows(InvalidPayInputException.class,
                () -> calculator.calculateNetPay("e1", badHours, 20));

        assertEquals("Hours worked", failure.getField());
    }

    @ParameterizedTest
    @ValueSource(doubles = {-20, Double.POSITIVE_INFINITY})
    void rejectsAnHourlyRateThatCannotBePaid(double badRate) {
        InvalidPayInputException failure = assertThrows(InvalidPayInputException.class,
                () -> calculator.calculateNetPay("e1", 40, badRate));

        assertEquals("Hourly rate", failure.getField());
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

    @Test
    void logsAnExpectedFailureAsAWarningAndStillThrowsIt() {
        assertThrows(EmployeeNotFoundException.class,
                () -> calculator.calculateNetPay("unknown", 40, 20));

        assertEquals(1, logged.size());
        assertEquals(Level.WARNING, logged.get(0).getLevel());
    }

    @Test
    void logsAnUnexpectedFailureAsAnErrorWithItsCause() {
        UncheckedIOException connectionLost =
                new UncheckedIOException("connection lost", new IOException("reset"));
        EmployeeRepository repository = mock(EmployeeRepository.class);
        when(repository.findById("e1")).thenThrow(connectionLost);

        assertThrows(UncheckedIOException.class,
                () -> calculatorUsing(repository).calculateNetPay("e1", 40, 20));

        assertEquals(1, logged.size());
        assertEquals(Level.SEVERE, logged.get(0).getLevel());
        assertSame(connectionLost, logged.get(0).getThrown());
    }

    @Test
    void logsNothingWhenPayIsCalculated() {
        calculator.calculateNetPay("e1", 40, 20);

        assertEquals(0, logged.size());
    }

    @Test
    void netPayIsWhatIsLeftOfGrossPayAfterTheTaxCalculatorsTax() {
        GrossPayCalculator grossPayCalculator = mock(GrossPayCalculator.class);
        TaxCalculator taxCalculator = mock(TaxCalculator.class);
        when(grossPayCalculator.calculateGrossPay(anyDouble(), anyDouble())).thenReturn(1000.0);
        when(taxCalculator.calculateTax(1000.0)).thenReturn(123.0);
        PayrollCalculator calculatorWithFixedFigures = new PayrollCalculator(
                new InMemoryEmployeeRepository(List.of(EFUA)), grossPayCalculator, taxCalculator);

        double netPay = calculatorWithFixedFigures.calculateNetPay("e1", 40, 20);

        assertEquals(877.0, netPay, PRECISION);
    }

    @Test
    void refusesToBeBuiltWithoutItsCollaborators() {
        EmployeeRepository repository = mock(EmployeeRepository.class);
        GrossPayCalculator grossPay = new GrossPayCalculator();
        TaxCalculator tax = new TaxCalculator(StandardTaxBands.current());

        assertThrows(NullPointerException.class,
                () -> new PayrollCalculator(null, grossPay, tax));
        assertThrows(NullPointerException.class,
                () -> new PayrollCalculator(repository, null, tax));
        assertThrows(NullPointerException.class,
                () -> new PayrollCalculator(repository, grossPay, null));
    }

    private static PayrollCalculator calculatorUsing(EmployeeRepository repository) {
        return new PayrollCalculator(repository, new GrossPayCalculator(),
                new TaxCalculator(StandardTaxBands.current()));
    }
}
