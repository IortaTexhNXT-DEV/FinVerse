package com.iortatechnxt.brokerverse.prodrecon.demo;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.opsledger.demo.DemoUsers;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerQueryService;
import com.iortatechnxt.brokerverse.organization.service.OrganizationService;
import com.iortatechnxt.brokerverse.prodrecon.domain.ReconCycleRepository;
import com.iortatechnxt.brokerverse.prodrecon.domain.ReconEnums.ExtractTrigger;
import com.iortatechnxt.brokerverse.prodrecon.domain.ReconExtract;
import com.iortatechnxt.brokerverse.prodrecon.service.ProductionExtractService;
import com.iortatechnxt.brokerverse.prodrecon.service.ProductionExtractService.ExtractRequest;
import com.iortatechnxt.brokerverse.prodrecon.service.ReconSendService;
import com.iortatechnxt.brokerverse.prodrecon.service.ReconUploadService;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Demo start-up (demo profile only, idempotent), signed in as the reconciliation handler ({@code
 * recon}), once the demo bookings are in the Operations ledger and the other Operations demos ran
 * (order 94): the September 2026 production register of INS-MGIC is extracted and sent, and the
 * insurer's answer is uploaded. The motor invoice of ARN-2026-940001 matches, the property invoice
 * of ARN-2026-940002 differs by its gross premium and a policy BDOI never booked waits in the
 * unbooked repository, so the cycle opens in "reconciling" with its discrepancies.
 */
@Component
@Profile("demo")
@Order(94)
public class ProdReconDemoData implements ApplicationRunner {

  private static final Logger LOG = LoggerFactory.getLogger(ProdReconDemoData.class);
  private static final String INSURER = "INS-MGIC";
  private static final String HANDLER = "recon";
  private static final LocalDate MONTH = LocalDate.of(2026, 9, 1);
  private static final BigDecimal DISCREPANCY = new BigDecimal("150.00");
  private static final String HEADER =
      "Month of Production,Insurer,Invoice Number,Policy No.,Assured Name,Gross Premium,"
          + "Gross Commission,Basic Premium,Incentive,Remarks";

  private final ProductionExtractService extracts;
  private final ReconSendService sender;
  private final ReconUploadService uploads;
  private final ReconCycleRepository cycles;
  private final InvoiceLedgerQueryService ledger;
  private final OrganizationService organization;
  private final DemoUsers users;

  /**
   * Creates the loader.
   *
   * @param extracts extraction
   * @param sender register sending
   * @param uploads insurer feedback upload
   * @param cycles cycles (idempotency)
   * @param ledger Operations ledger
   * @param organization demo company
   * @param users demo sign-in
   */
  public ProdReconDemoData(
      ProductionExtractService extracts,
      ReconSendService sender,
      ReconUploadService uploads,
      ReconCycleRepository cycles,
      InvoiceLedgerQueryService ledger,
      OrganizationService organization,
      DemoUsers users) {
    this.extracts = extracts;
    this.sender = sender;
    this.uploads = uploads;
    this.cycles = cycles;
    this.ledger = ledger;
    this.organization = organization;
    this.users = users;
  }

  @Override
  public void run(ApplicationArguments args) {
    organization.listCompanies().stream()
        .filter(c -> "FVI".equals(c.getCode()))
        .findFirst()
        .ifPresent(c -> users.run(HANDLER, () -> reconcile(c.getId())));
  }

  private void reconcile(Long companyId) {
    if (cycles
        .findByCompanyIdAndInsurerCodeAndProductionMonthAndClosedFalse(companyId, INSURER, MONTH)
        .isPresent()) {
      return;
    }
    try {
      ReconExtract extract =
          extracts.extract(
              new ExtractRequest(
                  companyId, INSURER, MONTH, MONTH.withDayOfMonth(MONTH.lengthOfMonth())),
              ExtractTrigger.MANUAL);
      LOG.info(
          "Production reconciliation demo: {} with {} line(s)",
          extract.getExtractNo(),
          extract.getRowCount());
      sender.send(extract.getId(), List.of("production@mgic-demo.ph"), null);
      uploads.upload(
          companyId,
          "MGIC_PRODUCTION_202609.csv",
          insurerAnswer().getBytes(StandardCharsets.UTF_8));
    } catch (BusinessRuleException | ResourceNotFoundException ex) {
      LOG.warn("Production reconciliation demo skipped: {}", ex.getMessage());
    }
  }

  private String insurerAnswer() {
    List<String> rows = new ArrayList<>();
    rows.add(HEADER);
    original("ARN-2026-940001").ifPresent(i -> rows.add(row(i, BigDecimal.ZERO, "As booked")));
    original("ARN-2026-940002")
        .ifPresent(i -> rows.add(row(i, DISCREPANCY, "Insurer premium includes a surcharge")));
    rows.add(
        "2026-09,INS-MGIC,,MGIC-MC-2026-99001,\"Juan Dela Cruz\",18500.00,3237.50,,,"
            + "Policy issued by the insurer; not booked by BDOI");
    return String.join("\n", rows) + "\n";
  }

  private static String row(OpsInvoice i, BigDecimal delta, String remarks) {
    return String.join(
        ",",
        "2026-09",
        INSURER,
        i.getInvoiceNo(),
        i.getPolicyNo() == null ? "" : i.getPolicyNo(),
        "\"" + i.getAssuredName().replace("\"", "\"\"") + "\"",
        i.getGrossPremium().add(delta).toPlainString(),
        i.getCommission().toPlainString(),
        "",
        "",
        remarks);
  }

  private Optional<OpsInvoice> original(String arn) {
    return ledger.forArn(arn).stream().filter(i -> i.getEndorsementNo() == null).findFirst();
  }
}
