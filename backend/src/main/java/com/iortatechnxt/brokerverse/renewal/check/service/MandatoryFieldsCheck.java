package com.iortatechnxt.brokerverse.renewal.check.service;

import com.iortatechnxt.brokerverse.account.service.AccountCheck;
import com.iortatechnxt.brokerverse.account.service.AccountQueryService;
import com.iortatechnxt.brokerverse.renewal.domain.CurrentDisposition;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalDisposition;
import com.iortatechnxt.brokerverse.renewal.service.RenewalParameters;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * {@code MANDATORY_FIELDS} (BRD 2.004.5, 3.007.3): the requirements of the disposition (the reason
 * of a Not for Renewal, the new invoice number of Booked to New Invoice) and, once the renewal
 * account exists, the mandatory fields, documents and premium of its product.
 */
@Component
public class MandatoryFieldsCheck implements RenewalCheck {

  /** Check code. */
  public static final String CODE = "MANDATORY_FIELDS";

  private final AccountQueryService accounts;
  private final RenewalParameters parameters;

  /**
   * Creates the check.
   *
   * @param accounts account completeness
   * @param parameters renewal parameters
   */
  public MandatoryFieldsCheck(AccountQueryService accounts, RenewalParameters parameters) {
    this.accounts = accounts;
    this.parameters = parameters;
  }

  @Override
  public String code() {
    return CODE;
  }

  @Override
  public Verdict evaluate(CheckContext context) {
    List<String> missing = new ArrayList<>();
    CurrentDisposition d = context.candidate().getDisposition();
    if (d.code() == RenewalDisposition.NOT_FOR_RENEWAL) {
      if (d.reasonCode() == null) {
        missing.add("reason for Not for Renewal");
      } else if (parameters.invoiceNoReason(d.reasonCode()) && d.newInvoiceNo() == null) {
        missing.add("new invoice number");
      }
    }
    context
        .renewalAccount()
        .ifPresent(
            a -> {
              AccountCheck check = accounts.check(a.getId());
              missing.addAll(check.fieldErrors().keySet());
              missing.addAll(check.missingDocuments());
              if (!check.premiumRated()) {
                missing.add("rated premium");
              }
            });
    return missing.isEmpty()
        ? Verdict.pass("Mandatory fields complete")
        : Verdict.fail("Complete the mandatory fields: " + String.join(", ", missing), null);
  }
}
