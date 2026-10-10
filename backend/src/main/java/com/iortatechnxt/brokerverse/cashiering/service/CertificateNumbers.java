package com.iortatechnxt.brokerverse.cashiering.service;

import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.brokerverse.organization.domain.BranchRepository;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * The certificate number of a printed receipt (FRS.CSH.02.04.07; Appendix R, C4): setting {@code
 * CASH_CERTIFICATE_NO_FORMAT}, by default {@code AC_{BRANCH}_{MMYYYY}_{SEQ}} as on the sample AR
 * (AC_125_022025_00482): the branch indicator, the month and year of the print date and a five
 * digit sequence of the branch and month, unique and never reused.
 */
@Component
public class CertificateNumbers {

  /** Setting of the format. */
  public static final String FORMAT = "CASH_CERTIFICATE_NO_FORMAT";

  private static final String DEFAULT = "AC_{BRANCH}_{MMYYYY}_{SEQ}";
  private static final String SEQ = "{SEQ}";
  private static final int DIGITS = 5;

  private final DocumentNumberService numbers;
  private final BranchRepository branches;
  private final SystemParameterService parameters;

  /**
   * Creates the numbering.
   *
   * @param numbers sequences
   * @param branches branch indicator
   * @param parameters format
   */
  public CertificateNumbers(
      DocumentNumberService numbers, BranchRepository branches, SystemParameterService parameters) {
    this.numbers = numbers;
    this.branches = branches;
    this.parameters = parameters;
  }

  /**
   * The next certificate number of a branch on a print date.
   *
   * @param branchId branch of the receipt
   * @param printDate print date
   * @return certificate number
   */
  @Transactional(propagation = Propagation.MANDATORY)
  public String next(Long branchId, LocalDate printDate) {
    String format = parameters.text(FORMAT, "").strip();
    if (format.isEmpty() || !format.contains(SEQ)) {
      format = DEFAULT;
    }
    String branch =
        branchId == null ? "HO" : branches.findById(branchId).map(b -> b.getCode()).orElse("HO");
    String stem =
        format
            .replace("{BRANCH}", branch)
            .replace(
                "{MMYYYY}", printDate.format(DateTimeFormatter.ofPattern("MMyyyy", Locale.ROOT)))
            .replace("{YEAR}", String.valueOf(printDate.getYear()));
    String allocated = numbers.next("CERT:" + stem.replace(SEQ, ""));
    long sequence = Long.parseLong(allocated.substring(allocated.lastIndexOf('-') + 1));
    return stem.replace(SEQ, String.format(Locale.ROOT, "%0" + DIGITS + "d", sequence));
  }
}
