package com.iortatechnxt.brokerverse.disbursement.seed;

import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.DisbursementMode;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.InstrumentStatus;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.PayeeSource;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.RequestSource;
import com.iortatechnxt.brokerverse.disbursement.domain.FundingRequest;
import com.iortatechnxt.brokerverse.disbursement.domain.FundingRequest.FundingTerms;
import com.iortatechnxt.brokerverse.disbursement.domain.IntakeRequest;
import com.iortatechnxt.brokerverse.disbursement.domain.IntakeRequest.RequestFacts;
import com.iortatechnxt.brokerverse.disbursement.domain.Payee;
import com.iortatechnxt.brokerverse.disbursement.domain.Payee.PayeeDetails;
import com.iortatechnxt.brokerverse.disbursement.domain.PayeeAccount.AccountDetails;
import com.iortatechnxt.brokerverse.disbursement.domain.VoucherRepository;
import com.iortatechnxt.brokerverse.disbursement.service.DisbursementSettings;
import com.iortatechnxt.brokerverse.disbursement.service.EodService;
import com.iortatechnxt.brokerverse.disbursement.service.FundingService;
import com.iortatechnxt.brokerverse.disbursement.service.InstrumentActions;
import com.iortatechnxt.brokerverse.disbursement.service.InstrumentService;
import com.iortatechnxt.brokerverse.disbursement.service.InstrumentService.Change;
import com.iortatechnxt.brokerverse.disbursement.service.InstrumentUploads;
import com.iortatechnxt.brokerverse.disbursement.service.PayeeQueryService;
import com.iortatechnxt.brokerverse.disbursement.service.PayeeService;
import com.iortatechnxt.brokerverse.disbursement.service.RequestIntakeService;
import com.iortatechnxt.brokerverse.disbursement.service.VoucherActions;
import com.iortatechnxt.brokerverse.opsledger.seed.SeedUsers;
import com.iortatechnxt.brokerverse.organization.domain.Company;
import com.iortatechnxt.brokerverse.organization.domain.CompanyRepository;
import com.iortatechnxt.brokerverse.payables.domain.BankAccount;
import com.iortatechnxt.brokerverse.payables.service.BankAccountQueryService;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceBatch;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceBatchRepository;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceEnums.BatchStage;
import com.iortatechnxt.brokerverse.remittance.service.BatchService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Seed masters and storyline of Disbursement (seed profile only, idempotent; V999 is full, design
 * 4), after the Operations runners (order 97), each step signed in as the team member who does it:
 *
 * <ul>
 *   <li>the team leader ({@code disbtl}) maintains the payees of an insurer, a client and a
 *       supplier, which the approver ({@code disbappr}) authorises;
 *   <li>when Operations left more than one remittance batch waiting for review, the remittance team
 *       submits and approves the last one (the first stays in review for the Operations storyline):
 *       its voucher is built at once and routed to the approver, who approves it (posting); the
 *       processor ({@code disb}) prints the check and releases it. Otherwise a remittance requested
 *       by e-mail is encoded, checked and left for the approver;
 *   <li>a client refund is encoded, submitted, checked and approved (credit to account);
 *   <li>a supplier payment is encoded, checked, approved and paid by check; a second one is left
 *       with the processor;
 *   <li>the end of day of today produces the DCTF, the check batch, the vouchers and the reports;
 *   <li>the deposited-checks file tags a released check negotiated;
 *   <li>a funding of the main account goes through maker, verifier and two approvers.
 * </ul>
 *
 * A step that fails is logged and skipped.
 */
@Component
@Profile("seed")
@Order(97)
public class DisbursementSeedData implements ApplicationRunner {

  private static final Logger LOG = LoggerFactory.getLogger(DisbursementSeedData.class);
  private static final String PROCESSOR = "disb";
  private static final String LEADER = "disbtl";
  private static final String APPROVER = "disbappr";
  private static final String PHP = "PHP";
  private static final String CLIENT = "CL-2026-000001";
  private static final String SUPPLIER = "S-0001";
  private static final String SUPPLIER_TYPE = "SUPPLIER";
  private static final String INSURER = "INS-MGIC";
  private static final String APPROVED = "Approved";

  private final CompanyRepository companies;
  private final PayeeService payees;
  private final PayeeQueryService payeeQuery;
  private final RequestIntakeService intake;
  private final VoucherActions actions;
  private final VoucherRepository vouchers;
  private final InstrumentActions instruments;
  private final InstrumentService instrumentService;
  private final InstrumentUploads uploads;
  private final EodService eod;
  private final FundingService funding;
  private final BankAccountQueryService banks;
  private final RemittanceBatchRepository batches;
  private final BatchService batchService;
  private final Clock clock;
  private final SeedUsers users;

