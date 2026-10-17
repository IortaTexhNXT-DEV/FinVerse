package com.iortatechnxt.brokerverse.placement.service;

import com.iortatechnxt.brokerverse.placement.domain.MatchStatus;
import com.iortatechnxt.brokerverse.placement.domain.PaymentReportKind;
import com.iortatechnxt.brokerverse.placement.domain.PaymentReportLine;
import com.iortatechnxt.brokerverse.placement.domain.PaymentReportRepository;
import com.iortatechnxt.brokerverse.placement.domain.PaymentReportStatus;
import java.util.Collection;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Today's {@link PaymentConfirmationSource}: the lines of confirmed CLPC and payment reports
 * matched as paid to an account (BRNB.067/068).
 */
@Component
public class ReportPaymentSource implements PaymentConfirmationSource {

  /** Source code stored with the evidence. */
  public static final String SOURCE = "PAYMENT_REPORT";

  private final PaymentReportRepository reports;

  /**
   * Creates the source.
   *
   * @param reports payment reports
   */
  public ReportPaymentSource(PaymentReportRepository reports) {
    this.reports = reports;
  }

  @Override
  public String sourceCode() {
    return SOURCE;
  }

  @Override
  @Transactional(readOnly = true)
  public List<ConfirmedPayment> confirmedFor(Long companyId, Collection<String> arns) {
    return reports.confirmedLines(arns, PaymentReportStatus.CONFIRMED, MatchStatus.MATCHED).stream()
        .filter(l -> l.getReport().getCompanyId().equals(companyId))
        .map(ReportPaymentSource::confirmation)
        .toList();
  }

  /**
   * The confirmation of a matched, paid report line.
   *
   * @param line report line
   * @return confirmation
   */
  static ConfirmedPayment confirmation(PaymentReportLine line) {
    String kind = line.getReport().getKind() == PaymentReportKind.CLPC ? "CLPC" : "Payment";
    return new ConfirmedPayment(
        line.getArn(),
        line.getReport().getReportNo() + "#" + line.getRowNo(),
        line.getAmount(),
        line.getPaidOn(),
        kind + " report " + line.getReport().getReportNo() + " (" + line.getReference() + ")");
  }
}
