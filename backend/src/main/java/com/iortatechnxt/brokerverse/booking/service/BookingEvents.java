package com.iortatechnxt.brokerverse.booking.service;

import com.iortatechnxt.brokerverse.accounting.service.BusinessEvent;
import com.iortatechnxt.brokerverse.booking.domain.CommissionTerms;
import com.iortatechnxt.brokerverse.booking.domain.InsurerShare;
import com.iortatechnxt.brokerverse.booking.domain.InvoiceDraft;
import com.iortatechnxt.brokerverse.booking.domain.PremiumComponents;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Builds the {@code BROKER_BOOKING} accounting events of a booked invoice (BRNB.027,
 * OPERATIONS_DESIGN section 5 rows 0, 16 and 19): one event per insurer share, whose party is the
 * insurer while the premium receivable components carry the client as their party.
 *
 * <ul>
 *   <li>Premium: Dr PR by component (client) / Cr DTIP (insurer). Not for direct payment
 *       (BRNB.114), where the client pays the insurer.
 *   <li>Commission: Dr commission receivable (insurer) / Cr unrealized commission and deferred
 *       output VAT; with {@code OPS_COMMISSION_REALIZATION = ON_BOOKING}, Cr commission income and
 *       output VAT instead.
 * </ul>
 *
 * Negative amounts (return premium, cancellation) post to the opposite sides and so reverse event
 * 0. The share of the last insurer takes the rounding difference.
 */
@Component
public class BookingEvents {

  /** Accounting event type (V870). */
  public static final String EVENT_TYPE = "BROKER_BOOKING";

  /** Source module on events, journals and open items. */
  public static final String MODULE = "BOOKING";

  static final String PR_BASIC = "PR_BASIC";
  static final String PR_DST = "PR_DST";
  static final String PR_PTX_VAT = "PR_PTX_VAT";
  static final String PR_LGT = "PR_LGT";
  static final String PR_FST = "PR_FST";
  static final String PR_OTHER = "PR_OTHER";
  static final String DTIP = "DTIP";
  static final String COMMISSION_RECEIVABLE = "COMMISSION_RECEIVABLE";
  static final String UNREALIZED_COMMISSION = "UNREALIZED_COMMISSION";
  static final String DEFERRED_OUTPUT_VAT = "DEFERRED_OUTPUT_VAT";
  static final String COMMISSION_INCOME = "COMMISSION_INCOME";
  static final String OUTPUT_VAT = "OUTPUT_VAT";

  private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
  private static final List<String> PR_COMPONENTS =
      List.of(PR_BASIC, PR_DST, PR_PTX_VAT, PR_LGT, PR_FST, PR_OTHER);

  private final BookingSettings settings;

  /**
   * Creates the builder.
   *
   * @param settings commission realization parameter
   */
  public BookingEvents(BookingSettings settings) {
    this.settings = settings;
  }

  /**
   * The events of an invoice, one per insurer share.
   *
   * @param posting invoice facts and amounts
   * @return events with the insurer share they account for
   */
  public List<ShareEvent> events(PostingFacts posting) {
    boolean realize = settings.realizeCommissionOnBooking();
    List<ShareEvent> events = new ArrayList<>();
    List<ShareAmounts> split = split(posting.premium(), posting.commission(), posting.shares());
    for (ShareAmounts share : split) {
      Map<String, BigDecimal> amounts = new LinkedHashMap<>();
      Map<String, String> parties = new HashMap<>();
      if (!posting.directPayment()) {
        premiumAmounts(share.premium(), amounts);
        PR_COMPONENTS.forEach(c -> parties.put(c, posting.clientCode()));
      }
      commissionAmounts(share.commission(), realize, amounts);
      events.add(
          new ShareEvent(
              share.insurerCode(),
              share.premium(),
              share.commission(),
              new BusinessEvent(
                  EVENT_TYPE,
                  posting.companyId(),
                  posting.branchId(),
                  posting.valueDate(),
                  posting.currency(),
                  MODULE,
                  "BKG:" + posting.reference() + ":" + share.insurerCode(),
                  posting.reference(),
                  share.insurerCode(),
                  posting.lineCode(),
                  posting.costCenter(),
                  posting.narration(),
                  amounts,
                  Map.of(),
                  parties)));
    }
    return events;
  }