  /**
   * Creates the loader.
   *
   * @param companies companies
   * @param payees payee maintenance
   * @param payeeQuery payee reads
   * @param intake payment requests
   * @param actions voucher actions
   * @param vouchers vouchers
   * @param instruments instrument actions
   * @param instrumentService instruments
   * @param uploads bank file statuses
   * @param eod end of day
   * @param funding account funding
   * @param banks bank accounts
   * @param batches remittance batches
   * @param batchService Process Remittance
   * @param clock clock
   * @param users seed sign-in
   */
  @SuppressWarnings("java:S107") // constructor injection
  public DisbursementSeedData(
      CompanyRepository companies,
      PayeeService payees,
      PayeeQueryService payeeQuery,
      RequestIntakeService intake,
      VoucherActions actions,
      VoucherRepository vouchers,
      InstrumentActions instruments,
      InstrumentService instrumentService,
      InstrumentUploads uploads,
      EodService eod,
      FundingService funding,
      BankAccountQueryService banks,
      RemittanceBatchRepository batches,
      BatchService batchService,
      Clock clock,
      SeedUsers users) {
    this.companies = companies;
    this.payees = payees;
    this.payeeQuery = payeeQuery;
    this.intake = intake;
    this.actions = actions;
    this.vouchers = vouchers;
    this.instruments = instruments;
    this.instrumentService = instrumentService;
    this.uploads = uploads;
    this.eod = eod;
    this.funding = funding;
    this.banks = banks;
    this.batches = batches;
    this.batchService = batchService;
    this.clock = clock;
    this.users = users;
  }

  @Override
  public void run(ApplicationArguments args) {
    Long companyId = companies.findByCode("FVI").map(Company::getId).orElse(null);
    if (companyId == null || payeeQuery.byCode(companyId, CLIENT).isPresent()) {
      return;
    }
    step("payees", () -> masters(companyId));
    step("remittance voucher", () -> remittance(companyId));
    step("refund voucher", () -> refund(companyId));
    step("supplier check", () -> supplierCheck(companyId));
    step("supplier request", () -> supplier(companyId));
    step("end of day", () -> users.run(LEADER, () -> eod.run(companyId, today())));
    step("negotiated check", this::negotiated);
    step("account funding", () -> funding(companyId));
  }

  private void masters(Long companyId) {
    payee(companyId, INSURER, "INSURER", "MAPFRE Insular (Seed)", DisbursementMode.CHECK, null);
    payee(companyId, CLIENT, "CLIENT", "Client Refund Payee", DisbursementMode.CTA, "001122334455");
    payee(
        companyId, SUPPLIER, "SUPPLIER", "Metro Office Supplies Co.", DisbursementMode.CHECK, null);
  }

  private void payee(
      Long companyId, String code, String klass, String name, DisbursementMode mode, String acct) {
    if (payeeQuery.byCode(companyId, code).isPresent()) {
      return;
    }
    List<AccountDetails> accounts =
        acct == null
            ? List.of()
            : List.of(
                new AccountDetails("BDO Unibank, Inc.", "Makati", acct, name, PHP, mode, true));
    Payee p =
        users.as(
            LEADER,
            () ->
                payees.create(
                    companyId,
                    code,
                    new PayeeDetails(
                        klass,
                        name,
                        "Makati City",
                        "disbursement-seed@brokerverse-seed.ph",
                        null,
                        mode,
                        List.of(mode, DisbursementMode.CHECK, DisbursementMode.CTA).stream()
                            .distinct()
                            .toList(),
                        List.of(),
                        PHP,
                        null,
                        "Seed payee"),
                    accounts,
                    PayeeSource.MANUAL));
    users.run(LEADER, () -> payees.submit(p.getId()));
    users.run(APPROVER, () -> payees.authorize(p.getId()));
  }

  private void remittance(Long companyId) {
    List<RemittanceBatch> inReview =
        batches.findByStageInOrderByIdAsc(List.of(BatchStage.REVIEW_IN_PROCESS)).stream()
            .filter(b -> b.getCompanyId().equals(companyId))
            .toList();
    if (inReview.size() < 2) {
      LOG.info("Disbursement seed: the remittance batch in review stays with Operations");
      encodedRemittance(companyId);
      return;
    }
    RemittanceBatch batch = inReview.get(inReview.size() - 1);
    payee(
        companyId,
        batch.getInsurerCode(),
        "INSURER",
        batch.getInsurerCode(),
        DisbursementMode.CHECK,
        null);
    users.run("remit", () -> batchService.submit(batch.getId(), "Checked for Disbursement seed"));
    RemittanceBatch approved =
        users.as("remittl", () -> batchService.approve(batch.getId(), APPROVED));
    IntakeRequest request =
        users.as(PROCESSOR, () -> intake.byReference("REMITTANCE", approved.getBatchNo()));
    Long voucherId = request.getVoucherId();
    users.run(APPROVER, () -> actions.approve(voucherId, "Remittance approved"));
    users.run(PROCESSOR, () -> instruments.print(voucherId, Change.user("Printed")));
    users.run(PROCESSOR, () -> instruments.release(voucherId, "Insurer messenger"));
    LOG.info("Disbursement seed: remittance {} paid by check", approved.getBatchNo());
  }

