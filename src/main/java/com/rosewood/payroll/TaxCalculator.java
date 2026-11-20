package com.rosewood.payroll;

import java.util.Comparator;
import java.util.List;

/**
 * Applies the tax band that matches a gross pay. The bands are data (see
 * {@link StandardTaxBands}), so this class holds no rates of its own.
 */
public class TaxCalculator {

    private final List<TaxBand> bands;

    public TaxCalculator(List<TaxBand> bands) {
        this.bands = List.copyOf(bands);
    }

    public double calculateTax(double grossPay) {
        return grossPay * bandFor(grossPay).rate();
    }

    private TaxBand bandFor(double grossPay) {
        return bands.stream()
                .filter(band -> band.startsAt() <= grossPay)
                .max(Comparator.comparingDouble(TaxBand::startsAt))
                .orElseThrow(() -> new TaxBandNotFoundException(grossPay));
    }
}
