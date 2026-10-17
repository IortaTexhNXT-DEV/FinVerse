package com.iortatechnxt.brokerverse.renewal.extraction.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.renewal.check.service.DuplicateAccountCheck;
import com.iortatechnxt.brokerverse.renewal.check.service.RenewalDuplicates;
import com.iortatechnxt.brokerverse.renewal.check.service.RenewalDuplicates.Match;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidateRepository;
import com.iortatechnxt.brokerverse.renewal.service.RenewalCodes;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Manual creation of a renewal account (BDOI Renewal FRS FRRN.005.01) with the duplicate checking
 * of FRRN.006.01: the user chooses an expiring account; an exact duplicate (the account already has
 * its renewal account) is refused with the link to it; a potential duplicate is shown with its link
 * and the account is created only once the user confirms; a match with a cancelled account is shown
 * and does not block. The renewal account then goes through the checks, the classification and the
 * routing like a generated one; its renewal account starts one day after the expiry and runs one
 * year.
 */
@Service
public class ManualCreationService {

  /** Warning of a potential duplicate. */
  public static final String POTENTIAL =
      "Potential Duplicate Account Detected. A similar account was found based on the configured"
          + " duplicate validation criteria. Please review before proceeding.";

  private final ExpiringPolicies policies;
  private final ExtractionService extraction;
  private final RenewalDuplicates duplicates;
  private final RenewalCandidateRepository candidates;
  private final AuditTrailService audit;

  /**
   * Creates the service.
   *
   * @param policies expiring accounts
   * @param extraction creation of renewal accounts
   * @param duplicates duplicate checking
   * @param candidates renewals
   * @param audit audit trail
   */
  public ManualCreationService(
      ExpiringPolicies policies,
      ExtractionService extraction,
      RenewalDuplicates duplicates,
      RenewalCandidateRepository candidates,
      AuditTrailService audit) {
    this.policies = policies;
    this.extraction = extraction;
    this.duplicates = duplicates;
    this.candidates = candidates;
    this.audit = audit;
  }

  /**
   * Creates the renewal account of an expiring account, or tells the duplicates.
   *
   * @param companyId company
   * @param invoiceNo invoice of the expiring account
   * @param confirmed whether the user confirmed after a potential duplicate warning
   * @return the renewal created, or the duplicates found
   */
  public Result create(Long companyId, String invoiceNo, boolean confirmed) {
    ExpiringInvoice invoice =
        policies
            .invoice(companyId, invoiceNo)
            .orElseThrow(
                () ->
                    new BusinessRuleException(
                        "RNW_MANUAL_NOT_FOUND", "Expiring account " + invoiceNo + " not found"));
    if (invoice.extracted()) {
      String ref =
          candidates
              .findByCompanyIdAndExpiringInvoiceNo(companyId, invoiceNo)
              .map(RenewalCandidate::getRenewalRef)
              .orElse(null);
      return new Result(
          null,
          "The expiring account already has its renewal account " + ref,
          List.of(new Match(ref, true, false, List.of("EXPIRING_INVOICE"))));
    }
    List<Match> matches = duplicates.of(companyId, subject(invoice));
    boolean potential = matches.stream().anyMatch(m -> !m.cancelled());
    if (potential && !confirmed) {
      return new Result(null, POTENTIAL, matches);
    }
    RenewalCandidate c = extraction.createManual(companyId, invoice);
    audit.record(
        RenewalCodes.ENTITY,
        c.getRenewalRef(),
        AuditAction.CREATE,
        "Renewal account created by hand from " + invoiceNo);
    String message =
        matches.stream().anyMatch(Match::cancelled) ? DuplicateAccountCheck.CANCELLED : null;
    return new Result(c.getRenewalRef(), message, matches);
  }

  private static RenewalDuplicates.Subject subject(ExpiringInvoice i) {
    return new RenewalDuplicates.Subject(
        null,
        i.product().line(),
        i.client().code(),
        i.product().code(),
        i.facts().pnNos(),
        i.facts().policyNo());
  }

  /**
   * The outcome of a manual creation.
   *
   * @param renewalRef the renewal account created, or null
   * @param message warning shown to the user, or null
   * @param duplicates the matching renewal accounts (links)
   */
  public record Result(String renewalRef, String message, List<Match> duplicates) {

    /** Defensive copy. */
    public Result {
      duplicates = List.copyOf(duplicates);
    }
  }
}
