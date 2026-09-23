package com.iortatechnxt.finverse.payables.service;

import com.iortatechnxt.finverse.common.util.Money;
import com.iortatechnxt.finverse.organization.domain.Branch;
import com.iortatechnxt.finverse.organization.domain.Company;
import com.iortatechnxt.finverse.organization.service.OrganizationService;
import com.iortatechnxt.finverse.payables.domain.InvoiceLineValues;
import com.iortatechnxt.finverse.payables.domain.PaymentMode;
import com.iortatechnxt.finverse.payables.domain.PaymentVoucher;
import com.iortatechnxt.finverse.payables.domain.PdcStatus;
import com.iortatechnxt.finverse.payables.domain.SupplierInvoice;
import com.iortatechnxt.finverse.payables.domain.SupplierInvoiceRepository;
import com.iortatechnxt.finverse.subledger.service.OpenItemService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Demo payables data for the FVI demo company, generated through the services so that every
 * document has real journals and sub-ledger items: 60 supplier invoices of S-0001..S-0003 from
 * January to September 2026, their payments (cheques, transfers and PDCs; full up to May, partial
 * in June and July, open afterwards for ageing), a voided cheque, PDCs presented, due, replaced and
 * cancelled, and petty cash activity. Idempotent: nothing is created when the first demo invoice
 * exists.
 */
@Component
public class PayablesDemoData {

  private static final String COMPANY = "FVI";
  private static final int YEAR = 2026;
  private static final int INVOICES = 60;
  private static final int PER_MONTH = 7;
  private static final int DAY_SPREAD = 26;
  private static final long AMOUNT_BASE = 4_000;
  private static final long AMOUNT_STEP = 3_137;
  private static final long AMOUNT_SPREAD = 46_000;
  private static final int SECOND_LINE_EVERY = 4;
  private static final int SECOND_LINE_SHARE = 5;
  private static final int FULLY_PAID_UNTIL = 5;
  private static final int PARTIALLY_PAID_UNTIL = 7;
  private static final int PAYMENT_DAY = 10;
  private static final int PDC_DAY = 25;
  private static final int PARTIAL_DAY = 12;
  private static final int PRESENTED_AFTER = 3;
  private static final String DEPARTMENT = "FND";
  private static final String BDO_CA = "BDO-CA";
  private static final String BPI_SA = "BPI-SA";
  private static final List<String> SUPPLIERS = List.of("S-0001", "S-0002", "S-0003");
  private static final List<String> ACCOUNTS = List.of("5609", "5610", "5603");
  private static final List<String> SECOND_ACCOUNTS = List.of("5604", "5604", "5607");
  private static final List<String> COST_CENTERS = List.of("FIN", "IT", "EXEC");
  private static final List<String> DESCRIPTIONS =
      List.of(
          "Printing and promotional materials",
          "Cloud subscription and IT services",
          "Office space rental");
  private static final List<String> BRANCHES = List.of("HO", "CEB", "DVO");
  private static final BigDecimal HALF = new BigDecimal("0.5");
  private static final long CENT_STEP = 25;
  private static final LocalDate DEMO_TODAY = LocalDate.of(YEAR, 9, 23);
  private static final LocalDate PDC_ISSUED = LocalDate.of(YEAR, 9, 5);
  private static final List<LocalDate> PDC_CHEQUE_DATES =
      List.of(LocalDate.of(YEAR, 9, 15), LocalDate.of(YEAR, 9, 20), LocalDate.of(YEAR, 9, 28));
  private static final LocalDate PDC_CANCELLED = LocalDate.of(YEAR, 9, 18);
  private static final LocalDate PDC_REPLACED = LocalDate.of(YEAR, 9, 10);
  private static final LocalDate PDC_REPLACEMENT_DATE = LocalDate.of(YEAR, 9, 30);

  private final OrganizationService organization;
  private final SupplierInvoiceService invoiceService;
  private final SupplierInvoiceRepository invoices;
  private final PaymentVoucherService vouchers;
  private final PaymentApprovalService approvals;
  private final IssuedPdcService pdcs;
  private final BankAccountQueryService banks;
  private final OpenItemService openItems;
  private final PettyCashDemoData pettyCash;
  private final DemoActor actor;
  private final TransactionTemplate tx;

