package com.iortatechnxt.brokerverse.submitted.processing.service;

import com.iortatechnxt.brokerverse.lov.domain.LovValue;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import com.iortatechnxt.brokerverse.submitted.domain.SbmLamdLoan;
import com.iortatechnxt.brokerverse.submitted.domain.SbmLamdLoanRepository;
import com.iortatechnxt.brokerverse.submitted.domain.SbmLoanMatch;
import com.iortatechnxt.brokerverse.submitted.domain.SbmLoanMatch.MatchKey;
import com.iortatechnxt.brokerverse.submitted.domain.SbmLoanMatch.RunRecord;
import com.iortatechnxt.brokerverse.submitted.domain.SbmLoanMatchRepository;
import com.iortatechnxt.brokerverse.submitted.domain.SbmPolicy;
import com.iortatechnxt.brokerverse.submitted.service.SubmittedCodes;
import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Matches a record with the loan files of the bank (FR-SP-035): on the PN number first, then on the
 * loan application number. Of the rows found, the newest file wins; two reports of the same day
 * follow the order of the list SBM_LOAN_REPORT (LAMD, then LMS, then LAD). Every match or non-match
 * is logged with the loan file, the key and the time stamp.
 */
@Component
@Transactional(propagation = Propagation.MANDATORY)
public class LoanMatcher {

  private static final int LAST = Integer.MAX_VALUE;

  private final SbmLamdLoanRepository loans;
  private final SbmLoanMatchRepository log;
  private final LovService lovs;
  private final Clock clock;

  /**
   * Creates the matcher.
   *
   * @param loans loan files
   * @param log matching log
   * @param lovs lists of values (order of the loan reports)
   * @param clock clock
   */
  public LoanMatcher(
      SbmLamdLoanRepository loans, SbmLoanMatchRepository log, LovService lovs, Clock clock) {
    this.loans = loans;
    this.log = log;
    this.lovs = lovs;
    this.clock = clock;
  }

  /**
   * Matches a record and logs the result.
   *
   * @param runId processing run
   * @param p record
   * @return the loan found, null when the record is unmatched
   */
  public SbmLamdLoan match(Long runId, SbmPolicy p) {
    RunRecord record = new RunRecord(p.getCompanyId(), runId, p.getId());
    String pn = p.getLoan().pnNo();
    String application = p.getLoan().loanApplicationNo();
    Map<String, Integer> order = precedence();
    Optional<SbmLamdLoan> byPn =
        pn == null
            ? Optional.empty()
            : newest(
                loans.findByCompanyIdAndPnNoOrderBySnapshotDateDescIdDesc(p.getCompanyId(), pn),
                order);
    if (byPn.isPresent()) {
      log.save(SbmLoanMatch.matched(record, clock.instant(), MatchKey.PN, byPn.get()));
      return byPn.get();
    }
    Optional<SbmLamdLoan> byApplication =
        application == null
            ? Optional.empty()
            : newest(
                loans.findByCompanyIdAndLoanApplicationNoOrderBySnapshotDateDescIdDesc(
                    p.getCompanyId(), application),
                order);
    byApplication.ifPresentOrElse(
        loan ->
            log.save(
                SbmLoanMatch.matched(record, clock.instant(), MatchKey.LOAN_APPLICATION, loan)),
        () -> log.save(SbmLoanMatch.unmatched(record, clock.instant(), reason(pn, application))));
    return byApplication.orElse(null);
  }

  static Optional<SbmLamdLoan> newest(List<SbmLamdLoan> rows, Map<String, Integer> order) {
    return rows.stream()
        .min(
            Comparator.comparing(SbmLamdLoan::getSnapshotDate, Comparator.reverseOrder())
                .thenComparing(l -> order.getOrDefault(l.getLoanReport(), LAST)));
  }

  static String reason(String pn, String application) {
    if (pn == null && application == null) {
      return "The record has no PN number and no loan application number";
    }
    return "No loan file holds "
        + (pn == null ? "" : "PN number " + pn)
        + (pn != null && application != null ? " or " : "")
        + (application == null ? "" : "loan application number " + application);
  }

  private Map<String, Integer> precedence() {
    return lovs.values(SubmittedCodes.LOV_LOAN_REPORT).stream()
        .collect(Collectors.toMap(LovValue::getCode, LovValue::getSortOrder, (a, b) -> a));
  }
}
