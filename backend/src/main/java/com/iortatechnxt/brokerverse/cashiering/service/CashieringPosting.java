package com.iortatechnxt.brokerverse.cashiering.service;

import com.iortatechnxt.brokerverse.accounting.service.AccountingEventPublisher;
import com.iortatechnxt.brokerverse.accounting.service.BusinessEvent;
import com.iortatechnxt.brokerverse.opsledger.domain.LedgerComponent;
import com.iortatechnxt.brokerverse.opsledger.service.BookRates;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Publishes the cashiering accounting events (OPERATIONS_DESIGN section 5) to the accounting engine
 * inside the caller's transaction, at the Comptrollership BOOK rate with 2 decimals
 * (CSHID.012-014). GL accounts are never chosen here: the rules of each event are configured by
 * Comptrollership (OQ07); only the {@code @BANK} role is supplied from the collection account
 * parameters.
 */
@Component
@Transactional(propagation = Propagation.MANDATORY)
public class CashieringPosting {

  /** AR issued (row 1). */
  public static final String AR_RECEIPT = "OPS_AR_RECEIPT";

  /** Payment applied (row 2). */
  public static final String PAYMENT_APPLY = "OPS_PAYMENT_APPLY";

  /** PR to PR2307 (row 3). */
  public static final String CWT_RECLASS = "OPS_CWT_RECLASS";

  /** PR2307 against DTIP (row 4). */
  public static final String CWT_DTIP_OFFSET = "OPS_CWT_DTIP_OFFSET";

  /** Minimal excess to overages (row 6). */
  public static final String EXCESS_TO_OVERAGES = "OPS_EXCESS_TO_OVERAGES";

  /** PR minimal balance (row 7). */
  public static final String MINIMAL_BALANCE = "OPS_MINIMAL_BALANCE_REVERSAL";

  /** Unapplied refund (row 8). */
  public static final String UNAPPLIED_REFUND = "OPS_UNAPPLIED_REFUND";

  /** Unapplied reclass or transfer (row 8). */
  public static final String UNAPPLIED_RECLASS = "OPS_UNAPPLIED_RECLASS";

  /** Reinstatement (row 10). */
  public static final String RECEIPT_REINSTATE = "OPS_RECEIPT_REINSTATE";

  /** Insurer non-premium receipt (row 11). */
  public static final String AR_INSURANCE_RECEIPT = "OPS_AR_INSURANCE_RECEIPT";

  /** Official receipt (rows 14-15). */
  public static final String OR_ISSUE = "OPS_OR_ISSUE";

  /** Single amount component. */
  public static final String AMOUNT = "AMOUNT";

  private final AccountingEventPublisher publisher;
  private final BookRates bookRates;

  /**
   * Creates the posting.
   *
   * @param publisher accounting engine
   * @param bookRates BOOK rates
   */
  public CashieringPosting(AccountingEventPublisher publisher, BookRates bookRates) {
    this.publisher = publisher;
    this.bookRates = bookRates;
  }

  /**
   * Publishes an event; zero amounts are dropped and an event without amounts is not published.
   *
   * @param context company, branch, date, currency, party and dimensions
   * @param eventType event type
   * @param sourceRef idempotency key
   * @param amounts amount components (signed; negative reverses)
   * @return journal batch number, null when nothing was posted
   */
  public String publish(
      PostingContext context, String eventType, String sourceRef, Map<String, BigDecimal> amounts) {
    return publish(context, eventType, sourceRef, amounts, Map.of());
  }

  /**
   * Publishes an event whose party lines concern several parties.
   *
   * @param context company, branch, date, currency, party and dimensions
   * @param eventType event type
   * @param sourceRef idempotency key
   * @param amounts amount components (signed; negative reverses)
   * @param parties party per component, others use the context party
   * @return journal batch number, null when nothing was posted
   */
  public String publish(
      PostingContext context,
      String eventType,
      String sourceRef,
      Map<String, BigDecimal> amounts,
      Map<String, String> parties) {
    Map<String, BigDecimal> nonZero = new LinkedHashMap<>();
    amounts.forEach(
        (k, v) -> {
          if (v != null && v.signum() != 0) {
            nonZero.put(k, v);
          }
        });
    if (nonZero.isEmpty()) {
      return null;
    }
    BusinessEvent event =
        new BusinessEvent(
            eventType,
            context.companyId(),
            context.branchId(),
            context.valueDate(),
            context.currency(),
            CashieringSettings.MODULE,
            sourceRef,
            context.reference(),
            context.partyCode(),
            context.businessLine(),
            context.costCenter(),
            context.narration(),
            nonZero,
            context.bankAccount() == null ? Map.of() : Map.of("BANK", context.bankAccount()),
            parties);
    return publisher.publish(bookRates.price(event)).getBatchNo();
  }

  /**
   * The event component of a premium receivable component.
   *
   * @param component ledger component
   * @return PR_BASIC, PR_DST, PR_PTX_VAT, PR_LGT, PR_FST or PR_OTHER
   */
  public static String prComponent(LedgerComponent component) {
    return switch (component) {
      case DST -> "PR_DST";
      case PREMIUM_TAX_VAT -> "PR_PTX_VAT";
      case LGT -> "PR_LGT";
      case FST -> "PR_FST";
      case OTHER -> "PR_OTHER";
      default -> "PR_BASIC";
    };
  }

  /**
   * Premium components as event amounts, optionally negated.
   *
   * @param allocation amount per component
   * @param negate whether to reverse
   * @return event amounts
   */
  public static Map<String, BigDecimal> prAmounts(
      Map<LedgerComponent, BigDecimal> allocation, boolean negate) {
    Map<String, BigDecimal> map = new LinkedHashMap<>();
    allocation.forEach(
        (c, v) -> map.merge(prComponent(c), negate ? v.negate() : v, BigDecimal::add));
    return map;
  }

  /**
   * Facts shared by the events of one transaction.
   *
   * @param companyId company
   * @param branchId branch
   * @param valueDate accounting date
   * @param currency currency
   * @param reference business reference on the ledger (AR, OR, invoice)
   * @param partyCode sub-ledger party
   * @param businessLine line of business, may be null
   * @param costCenter cost centre, may be null
   * @param narration narration
   * @param bankAccount account of the {@code @BANK} role, null when the event has none
   */
  public record PostingContext(
      Long companyId,
      Long branchId,
      LocalDate valueDate,
      String currency,
      String reference,
      String partyCode,
      String businessLine,
      String costCenter,
      String narration,
      String bankAccount) {}
}