  /**
   * Creates the generator.
   *
   * @param organization organization service
   * @param invoiceService invoice service
   * @param invoices invoice repository
   * @param vouchers voucher service
   * @param approvals payment approvals
   * @param pdcs PDC register
   * @param banks bank accounts
   * @param openItems sub-ledger
   * @param pettyCash petty cash demo data
   * @param actor demo users
   * @param transactions transaction manager
   */
  public PayablesDemoData(
      OrganizationService organization,
      SupplierInvoiceService invoiceService,
      SupplierInvoiceRepository invoices,
      PaymentVoucherService vouchers,
      PaymentApprovalService approvals,
      IssuedPdcService pdcs,
      BankAccountQueryService banks,
      OpenItemService openItems,
      PettyCashDemoData pettyCash,
      DemoActor actor,
      PlatformTransactionManager transactions) {
    this.organization = organization;
    this.invoiceService = invoiceService;
    this.invoices = invoices;
    this.vouchers = vouchers;
    this.approvals = approvals;
    this.pdcs = pdcs;
    this.banks = banks;
    this.openItems = openItems;
    this.pettyCash = pettyCash;
    this.actor = actor;
    this.tx = new TransactionTemplate(transactions);
  }

  /**
   * Loads the demo data unless already present.
   *
   * @return true when data was created
   */
  public boolean loadIfMissing() {
    Optional<Company> company =
        organization.listCompanies().stream().filter(c -> COMPANY.equals(c.getCode())).findFirst();
    if (company.isEmpty() || alreadyLoaded(company.get().getId())) {
      return false;
    }
    Long companyId = company.get().getId();
    tx.executeWithoutResult(status -> load(companyId));
    return true;
  }

  private boolean alreadyLoaded(Long companyId) {
    return invoices.existsByCompanyIdAndSupplierInvoiceNo(companyId, supplierInvoiceNo(0));
  }

  private static String supplierInvoiceNo(int i) {
    return SUPPLIERS.get(i % SUPPLIERS.size()) + "/" + YEAR + "-" + String.format("%03d", i + 1);
  }

  private void load(Long companyId) {
    Map<String, Long> branches =
        organization.listBranches(companyId).stream()
            .collect(Collectors.toMap(Branch::getCode, Branch::getId));
    List<SupplierInvoice> approved = new ArrayList<>();
    for (int i = 0; i < INVOICES; i++) {
      approved.add(invoice(companyId, branches, i));
    }
    Payments payments = new Payments(companyId, branches.get(BRANCHES.get(0)));
    for (int s = 0; s < SUPPLIERS.size(); s++) {
      for (int month = 1; month <= PARTIALLY_PAID_UNTIL + 1; month++) {
        payments.settle(s, month, itemsOf(approved, s, month));
      }
    }
    pdcs.refreshDue(DEMO_TODAY);
    pettyCash.load(companyId);
  }

  private SupplierInvoice invoice(Long companyId, Map<String, Long> branches, int i) {
    int s = i % SUPPLIERS.size();
    LocalDate date = LocalDate.of(YEAR, 1 + i / PER_MONTH, 2 + (i * PER_MONTH) % DAY_SPREAD);
    Long branchId =
        branches.get(
            s == 2 ? BRANCHES.get((i / SUPPLIERS.size()) % BRANCHES.size()) : BRANCHES.get(0));
    BigDecimal net =
        BigDecimal.valueOf(AMOUNT_BASE + (i * AMOUNT_STEP) % AMOUNT_SPREAD)
            .add(BigDecimal.valueOf(i % SECOND_LINE_EVERY * CENT_STEP, 2));
    List<InvoiceLineValues> lines = new ArrayList<>();
    lines.add(
        new InvoiceLineValues(ACCOUNTS.get(s), COST_CENTERS.get(s), DESCRIPTIONS.get(s), net));
    if (i % SECOND_LINE_EVERY == 0) {
      lines.add(
          new InvoiceLineValues(
              SECOND_ACCOUNTS.get(s),
              COST_CENTERS.get(s),
              "Related charges",
              Money.round(net.divide(BigDecimal.valueOf(SECOND_LINE_SHARE)))));
    }
    InvoiceCommand command =
        new InvoiceCommand(
            companyId,
            branchId,
            SUPPLIERS.get(s),
            supplierInvoiceNo(i),
            date,
            null,
            null,
            true,
            DESCRIPTIONS.get(s) + " " + date.getMonth(),
            lines);
    SupplierInvoice draft =
        actor.as(
            DemoActor.MAKER, () -> invoiceService.submit(invoiceService.create(command).getId()));
    return actor.as(DemoActor.CHECKER, () -> invoiceService.approve(draft.getId()));
  }

  private static List<Long> itemsOf(List<SupplierInvoice> approved, int s, int month) {
    return approved.stream()
        .filter(i -> i.getPartyCode().equals(SUPPLIERS.get(s)))
        .filter(i -> i.getInvoiceDate().getMonthValue() == month)
        .map(SupplierInvoice::getOpenItemId)
        .toList();
  }