  /** A remittance requested by e-mail, checked and waiting for the approver. */
  private void encodedRemittance(Long companyId) {
    IntakeRequest r =
        users.as(
            PROCESSOR,
            () ->
                intake.register(
                    encoded(
                        companyId,
                        "REMITTANCE",
                        INSURER,
                        new BigDecimal("125400.00"),
                        "Remittance of premiums collected per insurer statement")));
    Long voucherId = r.getVoucherId();
    users.run(PROCESSOR, () -> actions.submit(voucherId, "Statement of account attached"));
    users.run(LEADER, () -> actions.submitForApproval(voucherId, "Checked"));
  }

  private void negotiated() {
    // Only a printed or released check can be negotiated; which check vouchers have reached that
    // point depends on the end-of-day cut-off (Manila time), so pick one that has, not the first.
    Set<InstrumentStatus> negotiable =
        EnumSet.of(InstrumentStatus.PRINTED, InstrumentStatus.RELEASED);
    vouchers.findAll().stream()
        .filter(v -> v.getEodRunId() != null && v.getMode() == DisbursementMode.CHECK)
        .map(v -> users.as(PROCESSOR, () -> instrumentService.forVoucher(v.getId())))
        .filter(check -> negotiable.contains(check.getStatus()))
        .findFirst()
        .ifPresent(
            check ->
                users.run(
                    PROCESSOR,
                    () ->
                        uploads.negotiated(
                            check.getInstrumentNo(),
                            check.getAmount(),
                            "Deposited (seed)",
                            "SEED")));
  }

  private void refund(Long companyId) {
    IntakeRequest r =
        users.as(
            PROCESSOR,
            () ->
                intake.register(
                    encoded(
                        companyId,
                        "REFUND",
                        CLIENT,
                        new BigDecimal("2500.00"),
                        "Refund of overpayment")));
    Long voucherId = r.getVoucherId();
    users.run(PROCESSOR, () -> actions.submit(voucherId, "Refund documents complete"));
    users.run(LEADER, () -> actions.submitForApproval(voucherId, "Checked"));
    users.run("disbappr2", () -> actions.approve(voucherId, APPROVED));
  }

  private void supplierCheck(Long companyId) {
    IntakeRequest r =
        users.as(
            PROCESSOR,
            () ->
                intake.register(
                    encoded(
                        companyId,
                        SUPPLIER_TYPE,
                        SUPPLIER,
                        new BigDecimal("7980.00"),
                        "Printer toner and paper August")));
    Long voucherId = r.getVoucherId();
    users.run(PROCESSOR, () -> actions.submit(voucherId, "Invoice and delivery receipt attached"));
    users.run(LEADER, () -> actions.submitForApproval(voucherId, "Checked"));
    users.run(APPROVER, () -> actions.approve(voucherId, APPROVED));
    users.run(PROCESSOR, () -> instruments.print(voucherId, Change.user("Printed")));
    users.run(PROCESSOR, () -> instruments.release(voucherId, "Supplier representative"));
  }

  private void supplier(Long companyId) {
    users.run(
        PROCESSOR,
        () ->
            intake.register(
                encoded(
                    companyId,
                    SUPPLIER_TYPE,
                    SUPPLIER,
                    new BigDecimal("18450.00"),
                    "Office supplies September")));
  }

  private static RequestFacts encoded(
      Long companyId, String type, String payee, BigDecimal amount, String purpose) {
    return new RequestFacts(
        companyId,
        RequestSource.ENCODED,
        "DISBURSEMENT",
        null,
        null,
        type,
        null,
        payee,
        null,
        PHP,
        amount,
        purpose,
        null,
        List.of(),
        List.of(),
        false,
        null,
        null,
        null,
        null);
  }

  private void funding(Long companyId) {
    List<BankAccount> php = banks.listActive(companyId, PHP);
    if (php.size() < 2) {
      return;
    }
    FundingRequest f =
        users.as(
            LEADER,
            () ->
                funding.create(
                    companyId,
                    new FundingTerms(
                        php.get(1).getId(),
                        php.get(0).getId(),
                        new BigDecimal("500000.00"),
                        PHP,
                        "Funding of the main BDOIR account for the week's checks",
                        today(),
                        null)));
    users.run(LEADER, () -> funding.submit(f.getId()));
    users.run("disbtl2", () -> funding.verify(f.getId(), "Balances checked"));
    users.run(APPROVER, () -> funding.approve(f.getId(), APPROVED, null));
    users.run("disbappr2", () -> funding.approve(f.getId(), APPROVED, "BOB-2026-900001"));
  }

  /** The business day in Manila: end of day and its cut-off work on the Philippine date. */
  private LocalDate today() {
    return LocalDate.now(clock.withZone(DisbursementSettings.MANILA));
  }

  private static void step(String name, Runnable work) {
    try {
      work.run();
    } catch (RuntimeException ex) {
      LOG.warn("Disbursement seed {} skipped: {}", name, ex.getMessage());
    }
  }
}
