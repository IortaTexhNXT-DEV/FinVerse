package com.iortatechnxt.brokerverse.commission.demo;

import com.iortatechnxt.brokerverse.commission.domain.CommissionEnums.DpTag;
import com.iortatechnxt.brokerverse.commission.domain.CommissionEnums.ListSource;
import com.iortatechnxt.brokerverse.commission.domain.DpItem;
import com.iortatechnxt.brokerverse.commission.domain.DpItem.Submission;
import com.iortatechnxt.brokerverse.commission.domain.DpItemRepository;
import com.iortatechnxt.brokerverse.commission.domain.DpList;
import com.iortatechnxt.brokerverse.commission.domain.DpList.FileKey;
import com.iortatechnxt.brokerverse.commission.domain.DpList.Origin;
import com.iortatechnxt.brokerverse.commission.service.DpBillingService;
import com.iortatechnxt.brokerverse.commission.service.DpIntakeService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerQueryService;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Demo start-up (demo profile only, idempotent): the direct payment booking of the demo
 * (ARN-2026-940003) arrives on the Head Office DP list of 30 September 2026 through the real intake
 * (validation and sanitation), is confirmed as fully paid to the insurer and gathered into a
 * commission billing ready to send to INS-MGIC.
 */
@Component
@Profile("demo")
@Order(96)
public class CommissionDemoData implements ApplicationRunner {

  private static final Logger LOG = LoggerFactory.getLogger(CommissionDemoData.class);
  private static final String ARN = "ARN-2026-940003";
  private static final LocalDate LIST_DATE = LocalDate.parse("2026-09-30");

  private final InvoiceLedgerQueryService ledger;
  private final DpIntakeService intake;
  private final DpBillingService billings;
  private final DpItemRepository items;
  private final TransactionTemplate tx;

  /**
   * Creates the loader.
   *
   * @param ledger Operations ledger
   * @param intake DP intake
   * @param billings billings
   * @param items DP accounts (idempotency)
   * @param txManager transaction manager
   */
  public CommissionDemoData(
      InvoiceLedgerQueryService ledger,
      DpIntakeService intake,
      DpBillingService billings,
      DpItemRepository items,
      PlatformTransactionManager txManager) {
    this.ledger = ledger;
    this.intake = intake;
    this.billings = billings;
    this.items = items;
    this.tx = new TransactionTemplate(txManager);
  }

  @Override
  public void run(ApplicationArguments args) {
    List<OpsInvoice> invoices = ledger.forArn(ARN);
    if (invoices.isEmpty()
        || !items.findByInvoiceNoOrderByIdDesc(invoices.get(0).getInvoiceNo()).isEmpty()) {
      return;
    }
    OpsInvoice invoice = invoices.get(0);
    try {
      tx.executeWithoutResult(s -> seed(invoice));
      LOG.info(
          "Commission demo: DP account {} billed to {}",
          invoice.getInvoiceNo(),
          invoice.getInsurerCode());
    } catch (BusinessRuleException | ResourceNotFoundException ex) {
      LOG.warn("Commission demo skipped: {}", ex.getMessage());
    }
  }

  private void seed(OpsInvoice invoice) {
    DpList list =
        intake.open(
            invoice.getCompanyId(),
            new Origin(ListSource.HEAD_OFFICE, "HO", LIST_DATE),
            FileKey.NONE);
    intake.add(
        list.getId(),
        1,
        new Submission(
            invoice.getInvoiceNo(),
            invoice.getPolicyNo(),
            invoice.getInsurerCode(),
            invoice.getGrossPremium(),
            "Paid directly to the insurer",
            "HO"));
    intake.finish(list.getId(), null);
    List<Long> valid =
        items.findByListIdOrderByRowNoAscIdAsc(list.getId()).stream()
            .filter(i -> i.getTag() == DpTag.DP_FOR_CONFIRMATION)
            .map(DpItem::getId)
            .toList();
    if (!valid.isEmpty()) {
      intake.confirm(valid);
      billings.prepare(invoice.getCompanyId(), valid);
    }
  }
}
