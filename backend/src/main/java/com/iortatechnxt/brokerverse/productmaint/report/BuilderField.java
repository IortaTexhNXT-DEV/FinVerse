package com.iortatechnxt.brokerverse.productmaint.report;

import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import java.util.Arrays;
import java.util.List;

/**
 * The fields of the Product Maintenance report builder (BDOI FRS FRPM.020.01): the columns a user
 * may choose for a custom report of quotation requests, package requests or products; a field a
 * source does not carry stays blank.
 */
public enum BuilderField {
  /** Request number or risk code. */
  REFERENCE("Reference No.", false),
  /** Request date. */
  REQUEST_DATE("Request Date", true),
  /** Client or insured. */
  CLIENT("Client / Insured", false),
  /** Line of insurance. */
  LINE("Line of Insurance", false),
  /** Sub-line. */
  SUB_LINE("Sub-Line", false),
  /** Product or package. */
  PRODUCT("Product / Package", false),
  /** Account officer. */
  OFFICER("Account Officer", false),
  /** TSU handler. */
  TSU_USER("TSU Handler", false),
  /** Status. */
  STATUS("Status", false),
  /** Submission date. */
  SUBMITTED("Submission Date", true),
  /** Effective date. */
  EFFECTIVE("Effective Date", true),
  /** Expiry date. */
  EXPIRY("Expiry Date", true),
  /** Aging in days. */
  AGING("Aging (Days)", false),
  /** Insurers. */
  INSURERS("Insurer", false),
  /** Business or request type. */
  TYPE("Type", false);

  private final String label;
  private final boolean date;

  BuilderField(String label, boolean date) {
    this.label = label;
    this.date = date;
  }

  /**
   * Column header.
   *
   * @return label
   */
  public String label() {
    return label;
  }

  /**
   * The report column of the field.
   *
   * @return column
   */
  public ReportColumn column() {
    return date ? ReportColumn.date(name(), label) : ReportColumn.text(name(), label);
  }

  /**
   * The default columns of a new report.
   *
   * @return fields
   */
  public static List<BuilderField> defaults() {
    return List.of(REFERENCE, REQUEST_DATE, CLIENT, LINE, OFFICER, STATUS);
  }

  /**
   * Every field, in order.
   *
   * @return fields
   */
  public static List<BuilderField> all() {
    return Arrays.asList(values());
  }
}
