package com.rosewood.payroll;

/**
 * One band of the tax table: gross pay from {@code startsAt} (inclusive) up to the start of
 * the next higher band is taxed at {@code rate} (0.15 means 15%).
 */
public record TaxBand(String name, double startsAt, double rate) {
}
