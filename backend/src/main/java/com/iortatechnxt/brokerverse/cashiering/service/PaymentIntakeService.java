package com.iortatechnxt.brokerverse.cashiering.service;

import com.iortatechnxt.brokerverse.cashiering.domain.Application;
import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.ApplicationSource;
import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.MatchCategory;
import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.ReceiptSource;
import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.UnappliedOrigin;
import com.iortatechnxt.brokerverse.cashiering.domain.Payment;
import com.iortatechnxt.brokerverse.cashiering.domain.PaymentIntake;
import com.iortatechnxt.brokerverse.cashiering.domain.PaymentRepository;
import com.iortatechnxt.brokerverse.cashiering.domain.Prebooked;
import com.iortatechnxt.brokerverse.cashiering.domain.PrebookedRepository;
import com.iortatechnxt.brokerverse.cashiering.domain.Receipt;
import com.iortatechnxt.brokerverse.cashiering.domain.Receipt.ReceiptTender;
import com.iortatechnxt.brokerverse.cashiering.domain.Unapplied;
import com.iortatechnxt.brokerverse.cashiering.domain.Unapplied.UnappliedSpec;
import com.iortatechnxt.brokerverse.cashiering.service.ApplicationService.ApplyOptions;
import com.iortatechnxt.brokerverse.cashiering.service.CashReceiptService.ArIssue;
import com.iortatechnxt.brokerverse.cashiering.service.PaymentMatcher.Match;
import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Payment acceptance (CSHID.008/020): every payment, whatever the channel, is recorded, gets its AR
 * and is matched at acceptance. Booked invoices with outstanding premium are applied (up to 100%,
 * 98% for 2% CWT accounts) oldest first; the rest is an excess in the unapplied workbench. A
 * payment for an account not booked yet waits in the pre-booked queue; a payment without a match,
 * or against a cancelled booking, becomes an unapplied item.
 */
@Service
@Transactional
public class PaymentIntakeService {

  private final PaymentRepository payments;
  private final PrebookedRepository prebooked;
  private final PaymentMatcher matcher;
  private final CashReceiptService receipts;
  private final ApplicationService applications;
  private final UnappliedService unappliedItems;
  private final DocumentNumberService numbers;

  /**
   * Creates the service.
   *
   * @param payments payments
   * @param prebooked pre-booked items
   * @param matcher matching
   * @param receipts receipts
   * @param applications applications
   * @param unappliedItems unapplied workbench
   * @param numbers document numbers
   */
  public PaymentIntakeService(
      PaymentRepository payments,
      PrebookedRepository prebooked,
      PaymentMatcher matcher,
      CashReceiptService receipts,
      ApplicationService applications,
      UnappliedService unappliedItems,
      DocumentNumberService numbers) {
    this.payments = payments;
    this.prebooked = prebooked;
    this.matcher = matcher;
    this.receipts = receipts;
    this.applications = applications;
    this.unappliedItems = unappliedItems;
    this.numbers = numbers;
  }

  /**
   * Receives a payment: records it, issues its AR and matches it. Idempotent on the channel's
   * source key.
   *
   * @param target company, branch, AR class and receipt source
   * @param intake payment
   * @return what happened
   */
  public IntakeResult receive(IntakeTarget target, PaymentIntake intake) {
    Optional<Payment> earlier =
        payments.findByCompanyIdAndChannelAndSourceKey(
            target.companyId(), intake.channel(), intake.sourceKey());
    if (earlier.isPresent()) {
      return new IntakeResult(earlier.get(), null, List.of(), null, null);
    }
    Payment payment =
        payments.save(
            new Payment(
                target.companyId(),
                target.branchId(),
                numbers.next("PAY-" + intake.valueDate().getYear()),
                intake));
    Match match = matcher.match(target.companyId(), intake.references());
    Receipt ar = receipts.issueAr(arOf(target, intake, payment, match));
    payment.receipted(ar.getId());
    return settle(payment, ar, match);
  }

  private IntakeResult settle(Payment payment, Receipt ar, Match match) {
    return switch (match.kind()) {
      case BOOKED -> applyBooked(payment, ar, match);
      case PREBOOKED -> {
        Prebooked item =
            prebooked.save(
                new Prebooked(payment, ar.getId(), match.reference(), match.account().getArn()));
        payment.matched(
            MatchCategory.PREBOOKED,
            match.account().getArn(),
            BigDecimal.ZERO,
            "Account " + match.account().getArn() + " not booked yet: waiting for the booking");
        yield new IntakeResult(payment, ar, List.of(), null, item);
      }
      case CANCELLED -> {
        Unapplied item = toUnapplied(payment, ar, UnappliedOrigin.CANCELLED_REFERENCE, match);
        payment.matched(
            MatchCategory.CANCELLED_REFERENCE,
            match.invoices().get(0).getInvoiceNo(),
            BigDecimal.ZERO,
            "Reference " + match.reference() + " is a cancelled booking");
        yield new IntakeResult(payment, ar, List.of(), item, null);
      }
      case NONE -> {
        Unapplied item = toUnapplied(payment, ar, UnappliedOrigin.NO_MATCH, match);
        payment.matched(
            MatchCategory.UNAPPLIED_NO_MATCH,
            null,
            BigDecimal.ZERO,
            "No booked or pre-booked account");
        yield new IntakeResult(payment, ar, List.of(), item, null);
      }
    };
  }

