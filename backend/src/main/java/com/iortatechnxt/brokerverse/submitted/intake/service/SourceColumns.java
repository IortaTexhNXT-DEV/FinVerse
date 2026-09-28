package com.iortatechnxt.brokerverse.submitted.intake.service;

import com.iortatechnxt.brokerverse.bulk.service.BulkColumn;
import com.iortatechnxt.brokerverse.bulk.service.BulkColumn.Type;
import com.iortatechnxt.brokerverse.bulk.service.BulkImportHandler;
import com.iortatechnxt.brokerverse.bulk.service.BulkRow;
import com.iortatechnxt.brokerverse.common.excel.GuideColumn.Choice;
import com.iortatechnxt.brokerverse.submitted.domain.SbmAssured;
import com.iortatechnxt.brokerverse.submitted.domain.SbmBusinessType;
import com.iortatechnxt.brokerverse.submitted.domain.SbmLoan;
import com.iortatechnxt.brokerverse.submitted.domain.SbmMarks;
import com.iortatechnxt.brokerverse.submitted.domain.SbmPolicyData;
import com.iortatechnxt.brokerverse.submitted.domain.SbmRisk;
import com.iortatechnxt.brokerverse.submitted.domain.SbmTerms;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The columns of the source files of submitted policies (BRIDSP-01; the fields of Report List #151)
 * and their mapping to the masterlist data. One layout serves every source until BDOI supplies the
 * layouts of each (SP SQ01, SQ02): a source with a fixed segment or business type fills those when
 * the file leaves them blank.
 */
public final class SourceColumns {

  /** Segment. */
  public static final String SEGMENT = "Segment";

  /** Business type. */
  public static final String BUSINESS_TYPE = "Business Type";

  /** PN. */
  public static final String PN = "PN No";

  /** Assured. */
  public static final String ASSURED = "Assured";

  /** Expiry. */
  public static final String EXPIRY = "Expiry Date";

  /** Handler. */
  public static final String HANDLER = "Handler";

  private static final String YEAR_MODEL = "Year Model";

  private SourceColumns() {}

  /**
   * The template columns.
   *
   * @return columns
   */
  public static List<BulkColumn> columns() {
    List<BulkColumn> c = new ArrayList<>();
    c.add(
        BulkColumn.optional(
                SEGMENT, "Segment of the policy; blank for the segment of the source", "CBG Motor")
            .values("CBG Motor", "CBG Fire", "Non-CBG Corporate", "Non-CBG Retail"));
    c.add(
        BulkColumn.optional(BUSINESS_TYPE, "New or renewal business; blank for NB", "NB")
            .choices(
                List.of(new Choice("NB", "New business"), new Choice("RB", "Renewal business"))));
    c.add(
        BulkColumn.optional(PN, "Promissory note number of the loan", "PN-2026-000123")
            .when("the segment is CBG Motor or CBG Fire"));
    c.add(BulkColumn.optional("Loan Application No", "Loan application number", "LA-778812"));
    c.add(BulkColumn.optional("CIF", "Bank client number", "CIF-00012345"));
    c.add(date("Value Date", "Loan value date"));
    c.add(date("Maturity Date", "Loan maturity date"));
    c.add(BulkColumn.optional("Referring Branch", "Referring branch", "Makati Ayala"));
    c.add(BulkColumn.optional("Originating Unit", "Originating unit of the loan", "Auto Loans"));
    c.add(BulkColumn.optional("Borrower", "Borrower name", "Juan Dela Cruz"));
    c.add(BulkColumn.required(ASSURED, "Assured (client) name", "Juan Dela Cruz"));
    c.add(
        BulkColumn.optional(
            "Mailing Address", "Mailing address of the assured", "12 Rizal St, Makati City"));
    c.add(BulkColumn.optional("Telephone", "Telephone", "02 8888 1234"));
    c.add(BulkColumn.optional("Mobile", "Mobile number", "0917 123 4567"));
    c.add(BulkColumn.optional("Email", "E-mail of the assured", "juan@example.ph"));
    c.add(
        BulkColumn.optional(
            "Bank Counterpart Email", "E-mail of the bank counterpart", "loans@bank.ph"));
    c.add(
        BulkColumn.optional("Insurer Code", "Insurer of the policy", "INS-MGIC").master("insurer"));
    c.add(BulkColumn.optional("Policy No", "Policy number", "MC-2026-001234"));
    c.add(date("Inception Date", "Policy inception"));
    c.add(new BulkColumn(EXPIRY, "Policy expiry", true, Type.DATE, "2027-03-31"));
    c.add(number("Sum Insured", "Amount insured", "850000.00"));
    c.add(number("Total Premium", "Total premium", "21500.00"));
    c.add(
        BulkColumn.optional(
            "Unit Description", "Vehicle make and model (motor)", "Toyota Vios 1.3 XLE"));
    c.add(BulkColumn.optional("Serial No", "Chassis or serial number (motor)", "MHFA1234567"));
    c.add(BulkColumn.optional("Motor No", "Engine or motor number (motor)", "2NR1234567"));
    c.add(BulkColumn.optional("Colour", "Colour (motor)", "White"));
    c.add(BulkColumn.optional("Plate No", "Plate number (motor)", "ABC 1234"));
    c.add(
        BulkColumn.optional(
            "Vehicle Type", "Vehicle type (motor), as in the insurer rules", "Private car"));
    c.add(number(YEAR_MODEL, "Year model (motor)", "2023"));
    c.add(
        BulkColumn.optional(
            "Property Location", "Location of the property (fire)", "Lot 5, Taguig City"));
    c.add(BulkColumn.optional("Occupancy", "Occupancy (fire)", "Residential"));
    c.add(BulkColumn.optional("Mortgagee", "Mortgagee", "BDO Unibank, Inc."));
    c.add(yesNo("FFY", "Y when the auto loan has the Free First Year promotion"));
    c.add(yesNo("Employee Account", "Y for a BDO or SM Group employee account"));
    c.add(yesNo("No Touch", "Y for a No Touch account"));
    c.add(
        BulkColumn.optional(HANDLER, "User ID of the handler of the policy", "sbmhandler")
            .allowed("User ID of an active user of Submitted Policies"));
    return List.copyOf(c);
  }

  private static BulkColumn date(String header, String description) {
    return new BulkColumn(header, description, false, Type.DATE, "2026-04-01");
  }

  private static BulkColumn number(String header, String description, String example) {
    return new BulkColumn(header, description, false, Type.NUMBER, example);
  }

  private static BulkColumn yesNo(String header, String description) {
    return new BulkColumn(header, description, false, Type.YES_NO, "N");
  }

  /**
   * Identifiers upper-cased without spaces.
   *
   * @param header column
   * @param value value
   * @return clean value
   */
  public static String sanitize(String header, String value) {
    String v = value.trim().replaceAll("\\s+", " ");
    return switch (header) {
      case PN, "Serial No", "Motor No" -> BulkImportHandler.identifier(v);
      case SEGMENT -> segmentCode(v);
      case BUSINESS_TYPE -> v.toUpperCase(Locale.ROOT);
      default -> v;
    };
  }

  /**
   * The segment code of a segment written as its code or its name.
   *
   * @param value code or name (for example "CBG Motor")
   * @return code
   */
  public static String segmentCode(String value) {
    String code =
        value.toUpperCase(Locale.ROOT).replaceAll("[\\s-]+", "_").replace("NON_CBG", "NONCBG");
    return code.startsWith("NONCBG_CORPORATE") ? "NONCBG_CORPORATE" : code;
  }

  /**
   * The masterlist data of a row.
   *
   * @param row row
   * @param defaultSegment segment of the source when the row has none
   * @param defaultBusinessType business type of the source when the row has none
   * @return data
   */
  public static SbmPolicyData data(BulkRow row, String defaultSegment, String defaultBusinessType) {
    String segment = orElse(row.text(SEGMENT), defaultSegment);
    String type = orElse(row.text(BUSINESS_TYPE), orElse(defaultBusinessType, "NB"));
    BigDecimal year = row.number(YEAR_MODEL);
    return new SbmPolicyData(
        segment,
        SbmBusinessType.valueOf(type),
        new SbmLoan(
            row.text(PN),
            row.text("Loan Application No"),
            row.text("CIF"),
            row.date("Value Date"),
            row.date("Maturity Date"),
            row.text("Referring Branch"),
            row.text("Originating Unit"),
            row.text("Borrower")),
        new SbmAssured(
            row.text(ASSURED),
            row.text("Mailing Address"),
            row.text("Telephone"),
            row.text("Mobile"),
            row.text("Email"),
            row.text("Bank Counterpart Email")),
        new SbmTerms(
            row.text("Insurer Code"),
            row.text("Policy No"),
            row.date("Inception Date"),
            row.date(EXPIRY),
            null,
            row.number("Sum Insured"),
            row.number("Total Premium"),
            "PHP"),
        new SbmRisk(
            row.text("Unit Description"),
            row.text("Serial No"),
            row.text("Motor No"),
            row.text("Colour"),
            row.text("Plate No"),
            row.text("Vehicle Type"),
            year == null ? null : year.intValue(),
            row.text("Property Location"),
            row.text("Occupancy"),
            row.text("Mortgagee")),
        new SbmMarks(row.yes("FFY"), row.yes("Employee Account"), row.yes("No Touch")));
  }

  /**
   * Whether a business type code is valid.
   *
   * @param value value, may be null
   * @return true for NB, RB or blank
   */
  public static boolean validBusinessType(String value) {
    return value == null || "NB".equals(value) || "RB".equals(value);
  }

  private static String orElse(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value;
  }
}
