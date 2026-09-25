package com.iortatechnxt.brokerverse.commission.service;

import com.iortatechnxt.brokerverse.commission.domain.CertificateSubmission;
import com.iortatechnxt.brokerverse.commission.domain.CertificateSubmissionRepository;
import com.iortatechnxt.brokerverse.commission.domain.CommissionEnums.DpTag;
import com.iortatechnxt.brokerverse.commission.domain.DpBilling;
import com.iortatechnxt.brokerverse.commission.domain.DpBillingRepository;
import com.iortatechnxt.brokerverse.commission.domain.DpItem;
import com.iortatechnxt.brokerverse.commission.domain.DpItemRepository;
import com.iortatechnxt.brokerverse.opsledger.service.port.InvoiceRelatedItems;
import com.iortatechnxt.brokerverse.opsledger.service.port.OpsWorkCountSource;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Commission receivables on the Operations home and the invoice 360 view (BRQID.003, RMTID.026):
 * accounts to confirm and to bill, billings awaiting the insurer (red when the feedback is overdue,
 * CMRID.011), billings to collect and certificates waiting for Comptrollership; the DP accounts of
 * an invoice in the Commission section.
 */
@Component
@Transactional(readOnly = true)
public class CommissionWorkCounts implements OpsWorkCountSource, InvoiceRelatedItems {

  private static final String VALIDATION = "/commission/dp/items";
  private static final String BILLINGS = "/commission/dp/billings";

  private final DpItemRepository items;
  private final DpBillingRepository billings;
  private final CertificateSubmissionRepository certificates;
  private final Clock clock;

  /**
   * Creates the source.
   *
   * @param items DP accounts
   * @param billings billings
   * @param certificates certificate submissions
   * @param clock clock
   */
  public CommissionWorkCounts(
      DpItemRepository items,
      DpBillingRepository billings,
      CertificateSubmissionRepository certificates,
      Clock clock) {
    this.items = items;
    this.billings = billings;
    this.certificates = certificates;
    this.clock = clock;
  }

  @Override
  public List<WorkCount> counts(Long companyId) {
    return List.of(
        tile(
            "DP_TO_CONFIRM",
            "DP Accounts to Confirm",
            items.countByCompanyIdAndTag(companyId, DpTag.DP_FOR_CONFIRMATION),
            Severity.INFO,
            VALIDATION),
        tile(
            "DP_TO_BILL",
            "DP Accounts for Billing",
            items.countByCompanyIdAndTag(companyId, DpTag.DP_FOR_BILLING),
            Severity.INFO,
            VALIDATION),
        tile(
            "DP_AWAITING",
            "Billings Awaiting Insurer",
            billings.countByCompanyIdAndStage(companyId, DpBilling.AWAITING),
            Severity.WARNING,
            BILLINGS),
        tile(
            "DP_OVERDUE",
            "Insurer Feedback Overdue",
            billings.countByCompanyIdAndStageAndSlaDueBefore(
                companyId, DpBilling.AWAITING, LocalDate.now(clock)),
            Severity.ALERT,
            BILLINGS),
        tile(
            "DP_TO_COLLECT",
            "Approved Billings to Collect",
            billings.countByCompanyIdAndStage(companyId, DpBilling.APPROVED),
            Severity.WARNING,
            BILLINGS),
        tile(
            "BIR_CERT_SUBMITTED",
            "BIR Certificates to Acknowledge",
            certificates.countByCompanyIdAndStage(companyId, CertificateSubmission.SUBMITTED),
            Severity.INFO,
            "/commission/certificates"));
  }

  private static WorkCount tile(
      String key, String label, long count, Severity severity, String link) {
    return new WorkCount(OpsWorkCountSource.Section.COMMISSION, key, label, count, severity, link);
  }

  @Override
  public InvoiceRelatedItems.Section section() {
    return InvoiceRelatedItems.Section.COMMISSION;
  }

  @Override
  public List<RelatedItem> itemsFor(String invoiceNo) {
    return items.findByInvoiceNoOrderByIdDesc(invoiceNo).stream()
        .map(CommissionWorkCounts::related)
        .toList();
  }

  private static RelatedItem related(DpItem i) {
    return new RelatedItem(
        "DP_ACCOUNT",
        i.getOrNo() == null ? "DP #" + i.getId() : i.getOrNo(),
        i.getCollectedOn(),
        i.getNetCommission(),
        i.getTag().name(),
        "Direct payment commission"
            + (i.getBranchCode() == null ? "" : " listed by " + i.getBranchCode()),
        i.getBillingId() == null ? VALIDATION : BILLINGS + "/" + i.getBillingId());
  }
}
