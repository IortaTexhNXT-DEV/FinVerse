package com.iortatechnxt.brokerverse.cashiering.service;

import com.iortatechnxt.brokerverse.cashiering.domain.Application;
import com.iortatechnxt.brokerverse.cashiering.domain.ApplicationRepository;
import com.iortatechnxt.brokerverse.cashiering.domain.Prebooked;
import com.iortatechnxt.brokerverse.cashiering.domain.PrebookedRepository;
import com.iortatechnxt.brokerverse.placement.service.ConfirmedPayment;
import com.iortatechnxt.brokerverse.placement.service.PaymentConfirmationSource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cashiering as a source of the NB payment gate (BRNB.067/068, CSHID.008/020, OQ12): placement's
 * {@code PAYMENT_CONFIRMATION_SWEEP} asks for the confirmed payments of the accounts awaiting
 * payment. Cashiering answers with each application that is not reversed ({@code APP:<id>}) and
 * each payment waiting in the pre-booked queue for the account ({@code PRE:<id>}: the account is
 * not booked yet, so this is how a payment received before booking opens the gate). It is not a
 * second payment intake: placement records only the gate evidence.
 */
@Component
@Transactional(readOnly = true)
public class CashieringPaymentConfirmationSource implements PaymentConfirmationSource {

  /** Source code on the gate evidence. */
  public static final String SOURCE = "CASHIERING";

  private final ApplicationRepository applications;
  private final PrebookedRepository prebooked;

  /**
   * Creates the source.
   *
   * @param applications applications
   * @param prebooked pre-booked payments
   */
  public CashieringPaymentConfirmationSource(
      ApplicationRepository applications, PrebookedRepository prebooked) {
    this.applications = applications;
    this.prebooked = prebooked;
  }

  @Override
  public String sourceCode() {
    return SOURCE;
  }

  @Override
  public List<ConfirmedPayment> confirmedFor(Long companyId, Collection<String> arns) {
    if (arns.isEmpty()) {
      return List.of();
    }
    List<ConfirmedPayment> confirmed = new ArrayList<>();
    for (Application a : applications.activeForArns(companyId, arns)) {
      confirmed.add(
          new ConfirmedPayment(
              a.getArn(),
              a.reference(),
              a.getAmount(),
              a.getValueDate(),
              "Cashiering application to " + a.getInvoiceNo()));
    }
    for (Prebooked p :
        prebooked.findByCompanyIdAndArnInAndStatus(companyId, arns, Prebooked.OPEN)) {
      confirmed.add(
          new ConfirmedPayment(
              p.getArn(),
              "PRE:" + p.getId(),
              p.getAmount(),
              p.getFirstSeen(),
              "Cashiering payment received before booking (" + p.getReference() + ")"));
    }
    return confirmed;
  }
}
