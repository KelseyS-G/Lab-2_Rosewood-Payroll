package com.rosewood.payroll;

import java.util.List;

/**
 * Rosewood's tax table, kept apart from the calculation. To add a band, add one line here.
 * Nothing in {@link TaxCalculator} or {@link PayrollCalculator} needs to change.
 */
public final class StandardTaxBands {

    private StandardTaxBands() {
    }

    public static List<TaxBand> current() {
        return List.of(
                new TaxBand("LOW", 0, 0.05),
                new TaxBand("MEDIUM", 1000, 0.15),
                new TaxBand("HIGH", 5000, 0.25));
    }
}
