package com.iortatechnxt.brokerverse.remittance.service;

import com.iortatechnxt.brokerverse.booking.domain.ServiceInvoice;
import com.iortatechnxt.brokerverse.booking.service.ServiceInvoiceService;
import com.iortatechnxt.brokerverse.booking.service.ServiceInvoiceService.IssueRequest;
import com.iortatechnxt.brokerverse.common.util.Money;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceAmounts;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceBatch;
import com.iortatechnxt.brokerverse.remittance.service.RemittancePostings.Posting;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * The incentives of an approved batch (OPERATIONS_DESIGN 5 row 13; ACCOUNTING_DISBURSEMENT_DESIGN 6
 * rows 15-16):
 *
 * <ul>
 *   <li>early remittance incentive (RMTID.023): event {@code OPS_REMIT_INCENTIVE} ({@code
 *       RMB:<ref>:INC}) and, once per batch, the automatic service invoice of type {@code
 *       EARLY_INCENTIVE} to the insurer with the withholding tax of {@code
 *       EARLY_INCENTIVE_WTAX_RATE} (2%, DIS 3.29.1); the incentive OR carries the same tax;
 *   <li>CPC2 incentive on packaged Fire and Motor products (DIS 3.29.2): event {@code
 *       OPS_REMIT_CPC2} ({@code RMB:<ref>:CPC2}), recognised separately from the commission.
 * </ul>
 */
@Component
@Transactional(propagation = Propagation.MANDATORY)
public class BatchIncentives {

  /** Service invoice type of the early incentive (booking, V872). */
  public static final String EARLY_INCENTIVE_SI = "EARLY_INCENTIVE";

  private static final String INC = "INC";
  private static final String CPC2 = "CPC2";

  private final RemittancePostings postings;
  private final ServiceInvoiceService serviceInvoices;
  private final RemittanceSettings settings;

  /**
   * Creates the component.
   *
   * @param postings accounting events
   * @param serviceInvoices booking service invoices
   * @param settings parameters
   */
  public BatchIncentives(
      RemittancePostings postings,
      ServiceInvoiceService serviceInvoices,
      RemittanceSettings settings) {
    this.postings = postings;
    this.serviceInvoices = serviceInvoices;
    this.settings = settings;
  }

  /**
   * Posts the incentives of the batch's current cycle and issues the early-incentive SI once.
   *
   * @param batch approved batch
   * @param branchId branch of the postings
   * @param today value date
   */
  public void post(RemittanceBatch batch, Long branchId, LocalDate today) {
    RemittanceAmounts t = batch.getTotals();
    if (t.incentiveTotal().signum() > 0) {
      postings.publish(
          new Posting(
              RemittancePostings.INCENTIVE_EVENT,
              batch,
              branchId,
              today,
              RemittancePostings.sourceRef(batch, INC),
              null,
              RemittancePostings.incentiveAmounts(t, 1),
              "Early remittance incentive " + batch.getBatchNo()));
      issueServiceInvoice(batch, today);
    }
    if (t.cpc2Total().signum() > 0) {
      postings.publish(
          new Posting(
              RemittancePostings.CPC2_EVENT,
              batch,
              branchId,
              today,
              RemittancePostings.sourceRef(batch, CPC2),
              null,
              RemittancePostings.cpc2Amounts(t, 1),
              "CPC2 incentive " + batch.getBatchNo()));
    }
  }

  /**
   * Reverses the incentive postings of the batch's current cycle (DV cancelled, DIS 2.20.0). The
   * service invoice stays: it is issued once per batch and serves the next cycle.
   *
   * @param batch batch whose DV was cancelled
   * @param branchId branch of the postings
   * @param today value date
   */
  public void reverse(RemittanceBatch batch, Long branchId, LocalDate today) {
    RemittanceAmounts t = batch.getTotals();
    if (t.incentiveTotal().signum() > 0) {
      postings.publish(
          new Posting(
              RemittancePostings.INCENTIVE_EVENT,
              batch,
              branchId,
              today,
              RemittancePostings.sourceRef(batch, INC) + RemittancePostings.CANCEL,
              null,
              RemittancePostings.incentiveAmounts(t, -1),
              "Cancelled DV - early remittance incentive " + batch.getBatchNo()));
    }
    if (t.cpc2Total().signum() > 0) {
      postings.publish(
          new Posting(
              RemittancePostings.CPC2_EVENT,
              batch,
              branchId,
              today,
              RemittancePostings.sourceRef(batch, CPC2) + RemittancePostings.CANCEL,
              null,
              RemittancePostings.cpc2Amounts(t, -1),
              "Cancelled DV - CPC2 incentive " + batch.getBatchNo()));
    }
  }

  /**
   * The insurer's withholding tax on an early incentive (DIS 3.29.1).
   *
   * @param incentive incentive
   * @return tax at {@code EARLY_INCENTIVE_WTAX_RATE}
   */
  public BigDecimal wtaxOn(BigDecimal incentive) {
    return Money.round(incentive.multiply(settings.earlyIncentiveWtaxRate()).movePointLeft(2));
  }

  private void issueServiceInvoice(RemittanceBatch batch, LocalDate today) {
    if (batch.getSettlement().getEarlySiNo() != null) {
      return;
    }
    RemittanceAmounts t = batch.getTotals();
    BigDecimal wtax =
        batch.included().stream()
            .map(l -> wtaxOn(l.getAmounts().incentive()))
            .reduce(Money.zero(), BigDecimal::add);
    ServiceInvoice si =
        serviceInvoices.issue(
            new IssueRequest(
                batch.getCompanyId(),
                EARLY_INCENTIVE_SI,
                null,
                null,
                batch.getInsurerCode(),
                null,
                today,
                batch.getCurrency(),
                t.incentive(),
                t.incentiveVat(),
                wtax,
                "Early remittance incentive of batch " + batch.getBatchNo()));
    batch.earlyIncentiveInvoice(si.getSiNo(), wtax);
  }
}
