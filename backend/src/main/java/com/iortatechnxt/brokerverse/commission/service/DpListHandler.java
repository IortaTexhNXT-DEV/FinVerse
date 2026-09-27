package com.iortatechnxt.brokerverse.commission.service;

import com.iortatechnxt.brokerverse.bulk.service.BulkFileReader;
import com.iortatechnxt.brokerverse.bulk.service.ParsedFile;
import com.iortatechnxt.brokerverse.bulk.service.ParsedFile.RawRow;
import com.iortatechnxt.brokerverse.commission.domain.CommissionEnums.ListSource;
import com.iortatechnxt.brokerverse.commission.domain.DpItem.Submission;
import com.iortatechnxt.brokerverse.commission.domain.DpList;
import com.iortatechnxt.brokerverse.commission.domain.DpList.FileKey;
import com.iortatechnxt.brokerverse.commission.domain.DpList.Origin;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.util.Sha256;
import com.iortatechnxt.brokerverse.opsledger.service.FlowInContext;
import com.iortatechnxt.brokerverse.opsledger.service.FlowInHandler;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Handler of the flow-in feed {@code COLLECTION_DP_LIST} (CMRID.001): a DP list of Head Office or a
 * branch named {@code <Branch>_DP_<yyyyMMdd>} (XLSX, CSV or TXT) with the columns Invoice No.,
 * Policy No., Insurer, Premium and Remarks. A file breaking the naming convention or already taken
 * in is refused; each account is one flow-in record, validated and sanitised on intake. The shared
 * folders and the Collection system are parked (OQ38): lists are uploaded.
 */
@Component
public class DpListHandler implements FlowInHandler {

  /** Feed code. */
  public static final String FEED = "COLLECTION_DP_LIST";

  /** Head Office branch code in the file names. */
  public static final String HEAD_OFFICE = "HO";

  static final String INVOICE = "Invoice No.";
  static final String POLICY = "Policy No.";
  static final String INSURER = "Insurer";
  static final String PREMIUM = "Premium";
  static final String REMARKS = "Remarks";

  private static final Pattern NAME =
      Pattern.compile(
          "^([A-Za-z0-9][A-Za-z0-9 .-]{0,58})_DP_(\\d{8})\\.(xlsx|csv|txt|ods)$",
          Pattern.CASE_INSENSITIVE);
  private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyyMMdd");

  private final BulkFileReader reader;
  private final DpIntakeService intake;

  /**
   * Creates the handler.
   *
   * @param reader file reader
   * @param intake list intake
   */
  public DpListHandler(BulkFileReader reader, DpIntakeService intake) {
    this.reader = reader;
    this.intake = intake;
  }

  @Override
  public String feedCode() {
    return FEED;
  }

  @Override
  public void handle(FlowInFile file, FlowInContext context) {
    process(null, file, context);
  }

  /**
   * Takes a list in for a company, or for the company of its first booked invoice when the file
   * comes from Interfaces.
   *
   * @param companyId company, may be null
   * @param file file
   * @param context flow-in run
   * @return the list
   */
  public DpList process(Long companyId, FlowInFile file, FlowInContext context) {
    Origin origin = origin(file.fileName());
    ParsedFile parsed = reader.read(file.fileName(), file.content());
    if (!parsed.headers().contains(INVOICE)) {
      throw new BusinessRuleException(
          "DP_LIST_LAYOUT", "The DP list has no '" + INVOICE + "' column");
    }
    Long company = companyId == null ? companyOf(parsed) : companyId;
    DpList list =
        intake.open(company, origin, new FileKey(file.fileName(), Sha256.hex(file.content())));
    for (RawRow row : parsed.rows()) {
      context.accept(
          list.getListNo() + ":" + row.rowNo(),
          row.values().toString(),
          () -> intake.add(list.getId(), row.rowNo(), submission(row.values(), origin)));
    }
    return intake.finish(list.getId(), context.runNo());
  }

  /**
   * Branch and date of a list from its file name (CMRID.001 naming convention).
   *
   * @param fileName file name
   * @return origin
   */
  static Origin origin(String fileName) {
    Matcher m = NAME.matcher(fileName == null ? "" : fileName.strip());
    if (!m.matches()) {
      throw new BusinessRuleException(
          "DP_LIST_NAME",
          "Name the DP list <Branch>_DP_<yyyyMMdd> (e.g. HO_DP_20260930.xlsx): " + fileName);
    }
    LocalDate date;
    try {
      date = LocalDate.parse(m.group(2), DAY);
    } catch (DateTimeParseException ex) {
      throw new BusinessRuleException(
          "DP_LIST_NAME", "The date in " + fileName + " is not a date (yyyyMMdd)", ex);
    }
    String raw = m.group(1).strip();
    boolean headOffice =
        raw.length() == HEAD_OFFICE.length()
            && HEAD_OFFICE.regionMatches(true, 0, raw, 0, raw.length());
    return new Origin(
        headOffice ? ListSource.HEAD_OFFICE : ListSource.BRANCH,
        raw.toUpperCase(Locale.ROOT),
        date);
  }

  private Long companyOf(ParsedFile parsed) {
    return parsed.rows().stream()
        .map(r -> r.values().get(INVOICE))
        .filter(v -> v != null && !v.isBlank())
        .map(v -> intake.companyOf(v.strip()))
        .flatMap(Optional::stream)
        .findFirst()
        .orElseThrow(
            () ->
                new BusinessRuleException(
                    "DP_LIST_COMPANY",
                    "No invoice of the list is booked: upload it from Commission Receivables"));
  }

  /**
   * An account of a list row.
   *
   * @param values row values
   * @param origin list origin
   * @return submission
   */
  static Submission submission(Map<String, String> values, Origin origin) {
    String invoice = text(values, INVOICE);
    if (invoice == null) {
      throw new BusinessRuleException("DP_ROW_INCOMPLETE", "The row has no invoice number");
    }
    String premium = text(values, PREMIUM);
    BigDecimal amount;
    try {
      amount = premium == null ? null : new BigDecimal(premium.replace(",", ""));
    } catch (NumberFormatException ex) {
      throw new BusinessRuleException(
          "DP_ROW_PREMIUM", "Premium '" + premium + "' is not an amount", ex);
    }
    String insurer = text(values, INSURER);
    return new Submission(
        invoice,
        text(values, POLICY),
        insurer == null ? null : insurer.toUpperCase(Locale.ROOT),
        amount,
        text(values, REMARKS),
        origin.branchCode());
  }

  private static String text(Map<String, String> values, String header) {
    String v = values.get(header);
    return v == null || v.isBlank() ? null : v.strip();
  }
}
