package com.iortatechnxt.brokerverse.renewal.check.service;

import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import java.util.Objects;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * {@code REFERENCE_MATCH} (BRRN.022; FR-RN-021): the renewal reference is well formed and the
 * renewal matches its expiring policy - for a booked policy the root invoice is in the ledger with
 * the same ARN; for a migrated or submitted policy the source reference is present. A mismatch
 * sends the renewal to the Exception bucket and never drops it.
 */
@Component
public class ReferenceMatchCheck implements RenewalCheck {

  /** Check code. */
  public static final String CODE = "REFERENCE_MATCH";

  private static final Pattern REFERENCE = Pattern.compile("[A-Z0-9]{2,10}-\\d{4}-\\d{6}");

  @Override
  public String code() {
    return CODE;
  }

  @Override
  public Verdict evaluate(CheckContext context) {
    RenewalCandidate c = context.candidate();
    if (!REFERENCE.matcher(c.getRenewalRef()).matches()) {
      return Verdict.fail("Renewal reference " + c.getRenewalRef() + " is not valid", "INVALID");
    }
    if (!context.bibs()) {
      return c.getSourceRef() == null
          ? Verdict.fail("The renewal has no reference of its policy", "MISSING")
          : Verdict.pass("Matched to policy " + c.getSourceRef());
    }
    return ledgerMatch(context, c);
  }

  private static Verdict ledgerMatch(CheckContext context, RenewalCandidate c) {
    OpsInvoice root =
        context.family().stream()
            .filter(i -> i.getInvoiceNo().equals(c.getExpiringInvoiceNo()))
            .findFirst()
            .orElse(null);
    if (root == null) {
      return Verdict.fail(
          "Expiring invoice " + c.getExpiringInvoiceNo() + " is not in the ledger", "MISSING");
    }
    if (!Objects.equals(root.getArn(), c.getExpiringArn())) {
      return Verdict.fail(
          "Expiring invoice " + root.getInvoiceNo() + " belongs to account " + root.getArn(),
          "AMBIGUOUS");
    }
    return Verdict.pass("Matched to invoice " + root.getInvoiceNo() + " of " + root.getArn());
  }
}
