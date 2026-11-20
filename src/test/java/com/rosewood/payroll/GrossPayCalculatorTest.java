package com.rosewood.payroll;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class GrossPayCalculatorTest {

    private static final double PRECISION = 0.000001;

    private final GrossPayCalculator calculator = new GrossPayCalculator();

    @ParameterizedTest(name = "{0}")
    @CsvSource({
        "a standard week has no overtime,         40,     20,   800.0",
        "part-time hours,                         37.5,   25,   937.5",
        "no hours worked,                         0,      20,   0.0",
        "zero hourly rate,                        40,     0,    0.0",
        "one overtime hour is paid at 1.5x,       41,     20,   830.0",
        "overtime starts only after 40 hours,     40.5,   20,   815.0",
        "a long week with overtime,               50,     30,   1650.0",
        "overtime on a large amount,              100,    100,  13000.0"
    })
    void calculatesHoursTimesRatePlusOvertimePremium(
            String scenario, double hours, double rate, double expectedGross) {
        assertEquals(expectedGross, calculator.calculateGrossPay(hours, rate), PRECISION);
    }

    @ParameterizedTest
    @ValueSource(doubles = {-0.01, -5, Double.NaN, Double.POSITIVE_INFINITY})
    void rejectsHoursThatCannotBePaid(double badHours) {
        InvalidPayInputException failure = assertThrows(InvalidPayInputException.class,
                () -> calculator.calculateGrossPay(badHours, 20));

        assertEquals("Hours worked", failure.getField());
        assertEquals(badHours, failure.getValue());
    }

    @ParameterizedTest
    @ValueSource(doubles = {-0.01, -20, Double.NaN, Double.POSITIVE_INFINITY})
    void rejectsAHourlyRateThatCannotBePaid(double badRate) {
        InvalidPayInputException failure = assertThrows(InvalidPayInputException.class,
                () -> calculator.calculateGrossPay(40, badRate));

        assertEquals("Hourly rate", failure.getField());
        assertEquals(badRate, failure.getValue());
    }
}
