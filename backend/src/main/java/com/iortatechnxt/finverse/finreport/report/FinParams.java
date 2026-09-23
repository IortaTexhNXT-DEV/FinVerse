package com.iortatechnxt.finverse.finreport.report;

import com.iortatechnxt.finverse.finreport.service.StatusFilter;
import com.iortatechnxt.finverse.report.core.ParameterSpec;
import com.iortatechnxt.finverse.report.core.ParameterType;
import com.iortatechnxt.finverse.report.core.ReportParameters;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Parameter names and declarations shared by the finance reports. Range parameters follow the
 * FinVerse convention: a blank bound means "all" (PREMIA 0 / zzzz).
 */
public final class FinParams {

  /** Company. */
  public static final String COMPANY = "companyId";

  /** Branch (division). */
  public static final String BRANCH = "branchId";

  /** Cost centre (department). */
  public static final String COST_CENTER = "costCenter";

  /** Calendar month 1-12. */
  public static final String MONTH = "month";

  /** Calendar year. */
  public static final String YEAR = "year";

  /** Period start. */
  public static final String FROM = "fromDate";

  /** Period end. */
  public static final String TO = "toDate";

  /** As-of date. */
  public static final String AS_OF = "asOfDate";

  /** Main account range start. */
  public static final String MAIN_FROM = "mainFrom";

  /** Main account range end. */
  public static final String MAIN_TO = "mainTo";

  /** Sub account range start. */
  public static final String SUB_FROM = "subFrom";

  /** Sub account range end. */
  public static final String SUB_TO = "subTo";

  /** Transaction code range start. */
  public static final String TXN_FROM = "txnFrom";

  /** Transaction code range end. */
  public static final String TXN_TO = "txnTo";

  /** Document number range start. */
  public static final String DOC_FROM = "docFrom";

  /** Document number range end. */
  public static final String DOC_TO = "docTo";

  /** Transaction status (posted / unposted / both). */
  public static final String STATUS = "status";

  /** Entered-by user. */
  public static final String USER = "userId";

  /** Combine flag. */
  public static final String COMBINE = "combine";

  /** Order by option. */
  public static final String ORDER_BY = "orderBy";

  /** Order by document date. */
  public static final String BY_DATE = "DOCUMENT_DATE";

  /** Order by document number. */
  public static final String BY_NUMBER = "DOCUMENT_NUMBER";

  private static final int MONTHS = 12;

  private FinParams() {}

  /**
   * Company (mandatory).
   *
   * @return spec
   */
  public static ParameterSpec company() {
    return ParameterSpec.required(COMPANY, "Company", ParameterType.COMPANY);
  }

  /**
   * Division = branch (optional).
   *
   * @return spec
   */
  public static ParameterSpec division() {
    return ParameterSpec.optional(BRANCH, "Division (Branch)", ParameterType.BRANCH);
  }

  /**
   * Department = cost centre (optional).
   *
   * @return spec
   */
  public static ParameterSpec department() {
    return ParameterSpec.optional(COST_CENTER, "Department (Cost Centre)", ParameterType.TEXT);
  }

  /**
   * Calendar month defaulting to the current month.
   *
   * @param clock clock
   * @return spec
   */
  public static ParameterSpec month(Clock clock) {
    List<String> months = IntStream.rangeClosed(1, MONTHS).mapToObj(String::valueOf).toList();
    return ParameterSpec.select(
        MONTH, "Calendar Month", months, String.valueOf(LocalDate.now(clock).getMonthValue()));
  }

  /**
   * Calendar year defaulting to the current year.
   *
   * @param clock clock
   * @return spec
   */
  public static ParameterSpec year(Clock clock) {
    return ParameterSpec.required(YEAR, "Calendar Year", ParameterType.NUMBER)
        .withDefault(String.valueOf(LocalDate.now(clock).getYear()));
  }

  /**
   * Document date from, defaulting to the month start.
   *
   * @return spec
   */
  public static ParameterSpec from() {
    return ParameterSpec.required(FROM, "Document Date From", ParameterType.DATE)
        .withDefault("MONTH_START");
  }

  /**
   * Document date to, defaulting to today.
   *
   * @return spec
   */
  public static ParameterSpec to() {
    return ParameterSpec.required(TO, "Document Date To", ParameterType.DATE).withDefault("TODAY");
  }

  /**
   * As-of date defaulting to today.
   *
   * @return spec
   */
  public static ParameterSpec asOf() {
    return ParameterSpec.required(AS_OF, "As of Date", ParameterType.DATE).withDefault("TODAY");
  }

  /**
   * Main account code range.
   *
   * @return two specs
   */
  public static List<ParameterSpec> mainRange() {
    return List.of(
        ParameterSpec.optional(MAIN_FROM, "Main A/c Code From", ParameterType.ACCOUNT),
        ParameterSpec.optional(MAIN_TO, "Main A/c Code To", ParameterType.ACCOUNT));
  }

  /**
   * Sub account code range.
   *
   * @return two specs
   */
  public static List<ParameterSpec> subRange() {
    return List.of(
        ParameterSpec.optional(SUB_FROM, "Sub A/c Code From", ParameterType.ACCOUNT),
        ParameterSpec.optional(SUB_TO, "Sub A/c Code To", ParameterType.ACCOUNT));
  }

  /**
   * Transaction code (voucher prefix, e.g. JV, RCT) range.
   *
   * @return two specs
   */
  public static List<ParameterSpec> txnRange() {
    return List.of(
        ParameterSpec.optional(TXN_FROM, "Transaction Code From", ParameterType.TEXT),
        ParameterSpec.optional(TXN_TO, "Transaction Code To", ParameterType.TEXT));
  }

  /**
   * Document (voucher) number range.
   *
   * @return two specs
   */
  public static List<ParameterSpec> docRange() {
    return List.of(
        ParameterSpec.optional(DOC_FROM, "Document Number From", ParameterType.TEXT),
        ParameterSpec.optional(DOC_TO, "Document Number To", ParameterType.TEXT));
  }

  /**
   * Transaction status option.
   *
   * @param defaultValue default option
   * @return spec
   */
  public static ParameterSpec status(StatusFilter defaultValue) {
    return ParameterSpec.select(
        STATUS, "Transaction Status", StatusFilter.options(), defaultValue.name());
  }

  /**
   * Entered-by user.
   *
   * @return spec
   */
  public static ParameterSpec user() {
    return ParameterSpec.optional(USER, "User ID", ParameterType.TEXT);
  }

  /**
   * Order-by option (document date or number).
   *
   * @return spec
   */
  public static ParameterSpec orderBy() {
    return ParameterSpec.select(ORDER_BY, "Order By", List.of(BY_DATE, BY_NUMBER), BY_DATE);
  }

  /**
   * Flag parameter.
   *
   * @param name name
   * @param label label
   * @return spec
   */
  public static ParameterSpec flag(String name, String label) {
    return ParameterSpec.optional(name, label, ParameterType.BOOLEAN);
  }

  /**
   * Selected status option.
   *
   * @param p parameters
   * @return status filter
   */
  public static StatusFilter status(ReportParameters p) {
    return StatusFilter.valueOf(p.text(STATUS));
  }

  /**
   * Selected branch or null.
   *
   * @param p parameters
   * @return branch id
   */
  public static Long branch(ReportParameters p) {
    return p.optionalLong(BRANCH).orElse(null);
  }

  /**
   * Selected cost centre or null.
   *
   * @param p parameters
   * @return cost centre
   */
  public static String costCenter(ReportParameters p) {
    return p.optionalText(COST_CENTER).orElse(null);
  }
}
