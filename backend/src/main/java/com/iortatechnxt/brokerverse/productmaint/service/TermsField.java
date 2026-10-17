package com.iortatechnxt.brokerverse.productmaint.service;

import java.util.Arrays;
import java.util.List;

/**
 * The comparison fields of BDOI's comparative table (FRPM.006.02 and FRPM.012.02): the fields a
 * user may choose as the rows of the table; the quotation options of an insurer carry a value per
 * field.
 */
public enum TermsField {
  /** Coverage or perils. */
  COVERAGE("Coverage"),
  /** Sum insured. */
  SUM_INSURED("Sum Insured"),
  /** Period of insurance. */
  PERIOD("Period of Insurance"),
  /** Premium. */
  PREMIUM("Premium"),
  /** Premium rate. */
  RATE("Premium Rate (%)"),
  /** Commission rate. */
  COMMISSION("Commission Rate (%)"),
  /** Deductibles. */
  DEDUCTIBLES("Deductibles"),
  /** Terms and conditions. */
  CONDITIONS("Terms and Conditions"),
  /** Warranties. */
  WARRANTIES("Warranties"),
  /** Clauses. */
  CLAUSES("Clauses"),
  /** Extensions of cover. */
  EXTENSIONS("Extensions of Cover"),
  /** Other quotation details. */
  OTHER("Other Quotation Details");

  private final String label;

  TermsField(String label) {
    this.label = label;
  }

  /**
   * Label of the row.
   *
   * @return label
   */
  public String label() {
    return label;
  }

  /**
   * The keys of every field, in order.
   *
   * @return keys
   */
  public static List<String> keys() {
    return Arrays.stream(values()).map(Enum::name).toList();
  }

  /**
   * Whether a key names a field.
   *
   * @param key key
   * @return true when known
   */
  public static boolean known(String key) {
    return Arrays.stream(values()).anyMatch(f -> f.name().equals(key));
  }
}
