package com.iortatechnxt.finverse.tax.domain;

/**
 * How an IC line item reads the ledger: the closing BALANCE as of the report date, or the MOVEMENT
 * of the report period (year-end closing journals excluded, so income accounts keep their movement
 * after the year is closed).
 */
public enum IcMeasure {
  BALANCE,
  MOVEMENT
}
