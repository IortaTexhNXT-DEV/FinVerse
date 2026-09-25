package com.iortatechnxt.brokerverse.commission.service;

import com.iortatechnxt.brokerverse.commission.domain.CommissionEnums.DpTag;
import com.iortatechnxt.brokerverse.commission.domain.CommissionEnums.Sanitation;
import com.iortatechnxt.brokerverse.commission.domain.DpItem;
import com.iortatechnxt.brokerverse.commission.domain.DpItemRepository;
import com.iortatechnxt.brokerverse.opsledger.domain.LedgerComponent;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerQueryService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Validation and sanitation of a direct payment account (CMRID.002/007/013, MKTID.012): the invoice
 * is booked, tagged direct payment by Marketing, not cancelled or written off, of the insurer and
 * premium submitted, without a pending negative adjustment, with its commission still receivable
 * and not billed twice; the commission receivable (commission, VAT, withholding tax, net) is taken
 * from the Operations ledger. Valid accounts wait for the reviewer's confirmation that they are
 * fully paid to the insurer (OQ38).
 */
@Component
public class DpValidator {

  /** Difference allowed between the submitted premium and the booked gross premium. */
  static final BigDecimal PREMIUM_TOLERANCE = new BigDecimal("1.00");

  private static final String PASS = "|PASS|";
  private static final String FAIL = "|FAIL|";

  private final InvoiceLedgerQueryService ledger;
  private final DpItemRepository items;

  /**
   * Creates the validator.
   *
   * @param ledger Operations ledger
   * @param items DP accounts (duplicates)
   */
  public DpValidator(InvoiceLedgerQueryService ledger, DpItemRepository items) {
    this.ledger = ledger;
    this.items = items;
  }

  /** The rules, in the order they are reported. */
  public enum Rule {
    /** The invoice is in the Operations ledger. */
    INVOICE_BOOKED,
    /** Marketing tagged the account direct payment (MKTID.011/012). */
    DIRECT_PAYMENT,
    /** The invoice is not cancelled nor written off (erroneous bookings). */
    NOT_CANCELLED,
    /** The insurer submitted is the invoice's. */
    INSURER,
    /** The premium submitted is the booked gross premium (within 1.00). */
    PREMIUM,
    /** No negative adjustment is pending on the invoice (CMRID.002). */
    NO_PENDING_ADJUSTMENT,
    /** The commission is still receivable (not paid twice). */
    COMMISSION_OPEN,
    /** The account carries a policy number (incomplete accounts are removed, CMRID.013). */
    COMPLETE,
    /** The account is not already listed (cross-branch duplicates, CMRID.001). */
    NOT_DUPLICATE
  }

  /**
   * Validates an account, computes its commission receivable and records the sanitation.
   *
   * @param item account (saved, so that it has an id)
   */
  public void validate(DpItem item) {
    List<String> results = new ArrayList<>();
    Optional<OpsInvoice> found = ledger.find(item.getInvoiceNo());
    if (found.isEmpty()) {
      results.add(Rule.INVOICE_BOOKED + FAIL + "Invoice " + item.getInvoiceNo() + " is not booked");
      item.sanitized(Sanitation.INVALID, String.join("\n", results), null);
      return;
    }
    OpsInvoice invoice = found.get();
    String submittedInsurer = item.getInsurerCode();
    item.computed(facts(invoice));
    results.add(Rule.INVOICE_BOOKED + PASS + invoice.getInvoiceNo());
    boolean valid = rules(item, invoice, submittedInsurer, results);
    boolean complete =
        check(results, Rule.COMPLETE, item.getPolicyNo() != null, "no policy number yet");
    Optional<DpItem> duplicate =
        items.findFirstByCompanyIdAndInvoiceNoAndTagInAndIdNotOrderByIdAsc(
            item.getCompanyId(), item.getInvoiceNo(), DpTag.ACTIVE, item.getId());
    check(
        results,
        Rule.NOT_DUPLICATE,
        duplicate.isEmpty(),
        duplicate.map(d -> "already listed by " + d.getBranchCode()).orElse(""));
    Sanitation result;
    if (duplicate.isPresent()) {
      result = Sanitation.DUPLICATE;
    } else if (!valid) {
      result = Sanitation.INVALID;
    } else {
      result = complete ? Sanitation.VALID : Sanitation.INCOMPLETE;
    }
    item.sanitized(result, String.join("\n", results), duplicate.map(DpItem::getId).orElse(null));
  }

  private static boolean rules(
      DpItem item, OpsInvoice invoice, String submittedInsurer, List<String> results) {
    boolean ok =
        check(results, Rule.DIRECT_PAYMENT, invoice.isDpFlag(), "not tagged direct payment");
    ok &=
        check(
            results,
            Rule.NOT_CANCELLED,
            !invoice.isCancelled() && !invoice.isWrittenOff(),
            "cancelled or written off");
    ok &=
        check(
            results,
            Rule.INSURER,
            submittedInsurer == null || submittedInsurer.equals(invoice.getInsurerCode()),
            "booked with " + invoice.getInsurerCode());
    ok &=
        check(
            results,
            Rule.PREMIUM,
            item.getSubmittedPremium() == null
                || item.getSubmittedPremium()
                        .subtract(invoice.getGrossPremium())
                        .abs()
                        .compareTo(PREMIUM_TOLERANCE)
                    <= 0,
            "booked gross premium " + invoice.getGrossPremium());
    ok &=
        check(
            results,
            Rule.NO_PENDING_ADJUSTMENT,
            !invoice.isPendingNegAdj(),
            "a negative adjustment is pending");
    ok &=
        check(
            results,
            Rule.COMMISSION_OPEN,
            invoice.component(LedgerComponent.COMMISSION).getBalance().signum() > 0,
            "no commission receivable left");
    return ok;
  }

  private static boolean check(List<String> results, Rule rule, boolean passed, String failure) {
    results.add(rule + (passed ? PASS : FAIL + failure));
    return passed;
  }

  private static DpItem.LedgerFacts facts(OpsInvoice invoice) {
    return new DpItem.LedgerFacts(
        invoice.getPolicyNo(),
        invoice.getInsurerCode(),
        invoice.getClientCode(),
        invoice.getAssuredName(),
        invoice.getGrossPremium(),
        invoice.getCommission(),
        invoice.getVatOnCommission(),
        invoice.component(LedgerComponent.WTAX).getBooked());
  }
}