  /**
   * Posting facts of an invoice not yet booked (pre-booking confirmation, BRNB.036).
   *
   * @param draft invoice draft
   * @param reference reference shown in place of the invoice number
   * @param date booking date
   * @return facts
   */
  public static PostingFacts factsOf(InvoiceDraft draft, String reference, LocalDate date) {
    return new PostingFacts(
        draft.companyId(),
        draft.branchId(),
        reference,
        date,
        draft.currency(),
        draft.facts().clientCode(),
        draft.facts().lineCode(),
        draft.facts().costCenter(),
        draft.kind() + " " + draft.arn(),
        draft.flags().directPayment(),
        draft.premium(),
        draft.commission(),
        draft.shares());
  }

  private static void premiumAmounts(PremiumComponents p, Map<String, BigDecimal> amounts) {
    amounts.put(PR_BASIC, p.basic());
    amounts.put(PR_DST, p.dst());
    amounts.put(PR_PTX_VAT, p.premiumTaxVat());
    amounts.put(PR_LGT, p.lgt());
    amounts.put(PR_FST, p.fst());
    amounts.put(PR_OTHER, p.other());
    amounts.put(DTIP, p.total());
  }

  private static void commissionAmounts(
      CommissionTerms c, boolean realize, Map<String, BigDecimal> amounts) {
    amounts.put(COMMISSION_RECEIVABLE, c.receivable());
    amounts.put(realize ? COMMISSION_INCOME : UNREALIZED_COMMISSION, c.commission());
    amounts.put(realize ? OUTPUT_VAT : DEFERRED_OUTPUT_VAT, c.vatOnCommission());
  }

  /**
   * Splits the premium and commission by insurer share; the last share takes the remainder so the
   * shares add up exactly.
   *
   * @param premium invoice premium
   * @param commission invoice commission
   * @param shares insurer shares
   * @return amounts per insurer
   */
  public static List<ShareAmounts> split(
      PremiumComponents premium, CommissionTerms commission, List<InsurerShare> shares) {
    List<ShareAmounts> result = new ArrayList<>();
    PremiumComponents premiumLeft = premium;
    CommissionTerms commissionLeft = commission;
    for (int i = 0; i < shares.size(); i++) {
      InsurerShare share = shares.get(i);
      boolean last = i == shares.size() - 1;
      BigDecimal factor = share.sharePct().divide(HUNDRED);
      PremiumComponents p = last ? premiumLeft : premium.times(factor);
      CommissionTerms c = last ? commissionLeft : commission.times(factor);
      result.add(new ShareAmounts(share.insurerCode(), p, c));
      premiumLeft = premiumLeft.minus(p);
      commissionLeft =
          CommissionTerms.of(
              commission.rate(),
              commissionLeft.commission().subtract(c.commission()),
              commissionLeft.vatOnCommission().subtract(c.vatOnCommission()),
              commission.wtaxRate());
    }
    return result;
  }

  /**
   * What an invoice posting needs.
   *
   * @param companyId company
   * @param branchId branch
   * @param reference invoice number (source reference and ledger reference)
   * @param valueDate booking date
   * @param currency currency
   * @param clientCode client party (premium receivable)
   * @param lineCode line of business
   * @param costCenter cost center
   * @param narration narration
   * @param directPayment direct payment (no premium legs)
   * @param premium premium by component
   * @param commission commission terms
   * @param shares insurer shares
   */
  public record PostingFacts(
      Long companyId,
      Long branchId,
      String reference,
      LocalDate valueDate,
      String currency,
      String clientCode,
      String lineCode,
      String costCenter,
      String narration,
      boolean directPayment,
      PremiumComponents premium,
      CommissionTerms commission,
      List<InsurerShare> shares) {}

  /**
   * One insurer's part of an invoice.
   *
   * @param insurerCode insurer
   * @param premium premium share
   * @param commission commission share
   */
  public record ShareAmounts(
      String insurerCode, PremiumComponents premium, CommissionTerms commission) {}

  /**
   * The event of one insurer share.
   *
   * @param insurerCode insurer
   * @param premium premium share
   * @param commission commission share
   * @param event accounting event
   */
  public record ShareEvent(
      String insurerCode,
      PremiumComponents premium,
      CommissionTerms commission,
      BusinessEvent event) {

    /**
     * Whether the event has anything to post.
     *
     * @return true when some amount is not zero
     */
    public boolean hasAmounts() {
      return event.amounts().values().stream().anyMatch(a -> a.signum() != 0);
    }
  }
}
