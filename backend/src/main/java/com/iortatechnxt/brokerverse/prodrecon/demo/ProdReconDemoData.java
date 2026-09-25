package com.iortatechnxt.brokerverse.prodrecon.demo;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.organization.service.OrganizationService;
import com.iortatechnxt.brokerverse.prodrecon.domain.ReconCycleRepository;
import com.iortatechnxt.brokerverse.prodrecon.domain.ReconEnums.ExtractTrigger;
import com.iortatechnxt.brokerverse.prodrecon.domain.ReconExtract;
import com.iortatechnxt.brokerverse.prodrecon.service.ProductionExtractService;
import com.iortatechnxt.brokerverse.prodrecon.service.ProductionExtractService.ExtractRequest;
import java.time.LocalDate;
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
 * Demo start-up (demo profile only, idempotent): once the demo bookings are in the Operations
 * ledger ({@code OpsLedgerDemoReplay}, order 90), extracts the September 2026 production register
 * of INS-MGIC through the real extraction service, so the cycles board and the workbench open on a
 * cycle waiting to be sent to the insurer.
 */
@Component
@Profile("demo")
@Order(95)
public class ProdReconDemoData implements ApplicationRunner {

  private static final Logger LOG = LoggerFactory.getLogger(ProdReconDemoData.class);
  private static final String INSURER = "INS-MGIC";
  private static final LocalDate MONTH = LocalDate.of(2026, 9, 1);

  private final ProductionExtractService extracts;
  private final ReconCycleRepository cycles;
  private final OrganizationService organization;
  private final TransactionTemplate tx;

  /**
   * Creates the loader.
   *
   * @param extracts extraction
   * @param cycles cycles (idempotency)
   * @param organization demo company
   * @param txManager transaction manager
   */
  public ProdReconDemoData(
      ProductionExtractService extracts,
      ReconCycleRepository cycles,
      OrganizationService organization,
      PlatformTransactionManager txManager) {
    this.extracts = extracts;
    this.cycles = cycles;
    this.organization = organization;
    this.tx = new TransactionTemplate(txManager);
  }

  @Override
  public void run(ApplicationArguments args) {
    organization.listCompanies().stream()
        .filter(c -> "FVI".equals(c.getCode()))
        .findFirst()
        .ifPresent(c -> extract(c.getId()));
  }

  private void extract(Long companyId) {
    if (cycles
        .findByCompanyIdAndInsurerCodeAndProductionMonthAndClosedFalse(companyId, INSURER, MONTH)
        .isPresent()) {
      return;
    }
    try {
      ReconExtract extract =
          tx.execute(
              s ->
                  extracts.extract(
                      new ExtractRequest(
                          companyId, INSURER, MONTH, MONTH.withDayOfMonth(MONTH.lengthOfMonth())),
                      ExtractTrigger.MANUAL));
      if (extract != null) {
        LOG.info(
            "Production reconciliation demo: {} with {} line(s)",
            extract.getExtractNo(),
            extract.getRowCount());
      }
    } catch (BusinessRuleException | ResourceNotFoundException ex) {
      LOG.warn("Production reconciliation demo skipped: {}", ex.getMessage());
    }
  }
}
