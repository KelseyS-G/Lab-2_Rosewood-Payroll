package com.rosewood.payroll;

import java.util.Comparator;
import java.util.List;

/**
 * Applies the tax band that matches a gross pay. The bands are data: to add a band, add a
 * {@link TaxBand} to the list this class is built with. No calculation code changes.
 */
public class TaxCalculator {

    private final List<TaxBand> bands;

    public TaxCalculator(List<TaxBand> bands) {
        this.bands = List.copyOf(bands);
    }

    public static TaxCalculator withStandardBands() {
        return new TaxCalculator(List.of(
                new TaxBand("LOW", 0, 0.05),
                new TaxBand("MEDIUM", 1000, 0.15),
                new TaxBand("HIGH", 5000, 0.25)));
    }

    public double calculateTax(double grossPay) {
        return grossPay * bandFor(grossPay).rate();
    }

    private TaxBand bandFor(double grossPay) {
        return bands.stream()
                .filter(band -> band.startsAt() <= grossPay)
                .max(Comparator.comparingDouble(TaxBand::startsAt))
                .orElseThrow(() -> new IllegalStateException(
                        "No tax band covers a gross pay of " + grossPay));
    }
}