  private IntakeResult applyBooked(Payment payment, Receipt ar, Match match) {
    List<Application> made =
        applyOldestFirst(
            match.invoices(),
            payment.getAmount(),
            new Application.Origin(
                ar.getId(), null, ApplicationSource.PAYMENT, payment.getPaymentNo()),
            new ApplyOptions(payment.getValueDate(), false, ar.getReceiptNo()));
    BigDecimal applied =
        made.stream().map(Application::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal excess = payment.getAmount().subtract(applied);
    String invoiceNo = match.invoices().get(0).getInvoiceNo();
    Unapplied item =
        excess.signum() > 0 ? toUnapplied(payment, ar, UnappliedOrigin.EXCESS, match) : null;
    if (applied.signum() > 0 && excess.signum() == 0) {
      payment.matched(MatchCategory.APPLIED, invoiceNo, applied, "Applied to " + invoices(made));
    } else {
      payment.matched(
          MatchCategory.EXCESS,
          invoiceNo,
          applied,
          applied.signum() > 0
              ? "Applied to " + invoices(made) + "; excess " + excess + " unapplied"
              : "Nothing outstanding on " + match.reference() + ": payment unapplied");
    }
    return new IntakeResult(payment, ar, made, item, null);
  }

  /**
   * Applies money to invoices oldest first until it runs out.
   *
   * @param invoices invoices, oldest first
   * @param amount money
   * @param origin receipt or unapplied item and source
   * @param options value date and AR number
   * @return applications made
   */
  public List<Application> applyOldestFirst(
      List<OpsInvoice> invoices,
      BigDecimal amount,
      Application.Origin origin,
      ApplyOptions options) {
    List<Application> made = new ArrayList<>();
    BigDecimal left = amount;
    for (OpsInvoice invoice : invoices) {
      if (left.signum() <= 0) {
        break;
      }
      Optional<Application> app = applications.apply(invoice, left, origin, options);
      if (app.isPresent()) {
        made.add(app.get());
        left = left.subtract(app.get().getAmount());
      }
    }
    return made;
  }

  private Unapplied toUnapplied(Payment payment, Receipt ar, UnappliedOrigin origin, Match match) {
    BigDecimal balance = payment.getAmount().subtract(ar.getAppliedAmount());
    return unappliedItems.create(
        payment.getCompanyId(),
        payment.getBranchId(),
        new UnappliedSpec(
            origin,
            ar.getId(),
            payment.getId(),
            match.invoices().isEmpty() ? null : match.invoices().get(0).getInvoiceNo(),
            match.clientCode() != null ? match.clientCode() : ar.getPayorCode(),
            payment.getPayorName(),
            ar.getSalesUnit(),
            payment.getCurrency(),
            balance,
            null,
            CashieringSettings.MODULE,
            "PAY:" + payment.getPaymentNo(),
            payment.getReference()));
  }

  private static ArIssue arOf(IntakeTarget target, PaymentIntake intake, Payment payment, Match m) {
    String client = m.clientCode() != null ? m.clientCode() : intake.payor().code();
    String unit =
        m.invoices().isEmpty() ? null : m.invoices().get(0).getClassification().salesUnit();
    return new ArIssue(
        target.companyId(),
        target.branchId(),
        target.arClass(),
        intake.valueDate(),
        client,
        intake.payorName(),
        intake.assuredName() != null ? intake.assuredName() : assured(m),
        unit,
        intake.currency(),
        intake.amount(),
        new ReceiptTender(
            intake.mode(),
            intake.checkNo(),
            intake.checkBank(),
            null,
            null,
            target.source(),
            CashieringSettings.MODULE,
            "PAY:" + payment.getPaymentNo(),
            intake.reference()));
  }

  private static String assured(Match m) {
    if (m.account() != null) {
      return m.account().getClientName();
    }
    return m.invoices().isEmpty() ? null : m.invoices().get(0).getAssuredName();
  }

  private static String invoices(List<Application> made) {
    return String.join(", ", made.stream().map(Application::getInvoiceNo).toList());
  }

  /**
   * Where a payment is received.
   *
   * @param companyId company
   * @param branchId receiving branch
   * @param arClass AR class (LOV AR_CLASS)
   * @param source receipt source
   */
  public record IntakeTarget(Long companyId, Long branchId, String arClass, ReceiptSource source) {}

  /**
   * Outcome of an intake.
   *
   * @param payment payment with its category
   * @param receipt AR issued, null when the payment was already received
   * @param applications applications made
   * @param unapplied unapplied item created, may be null
   * @param prebooked pre-booked item created, may be null
   */
  public record IntakeResult(
      Payment payment,
      Receipt receipt,
      List<Application> applications,
      Unapplied unapplied,
      Prebooked prebooked) {

    /** Defensive copy. */
    public IntakeResult {
      applications = List.copyOf(applications);
    }
  }
}
