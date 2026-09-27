package com.iortatechnxt.brokerverse.adjustment.service;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.account.service.AccountQueryService;
import com.iortatechnxt.brokerverse.adjustment.domain.AmountInput;
import com.iortatechnxt.brokerverse.adjustment.domain.Computation;
import com.iortatechnxt.brokerverse.adjustment.domain.RefundBasis;
import com.iortatechnxt.brokerverse.adjustment.domain.RequestTerms;
import com.iortatechnxt.brokerverse.adjustment.domain.ShareChange;
import com.iortatechnxt.brokerverse.booking.domain.CancellationKind;
import com.iortatechnxt.brokerverse.booking.domain.CommissionTerms;
import com.iortatechnxt.brokerverse.booking.domain.EndorsementType;
import com.iortatechnxt.brokerverse.booking.domain.InvoiceDraft;
import com.iortatechnxt.brokerverse.booking.domain.PremiumComponents;
import com.iortatechnxt.brokerverse.booking.service.EndorsementPosting;
import com.iortatechnxt.brokerverse.booking.service.EndorsementPostingService;
import com.iortatechnxt.brokerverse.catalog.service.PeriodBasis;
import com.iortatechnxt.brokerverse.catalog.service.PremiumBreakdown;
import com.iortatechnxt.brokerverse.catalog.service.RatingQuery;
import com.iortatechnxt.brokerverse.catalog.service.RatingService;
import com.iortatechnxt.brokerverse.catalog.service.RatingService.Rating;
import com.iortatechnxt.brokerverse.opsledger.domain.LedgerComponent;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoiceShare;
import com.iortatechnxt.brokerverse.opsledger.service.InsurerShareAllocator;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Premium and commission changes of a request by computation (ADJID.008/009/014/027):
 *
 * <ul>
 *   <li>cancellations: booking's own preview ({@code EndorsementPostingService.preview}) of the
 *       flat, flat retain-DST or partial return, so the preview equals the posting;
 *   <li>TSI change: the catalog calculator in endorsement mode ({@code endorsement = true},
 *       remaining term pro-rata or short-period) once per insurer share, each insurer at its own
 *       commission rate, the lead insurer with its branch LGT;
 *   <li>amount change: the amounts entered, the commission derived from the invoice's rate when not
 *       entered, split by the insurer shares;
 *   <li>write-off: the outstanding premium receivable.
 * </ul>
 */
@Component
public class PremiumDeltas {

  private static final int MONEY = 2;
  private static final int RATE = 8;
  private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
  private static final String ENDORSEMENT_ITEM = "Endorsement";

  private final EndorsementPostingService endorsements;
  private final RatingService rating;
  private final AccountQueryService accounts;

  /**
   * Creates the calculator.
   *
   * @param endorsements booking endorsement preview (cancellations)
   * @param rating catalog rating (TSI change)
   * @param accounts accounts (product, insurer branch)
   */
  public PremiumDeltas(
      EndorsementPostingService endorsements, RatingService rating, AccountQueryService accounts) {
    this.endorsements = endorsements;
    this.rating = rating;
    this.accounts = accounts;
  }

  /**
   * The changes of a request.
   *
   * @param invoice invoice of the request
   * @param computation computation
   * @param terms request terms
   * @param amounts amounts entered
   * @return premium, commission and insurer changes
   */
  public Delta compute(
      OpsInvoice invoice, Computation computation, RequestTerms terms, AmountInput amounts) {
    return switch (computation) {
      case NONE -> Delta.NONE;
      case WRITE_OFF -> writeOff(invoice);
      case SUM_INSURED -> sumInsured(invoice, terms);
      case AMOUNTS -> entered(invoice, amounts);
      default -> cancellation(invoice, computation, terms);
    };
  }

  /**
   * The booking posting of a cancellation (preview and post use the same).
   *
   * @param invoice invoice
   * @param computation cancellation kind
   * @param terms terms
   * @param sourceRef idempotency key, null for a preview
   * @return posting
   */
  public EndorsementPosting cancellationPosting(
      OpsInvoice invoice, Computation computation, RequestTerms terms, String sourceRef) {
    return new EndorsementPosting(
        invoice.getArn(),
        EndorsementType.CANCELLATION,
        kindOf(computation),
        terms.effectiveDate(),
        basisOf(terms.refundBasis()),
        null,
        null,
        null,
        null,
        terms.description(),
        terms.reasonCode(),
        null,
        sourceRef);
  }

  /**
   * The catalog period basis of a refund basis.
   *
   * @param basis refund basis
   * @return period basis
   */
  public static PeriodBasis basisOf(RefundBasis basis) {
    return basis == RefundBasis.SHORT_PERIOD ? PeriodBasis.SHORT_PERIOD : PeriodBasis.PRO_RATA;
  }

  private static CancellationKind kindOf(Computation computation) {
    return switch (computation) {
      case CANCELLATION_FLAT -> CancellationKind.FLAT;
      case CANCELLATION_FLAT_RETAIN_DST -> CancellationKind.FLAT_RETAIN_DST;
      default -> CancellationKind.PARTIAL;
    };
  }

  private Delta cancellation(OpsInvoice invoice, Computation computation, RequestTerms terms) {
    InvoiceDraft draft =
        endorsements.preview(cancellationPosting(invoice, computation, terms, null)).invoice();
    return allocated(invoice, draft.premium(), draft.commission());
  }

  private Delta writeOff(OpsInvoice invoice) {
    Map<LedgerComponent, BigDecimal> balances = invoice.balances();
    PremiumComponents outstanding =
        new PremiumComponents(
            balances.get(LedgerComponent.BASIC),
            balances.get(LedgerComponent.DST),
            balances.get(LedgerComponent.PREMIUM_TAX_VAT),
            balances.get(LedgerComponent.LGT),
            balances.get(LedgerComponent.FST),
            balances.get(LedgerComponent.OTHER));
    return new Delta(outstanding.negate(), zeroCommission(invoice), List.of());
  }

