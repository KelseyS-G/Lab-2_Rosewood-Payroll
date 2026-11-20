package com.rosewood.payroll;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class TaxCalculatorTest {

    private static final double PRECISION = 0.000001;

    private final TaxCalculator calculator = TaxCalculator.withStandardBands();

    @ParameterizedTest(name = "{0}")
    @CsvSource({
        "zero gross pay,                 0,        0.0",
        "inside LOW (5%),                800,      40.0",
        "last pay in LOW,                999.99,   49.9995",
        "first pay in MEDIUM (15%),      1000,     150.0",
        "last pay in MEDIUM,             4999.99,  749.9985",
        "first pay in HIGH (25%),        5000,     1250.0",
        "well inside HIGH,               13000,    3250.0"
    })
    void taxesTheWholeGrossPayAtTheRateOfItsBand(
            String scenario, double grossPay, double expectedTax) {
        assertEquals(expectedTax, calculator.calculateTax(grossPay), PRECISION);
    }

    @Test
    void doesNotDependOnTheOrderTheBandsAreListedIn() {
        TaxCalculator reordered = new TaxCalculator(List.of(
                new TaxBand("HIGH", 5000, 0.25),
                new TaxBand("LOW", 0, 0.05),
                new TaxBand("MEDIUM", 1000, 0.15)));

        assertEquals(150.0, reordered.calculateTax(1000), PRECISION);
    }

    @Test
    void aNewBandIsJustAnotherEntryAndLeavesTheOthersUntouched() {
        List<TaxBand> bands = new ArrayList<>(List.of(
                new TaxBand("LOW", 0, 0.05),
                new TaxBand("MEDIUM", 1000, 0.15),
                new TaxBand("HIGH", 5000, 0.25)));
        bands.add(new TaxBand("TOP", 20000, 0.30));
        TaxCalculator withNewBand = new TaxCalculator(bands);

        assertEquals(6000.0, withNewBand.calculateTax(20000), PRECISION);
        assertEquals(4999.9975, withNewBand.calculateTax(19999.99), PRECISION);
        assertEquals(150.0, withNewBand.calculateTax(1000), PRECISION);
    }

    @Test
    void failsLoudlyWhenNoBandCoversTheGrossPay() {
        TaxCalculator withoutLowBand =
                new TaxCalculator(List.of(new TaxBand("MEDIUM", 1000, 0.15)));

        IllegalStateException failure =
                assertThrows(IllegalStateException.class, () -> withoutLowBand.calculateTax(500));

        assertTrue(failure.getMessage().contains("500"));
    }
}
