package com.iortatechnxt.brokerverse.commission.seed;

import com.iortatechnxt.brokerverse.commission.domain.CommissionEnums.DpTag;
import com.iortatechnxt.brokerverse.commission.domain.CommissionEnums.ListSource;
import com.iortatechnxt.brokerverse.commission.domain.DpBilling;
import com.iortatechnxt.brokerverse.commission.domain.DpItem;
import com.iortatechnxt.brokerverse.commission.domain.DpItem.Submission;
import com.iortatechnxt.brokerverse.commission.domain.DpItemRepository;
import com.iortatechnxt.brokerverse.commission.domain.DpList;
import com.iortatechnxt.brokerverse.commission.domain.DpList.FileKey;
import com.iortatechnxt.brokerverse.commission.domain.DpList.Origin;
import com.iortatechnxt.brokerverse.commission.service.DpBillingSender;
import com.iortatechnxt.brokerverse.commission.service.DpBillingService;
import com.iortatechnxt.brokerverse.commission.service.DpIntakeService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.seed.SeedUsers;
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
 * Seed start-up (seed profile only, idempotent), signed in as the commission handler ({@code
 * commrec}): the direct payment booking of the seed (ARN-2026-940003) arrives on the Head Office DP
 * list of 30 September 2026 through the real intake (validation and sanitation), is confirmed as
 * fully paid to the insurer, gathered into a commission billing and sent to INS-MGIC, which now has
 * ten working days to answer.
 */
@Component
@Profile("seed")
@Order(96)
public class CommissionSeedData implements ApplicationRunner {

  private static final Logger LOG = LoggerFactory.getLogger(CommissionSeedData.class);
  private static final String ARN = "ARN-2026-940003";
  private static final LocalDate LIST_DATE = LocalDate.parse("2026-09-30");
  private static final String HANDLER = "commrec";

  private final InvoiceLedgerQueryService ledger;
  private final DpIntakeService intake;
  private final DpBillingService billings;
  private final DpItemRepository items;
  private final DpBillingSender sender;
  private final SeedUsers users;
  private final TransactionTemplate tx;

  /**
   * Creates the loader.
   *
   * @param ledger Operations ledger
   * @param intake DP intake
   * @param billings billings
   * @param items DP accounts (idempotency)
   * @param sender billing e-mail
   * @param users seed sign-in
   * @param txManager transaction manager
   */
  public CommissionSeedData(
      InvoiceLedgerQueryService ledger,
      DpIntakeService intake,
      DpBillingService billings,
      DpItemRepository items,
      DpBillingSender sender,
      SeedUsers users,
      PlatformTransactionManager txManager) {
    this.ledger = ledger;
    this.intake = intake;
    this.billings = billings;
    this.items = items;
    this.sender = sender;
    this.users = users;
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
      List<DpBilling> billed = users.as(HANDLER, () -> tx.execute(s -> seed(invoice)));
      if (billed != null && !billed.isEmpty()) {
        users.as(
            HANDLER,
            () -> sender.send(billed.get(0).getId(), List.of("commission@mgic-seed.ph"), null));
      }
      LOG.info(
          "Commission seed: DP account {} billed to {}",
          invoice.getInvoiceNo(),
          invoice.getInsurerCode());
    } catch (BusinessRuleException | ResourceNotFoundException ex) {
      LOG.warn("Commission seed skipped: {}", ex.getMessage());
    }
  }

  private List<DpBilling> seed(OpsInvoice invoice) {
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
    if (valid.isEmpty()) {
      return List.of();
    }
    intake.confirm(valid);
    return billings.prepare(invoice.getCompanyId(), valid);
  }
}