  private Delta entered(OpsInvoice invoice, AmountInput a) {
    PremiumComponents premium =
        new PremiumComponents(a.basic(), a.dst(), a.premiumTaxVat(), a.lgt(), a.fst(), a.other());
    BigDecimal rate = commissionRate(invoice);
    BigDecimal commission =
        a.commission() != null
            ? a.commission()
            : premium.basic().multiply(rate).divide(HUNDRED, MONEY, RoundingMode.HALF_UP);
    BigDecimal vat = a.vatOnCommission() != null ? a.vatOnCommission() : vatOn(invoice, commission);
    return allocated(
        invoice, premium, CommissionTerms.of(rate, commission, vat, invoice.getWtaxRate()));
  }

  private Delta sumInsured(OpsInvoice invoice, RequestTerms terms) {
    Account account = accounts.requireByArn(invoice.getArn());
    List<ShareChange> changes = new ArrayList<>();
    PremiumComponents premium = PremiumComponents.ZERO;
    BigDecimal commission = BigDecimal.ZERO;
    BigDecimal vat = BigDecimal.ZERO;
    BigDecimal rate = BigDecimal.ZERO;
    for (OpsInvoiceShare share : invoice.getShares()) {
      BigDecimal part =
          terms
              .sumInsuredChange()
              .multiply(share.sharePct())
              .divide(HUNDRED, MONEY, RoundingMode.HALF_UP);
      if (part.signum() == 0) {
        continue;
      }
      Rating r = rating.rate(query(account, invoice, share, part, terms));
      PremiumBreakdown b = r.breakdown();
      PremiumComponents p =
          new PremiumComponents(
              b.netPremium(),
              b.dst(),
              b.premiumTax().add(b.vat()),
              b.lgt(),
              b.fst(),
              BigDecimal.ZERO);
      premium = premium.plus(p);
      commission = commission.add(b.commission());
      vat = vat.add(b.vatOnCommission());
      rate = share.lead() ? r.rates().commission() : rate;
      changes.add(
          new ShareChange(
              share.insurerCode(),
              share.sharePct(),
              share.lead(),
              p.total(),
              money(b.commission()),
              money(b.vatOnCommission())));
    }
    return new Delta(
        premium, CommissionTerms.of(rate, commission, vat, invoice.getWtaxRate()), changes);
  }

  private static RatingQuery query(
      Account account, OpsInvoice invoice, OpsInvoiceShare share, BigDecimal part, RequestTerms t) {
    LocalDate expiry = invoice.getClassification().expiryDate();
    return new RatingQuery(
        invoice.getCompanyId(),
        account.getProductCode(),
        share.insurerCode(),
        share.lead() ? account.getInsurerBranch() : null,
        List.of(new RatingQuery.Item(ENDORSEMENT_ITEM, part, t.ratePercent(), null, null)),
        account.isMultiYear(),
        basisOf(t.refundBasis()),
        t.effectiveDate(),
        expiry,
        null,
        true,
        t.effectiveDate());
  }

  private static Delta allocated(
      OpsInvoice invoice, PremiumComponents premium, CommissionTerms commission) {
    List<OpsInvoiceShare> shares = invoice.getShares();
    Map<String, BigDecimal> premiums = InsurerShareAllocator.allocate(premium.total(), shares);
    Map<String, BigDecimal> commissions =
        InsurerShareAllocator.allocate(commission.commission(), shares);
    Map<String, BigDecimal> vats =
        InsurerShareAllocator.allocate(commission.vatOnCommission(), shares);
    List<ShareChange> changes =
        shares.stream()
            .map(
                s ->
                    new ShareChange(
                        s.insurerCode(),
                        s.sharePct(),
                        s.lead(),
                        premiums.get(s.insurerCode()),
                        commissions.get(s.insurerCode()),
                        vats.get(s.insurerCode())))
            .toList();
    return new Delta(premium, commission, changes);
  }

  /**
   * The commission rate of an invoice, from its commission and basic premium.
   *
   * @param invoice invoice
   * @return rate in percent
   */
  static BigDecimal commissionRate(OpsInvoice invoice) {
    BigDecimal basic = invoice.component(LedgerComponent.BASIC).getBooked();
    if (basic.signum() == 0) {
      return BigDecimal.ZERO;
    }
    return invoice.getCommission().multiply(HUNDRED).divide(basic, RATE, RoundingMode.HALF_UP);
  }

  private static BigDecimal vatOn(OpsInvoice invoice, BigDecimal commission) {
    if (invoice.getCommission().signum() == 0) {
      return BigDecimal.ZERO.setScale(MONEY);
    }
    return commission
        .multiply(invoice.getVatOnCommission())
        .divide(invoice.getCommission(), MONEY, RoundingMode.HALF_UP);
  }

  private static CommissionTerms zeroCommission(OpsInvoice invoice) {
    return CommissionTerms.of(
        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, invoice.getWtaxRate());
  }

  private static BigDecimal money(BigDecimal amount) {
    return amount.setScale(MONEY, RoundingMode.HALF_UP);
  }

  /**
   * Changes of a request.
   *
   * @param premium premium change by component
   * @param commission commission change
   * @param shares change per insurer
   */
  public record Delta(
      PremiumComponents premium, CommissionTerms commission, List<ShareChange> shares) {

    /** No change. */
    public static final Delta NONE =
        new Delta(
            PremiumComponents.ZERO,
            CommissionTerms.of(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO),
            List.of());

    /** Defensive copy. */
    public Delta {
      shares = List.copyOf(shares);
    }
  }
}
