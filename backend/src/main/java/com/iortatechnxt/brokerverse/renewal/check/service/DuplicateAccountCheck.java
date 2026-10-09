package com.iortatechnxt.brokerverse.renewal.check.service;

import com.iortatechnxt.brokerverse.renewal.domain.CandidateSnapshot;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Potential duplicate of a generated renewal account (BDOI Renewal FRS FRRN.006.01): another
 * renewal account sharing the criteria of the line puts the renewal in Review with the reason
 * "Potential Duplicate Account - &lt;reference&gt;" (the reference opens the matching account); a
 * match with a cancelled account is reported and passes.
 */
@Component
public class DuplicateAccountCheck implements RenewalCheck {

  /** Check code. */
  public static final String CODE = "DUPLICATE_ACCOUNT";

  /** Message of a match with a cancelled account. */
  public static final String CANCELLED =
      "A matching account was found with a status of Cancelled. Account creation may proceed.";

  private final RenewalDuplicates duplicates;

  /**
   * Creates the check.
   *
   * @param duplicates duplicate checking
   */
  public DuplicateAccountCheck(RenewalDuplicates duplicates) {
    this.duplicates = duplicates;
  }

  @Override
  public String code() {
    return CODE;
  }

  @Override
  public Verdict evaluate(CheckContext context) {
    RenewalCandidate c = context.candidate();
    List<RenewalDuplicates.Match> matches = duplicates.of(c.getCompanyId(), subjectOf(c));
    Optional<RenewalDuplicates.Match> open =
        matches.stream().filter(m -> !m.cancelled()).findFirst();
    if (open.isPresent()) {
      return Verdict.fail("Potential Duplicate Account - " + open.get().ref(), open.get().ref());
    }
    return matches.isEmpty() ? Verdict.pass("No duplicate account") : Verdict.pass(CANCELLED);
  }

  /**
   * The duplicate criteria of a renewal.
   *
   * @param c renewal
   * @return subject
   */
  public static RenewalDuplicates.Subject subjectOf(RenewalCandidate c) {
    CandidateSnapshot s = c.getSnapshot();
    return new RenewalDuplicates.Subject(
        c.getRenewalRef(),
        s.product() == null ? null : s.product().lineCode(),
        s.client() == null ? null : s.client().clientCode(),
        s.product() == null ? null : s.product().productCode(),
        s.pnNos(),
        s.policyNo());
  }
}