  /** Payment plan of the demo suppliers. */
  private final class Payments {

    private final Long companyId;
    private final Long branchId;

    Payments(Long companyId, Long branchId) {
      this.companyId = companyId;
      this.branchId = branchId;
    }

    void settle(int s, int month, List<Long> items) {
      if (items.isEmpty()) {
        return;
      }
      if (month <= FULLY_PAID_UNTIL) {
        full(s, month, items);
      } else if (month <= PARTIALLY_PAID_UNTIL) {
        partial(s, month, items);
      } else {
        postDated(s, items);
      }
    }

    private void full(int s, int month, List<Long> items) {
      LocalDate date = LocalDate.of(YEAR, month + 1, PAYMENT_DAY);
      if (s == 0) {
        PaymentVoucher v = pay(s, items, null, PaymentMode.CHEQUE, BDO_CA, date, null);
        actor.as(
            DemoActor.CHECKER,
            () -> approvals.markPresented(v.getId(), date.plusDays(PRESENTED_AFTER)));
      } else if (s == 1) {
        pay(s, items, null, PaymentMode.BANK_TRANSFER, BPI_SA, date, null);
      } else {
        LocalDate chequeDate = LocalDate.of(YEAR, month + 1, PDC_DAY);
        PaymentVoucher v = pay(s, items, null, PaymentMode.PDC, BDO_CA, date, chequeDate);
        actor.as(
            DemoActor.CHECKER, () -> pdcs.present(pdcOf(v), chequeDate.plusDays(PRESENTED_AFTER)));
      }
    }

    private void partial(int s, int month, List<Long> items) {
      LocalDate date = LocalDate.of(YEAR, month + 1, PARTIAL_DAY);
      PaymentMode mode = s == 1 ? PaymentMode.BANK_TRANSFER : PaymentMode.CHEQUE;
      PaymentVoucher v = pay(s, items, HALF, mode, s == 1 ? BPI_SA : BDO_CA, date, null);
      if (s == 0 && month == PARTIALLY_PAID_UNTIL) {
        actor.as(
            DemoActor.CHECKER,
            () ->
                approvals.voidCheque(
                    v.getId(), date.plusDays(PAYMENT_DAY), "Cheque lost in transit"));
      }
    }

    private void postDated(int s, List<Long> items) {
      LocalDate issued = PDC_ISSUED.plusDays(s);
      LocalDate chequeDate = PDC_CHEQUE_DATES.get(s);
      PaymentVoucher v = pay(s, items, null, PaymentMode.PDC, BDO_CA, issued, chequeDate);
      Long pdcId = pdcOf(v);
      if (s == 1) {
        actor.as(
            DemoActor.CHECKER,
            () -> pdcs.cancel(pdcId, PDC_CANCELLED, "Supplier requested bank transfer"));
      } else if (s == 2) {
        actor.as(
            DemoActor.CHECKER,
            () -> pdcs.replace(pdcId, PDC_REPLACEMENT_DATE, PDC_REPLACED, "Cheque damaged"));
      }
    }

    private PaymentVoucher pay(
        int s,
        List<Long> items,
        BigDecimal share,
        PaymentMode mode,
        String bank,
        LocalDate date,
        LocalDate chequeDate) {
      List<PaymentCommand.ItemPayment> payments =
          items.stream()
              .map(
                  id ->
                      new PaymentCommand.ItemPayment(
                          id,
                          share == null
                              ? null
                              : Money.round(openItems.get(id).outstanding().multiply(share))))
              .toList();
      PaymentCommand command =
          new PaymentCommand(
              companyId,
              branchId,
              SUPPLIERS.get(s),
              null,
              null,
              mode,
              banks.getByCode(companyId, bank).getId(),
              date,
              chequeDate,
              DEPARTMENT,
              "Settlement of " + DESCRIPTIONS.get(s).toLowerCase(Locale.ROOT),
              payments);
      PaymentVoucher draft =
          actor.as(DemoActor.MAKER, () -> vouchers.submit(vouchers.create(command).getId()));
      return actor.as(DemoActor.CHECKER, () -> approvals.approve(draft.getId()));
    }

    private Long pdcOf(PaymentVoucher v) {
      return pdcs
          .search(
              companyId,
              EnumSet.of(PdcStatus.ISSUED, PdcStatus.DUE),
              v.getChequeDate(),
              v.getChequeDate())
          .stream()
          .filter(p -> p.getVoucherId().equals(v.getId()))
          .findFirst()
          .orElseThrow()
          .getId();
    }
  }
}
