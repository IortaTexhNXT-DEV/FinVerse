package com.iortatechnxt.brokerverse.payables.seed;

import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.organization.domain.Branch;
import com.iortatechnxt.brokerverse.organization.domain.Company;
import com.iortatechnxt.brokerverse.organization.service.OrganizationService;
import com.iortatechnxt.brokerverse.payables.domain.InvoiceLineValues;
import com.iortatechnxt.brokerverse.payables.domain.PaymentMode;
import com.iortatechnxt.brokerverse.payables.domain.PaymentVoucher;
import com.iortatechnxt.brokerverse.payables.domain.SupplierInvoice;
import com.iortatechnxt.brokerverse.payables.domain.SupplierInvoiceRepository;
import com.iortatechnxt.brokerverse.payables.service.BankAccountQueryService;
import com.iortatechnxt.brokerverse.payables.service.InvoiceCommand;
import com.iortatechnxt.brokerverse.payables.service.PaymentApprovalService;
import com.iortatechnxt.brokerverse.payables.service.PaymentCommand;
import com.iortatechnxt.brokerverse.payables.service.PaymentVoucherService;
import com.iortatechnxt.brokerverse.payables.service.SupplierInvoiceService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * SEED PROFILE ONLY: payables of the seed date for the seed company FVI (conformance gap G03),
 * after the tax masters (order 95) so the supplier's withholding profile applies: the monthly cloud
 * subscription of S-0002 invoiced today with expanded withholding, approved, and paid today by bank
 * transfer from the company's default bank account BDO-CA. The monthly and quarterly alphalists of
 * payees and the bank payment notification of the month have rows from day one. Idempotent: nothing
 * is created when the invoice of the month exists.
 */
@Component
@Profile("seed")
@Order(96)
public class CurrentPeriodSeedData implements ApplicationRunner {

  private static final Logger LOG = LoggerFactory.getLogger(CurrentPeriodSeedData.class);
  private static final String COMPANY = "FVI";
  private static final String BRANCH = "HO";
  private static final String SUPPLIER = "S-0002";
  private static final String BANK = "BDO-CA";
  private static final String ACCOUNT = "5610";
  private static final String COST_CENTER = "IT";
  private static final String DEPARTMENT = "FND";
  private static final BigDecimal NET = new BigDecimal("48000.00");

  private final OrganizationService organization;
  private final SupplierInvoiceRepository invoices;
  private final SupplierInvoiceService invoiceService;
  private final PaymentVoucherService vouchers;
  private final PaymentApprovalService approvals;
  private final BankAccountQueryService banks;
  private final SeedActor actor;
  private final Clock clock;

  /**
   * Creates the loader.
   *
   * @param organization companies and branches
   * @param invoices supplier invoices (idempotency)
   * @param invoiceService supplier invoices
   * @param vouchers payment vouchers
   * @param approvals voucher approval
   * @param banks bank accounts
   * @param actor SIT/UAT users
   * @param clock clock
   */
  @SuppressWarnings("java:S107") // constructor injection
  public CurrentPeriodSeedData(
      OrganizationService organization,
      SupplierInvoiceRepository invoices,
      SupplierInvoiceService invoiceService,
      PaymentVoucherService vouchers,
      PaymentApprovalService approvals,
      BankAccountQueryService banks,
      SeedActor actor,
      Clock clock) {
    this.organization = organization;
    this.invoices = invoices;
    this.invoiceService = invoiceService;
    this.vouchers = vouchers;
    this.approvals = approvals;
    this.banks = banks;
    this.actor = actor;
    this.clock = clock;
  }

  @Override
  public void run(ApplicationArguments args) {
    organization.listCompanies().stream()
        .filter(c -> COMPANY.equals(c.getCode()))
        .findFirst()
        .map(Company::getId)
        .ifPresent(this::load);
  }

  private void load(Long companyId) {
    LocalDate today = BusinessClock.today(clock);
    String invoiceNo = SUPPLIER + "/CLOUD-" + today.getYear() + "-" + today.getMonthValue();
    Long branchId =
        organization.listBranches(companyId).stream()
            .filter(b -> BRANCH.equals(b.getCode()))
            .findFirst()
            .map(Branch::getId)
            .orElse(null);
    if (branchId != null && !invoices.existsByCompanyIdAndSupplierInvoiceNo(companyId, invoiceNo)) {
      payInvoiceOfTheMonth(companyId, branchId, invoiceNo, today);
    }
  }

  private void payInvoiceOfTheMonth(
      Long companyId, Long branchId, String invoiceNo, LocalDate today) {
    try {
      InvoiceCommand command =
          new InvoiceCommand(
              companyId,
              branchId,
              SUPPLIER,
              invoiceNo,
              today,
              null,
              null,
              true,
              "Cloud subscription of the month",
              List.of(
                  new InvoiceLineValues(
                      ACCOUNT, COST_CENTER, "Cloud hosting and backup subscription", NET)));
      SupplierInvoice draft =
          actor.as(
              SeedActor.MAKER, () -> invoiceService.submit(invoiceService.create(command).getId()));
      SupplierInvoice approved =
          actor.as(SeedActor.CHECKER, () -> invoiceService.approve(draft.getId()));
      PaymentCommand payment =
          new PaymentCommand(
              companyId,
              branchId,
              SUPPLIER,
              null,
              null,
              PaymentMode.BANK_TRANSFER,
              banks.getByCode(companyId, BANK).getId(),
              today,
              null,
              DEPARTMENT,
              "Settlement of the cloud subscription of the month",
              List.of(new PaymentCommand.ItemPayment(approved.getOpenItemId(), null)));
      PaymentVoucher voucher =
          actor.as(SeedActor.MAKER, () -> vouchers.submit(vouchers.create(payment).getId()));
      actor.as(SeedActor.CHECKER, () -> approvals.approve(voucher.getId()));
      LOG.info("Payables seed of the month: {} paid", approved.getDocumentNo());
    } catch (RuntimeException ex) {
      LOG.warn("Payables seed of the month skipped: {}", ex.getMessage());
    }
  }
}
