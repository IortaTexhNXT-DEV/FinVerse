package com.iortatechnxt.brokerverse.payables;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.approval.service.ApprovalInboxService;
import com.iortatechnxt.brokerverse.approval.service.PendingApproval;
import com.iortatechnxt.brokerverse.payables.domain.BankAccount;
import com.iortatechnxt.brokerverse.payables.domain.InvoiceLineValues;
import com.iortatechnxt.brokerverse.payables.domain.PaymentMode;
import com.iortatechnxt.brokerverse.payables.domain.PaymentVoucher;
import com.iortatechnxt.brokerverse.payables.domain.PettyCashDisbursement;
import com.iortatechnxt.brokerverse.payables.domain.PettyCashDisbursementValues;
import com.iortatechnxt.brokerverse.payables.domain.PettyCashFund;
import com.iortatechnxt.brokerverse.payables.domain.PettyCashReimbursement;
import com.iortatechnxt.brokerverse.payables.domain.SupplierInvoice;
import com.iortatechnxt.brokerverse.payables.service.BankAccountCommand;
import com.iortatechnxt.brokerverse.payables.service.BankAccountQueryService;
import com.iortatechnxt.brokerverse.payables.service.BankAccountService;
import com.iortatechnxt.brokerverse.payables.service.FundCommand;
import com.iortatechnxt.brokerverse.payables.service.PaymentVoucherService;
import com.iortatechnxt.brokerverse.payables.service.PettyCashFundService;
import com.iortatechnxt.brokerverse.payables.service.PettyCashService;
import com.iortatechnxt.brokerverse.payables.service.SupplierInvoiceService;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.support.TestData;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/** Payables documents and masters in the universal approval inbox; every test rolls back. */
@IntegrationTest
@Transactional
class PayablesApprovalSourceIT {

  private static final LocalDate DAY = LocalDate.of(2026, 9, 3);

  @Autowired private PayablesFixtures fx;
  @Autowired private SupplierInvoiceService invoices;
  @Autowired private PaymentVoucherService vouchers;
  @Autowired private PettyCashFundService funds;
  @Autowired private PettyCashService pettyCash;
  @Autowired private BankAccountService bankService;
  @Autowired private BankAccountQueryService banks;
  @Autowired private ApprovalInboxService inbox;
  @Autowired private EntityManager entityManager;
  @Autowired private AsUser as;
  @Autowired private TestData data;

  private Map<String, PendingApproval> inboxOf(String user) {
    return as.run(user, () -> inbox.inbox(fx.companyId())).stream()
        .collect(Collectors.toMap(PendingApproval::reference, Function.identity(), (a, b) -> a));
  }

  private SupplierInvoice submittedInvoice(String net) {
    var cmd =
        fx.invoiceCommand(
            "G-0001",
            PayablesFixtures.DATE,
            List.of(new InvoiceLineValues("5608", "FIN", "Supplies", new BigDecimal(net))));
    return as.run("accountant", () -> invoices.submit(invoices.create(cmd).getId()));
  }

  private PettyCashFund fund(boolean established) {
    FundCommand cmd =
        new FundCommand(
            fx.companyId(),
            data.branch("CEB").getId(),
            PayablesFixtures.unique("PCF"),
            "Inbox test box",
            "Test custodian",
            "1102",
            fx.bankId("BDO-CA"),
            new BigDecimal("1000.00"));
    PettyCashFund fund = as.run("accountant", () -> funds.create(cmd));
    if (!established) {
      return fund;
    }
    as.run("checker", () -> funds.authorize(fund.getId()));
    return as.run("checker", () -> funds.establish(fund.getId(), DAY));
  }

  private static PettyCashDisbursementValues voucher(String amount) {
    return new PettyCashDisbursementValues(
        DAY, "Messenger", "5606", "FIN", "Taxi fare", "OR-1", new BigDecimal(amount));
  }

  @Test
  void pendingPayablesDocumentsReachTheCheckerButNotTheMaker() {
    SupplierInvoice invoice = submittedInvoice("1000.00");
    SupplierInvoice paid = fx.approvedInvoice("G-0001", "2000.00", PayablesFixtures.DATE);
    PaymentVoucher payment =
        as.run(
            "accountant",
            () ->
                vouchers.submit(
                    vouchers
                        .create(
                            fx.paymentCommand(
                                "G-0001",
                                PaymentMode.CHEQUE,
                                "BDO-CA",
                                PayablesFixtures.DATE,
                                null,
                                Map.of(paid.getOpenItemId(), new BigDecimal("500.00"))))
                        .getId()));
    PettyCashFund newFund = fund(false);
    PettyCashFund box = fund(true);
    PettyCashDisbursement spent =
        as.run("accountant", () -> pettyCash.disburse(box.getId(), voucher("300.00")));
    as.run("checker", () -> pettyCash.approveDisbursement(spent.getId()));
    PettyCashReimbursement claim =
        as.run(
            "accountant",
            () -> pettyCash.claimReimbursement(box.getId(), DAY, List.of(), "Refill"));
    PettyCashDisbursement pendingVoucher =
        as.run("accountant", () -> pettyCash.disburse(box.getId(), voucher("200.00")));
    BankAccount bank = banks.getByCode(fx.companyId(), "BPI-SA");
    as.run(
        "accountant",
        () -> {
          BankAccount changed = bankService.update(bank.getId(), sameValues(bank));
          entityManager.flush(); // records the accountant as the last modifier (maker)
          return changed;
        });

    Map<String, PendingApproval> checker = inboxOf("checker");
    PendingApproval invoiceItem = checker.get(invoice.getDocumentNo());
    assertThat(invoiceItem.module()).isEqualTo("PAYABLES");
    assertThat(invoiceItem.link()).isEqualTo("/payables/invoices");
    assertThat(invoiceItem.amount()).isEqualByComparingTo(invoice.getPayableAmount());
    assertThat(invoiceItem.submittedBy()).isEqualTo("accountant");
    assertThat(checker.get(payment.getVoucherNo()).link()).isEqualTo("/payables/vouchers");
    assertThat(checker.get(pendingVoucher.getDocumentNo()).currency()).isEqualTo("PHP");
    assertThat(checker.get(claim.getDocumentNo()).amount()).isEqualByComparingTo("300.00");
    assertThat(checker.get(newFund.getCode()).link()).isEqualTo("/payables/petty-cash");
    assertThat(checker.get("BPI-SA").link()).isEqualTo("/payables/bank-accounts");

    assertThat(inboxOf("accountant"))
        .doesNotContainKeys(
            invoice.getDocumentNo(),
            payment.getVoucherNo(),
            pendingVoucher.getDocumentNo(),
            claim.getDocumentNo(),
            newFund.getCode(),
            "BPI-SA");
  }

  @Test
  void documentsAboveTheCheckersLimitGoToAHigherAuthority() {
    SupplierInvoice big = submittedInvoice("6000000.00");
    assertThat(inboxOf("checker")).doesNotContainKey(big.getDocumentNo());
    assertThat(inboxOf("fmanager")).containsKey(big.getDocumentNo());
    assertThat(inbox.pendingAll())
        .extracting(PendingApproval::reference)
        .contains(big.getDocumentNo());
  }

  private static BankAccountCommand sameValues(BankAccount b) {
    return new BankAccountCommand(
        b.getCompanyId(),
        b.getCode(),
        b.getName(),
        b.getBankPartyCode(),
        b.getBankName(),
        b.getAccountNo(),
        b.getCurrency(),
        b.getGlAccountCode(),
        b.getPdcClearingAccountCode(),
        b.getBranchId(),
        b.getNotificationFormat());
  }
}
