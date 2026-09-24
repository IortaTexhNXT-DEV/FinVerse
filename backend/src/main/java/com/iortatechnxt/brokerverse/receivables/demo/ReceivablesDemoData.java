package com.iortatechnxt.brokerverse.receivables.demo;

import com.iortatechnxt.brokerverse.organization.domain.Branch;
import com.iortatechnxt.brokerverse.organization.domain.Company;
import com.iortatechnxt.brokerverse.organization.service.OrganizationService;
import com.iortatechnxt.brokerverse.receivables.domain.Receipt;
import com.iortatechnxt.brokerverse.receivables.domain.ReceiptRepository;
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
 * Loads the receivables demo data set (demo profile only, idempotent): debit notes when the
 * underwriting demo did not create any, about 80 receipts January-September 2026, weekly deposit
 * slips, a bounced cheque, post-dated cheques, rent cheques, the bank statements of account 1111
 * and a finalized August bank reconciliation.
 *
 * <p>Runs after the underwriting demo (order 45) so that its debit notes are collected first.
 */
@Component
@Profile("demo")
@Order(45)
public class ReceivablesDemoData implements ApplicationRunner {

  private static final Logger LOG = LoggerFactory.getLogger(ReceivablesDemoData.class);
  private static final String COMPANY = "FVI";
  private static final LocalDate APPLICATION_DATE = LocalDate.of(2026, 9, 10);

  private final OrganizationService organization;
  private final ReceiptRepository receipts;
  private final DemoDebitNotes debitNotes;
  private final DemoCollections collections;
  private final DemoBanking banking;
  private final DemoBankStatements statements;

  /**
   * Creates the loader.
   *
   * @param organization organization service
   * @param receipts receipt repository
   * @param debitNotes debit note builder
   * @param collections receipt builder
   * @param banking banking builder
   * @param statements bank statement builder
   */
  public ReceivablesDemoData(
      OrganizationService organization,
      ReceiptRepository receipts,
      DemoDebitNotes debitNotes,
      DemoCollections collections,
      DemoBanking banking,
      DemoBankStatements statements) {
    this.organization = organization;
    this.receipts = receipts;
    this.debitNotes = debitNotes;
    this.collections = collections;
    this.banking = banking;
    this.statements = statements;
  }

  @Override
  public void run(ApplicationArguments args) {
    Optional<Company> company =
        organization.listCompanies().stream().filter(c -> COMPANY.equals(c.getCode())).findFirst();
    if (company.isEmpty() || receipts.countByCompanyId(company.get().getId()) > 0) {
      return;
    }
    DemoContext ctx = context(company.get());
    int notes = debitNotes.ensure(ctx);
    List<Receipt> created = new ArrayList<>(collections.run(ctx));
    int slips = banking.depositWeekly(ctx);
    banking.bounceOne(created);
    int pdcs = banking.processPdcs(ctx);
    collections.applyOnAccount(APPLICATION_DATE);
    banking.payRent(ctx);
    statements.run(ctx);
    LOG.info(
        "Receivables demo data: {} debit notes, {} receipts, {} deposit slips, {} PDCs banked",
        notes,
        created.size(),
        slips,
        pdcs);
  }

  private DemoContext context(Company company) {
    List<Branch> branches = organization.listBranches(company.getId());
    Long headOffice =
        branches.stream()
            .filter(Branch::isHeadOffice)
            .map(Branch::getId)
            .findFirst()
            .orElse(branches.get(0).getId());
    List<Long> ids = new ArrayList<>();
    ids.add(headOffice);
    branches.stream().map(Branch::getId).filter(id -> !id.equals(headOffice)).forEach(ids::add);
    return new DemoContext(company.getId(), headOffice, ids);
  }
}
