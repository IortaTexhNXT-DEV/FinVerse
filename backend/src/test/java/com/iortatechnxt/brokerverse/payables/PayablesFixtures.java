package com.iortatechnxt.brokerverse.payables;

import com.iortatechnxt.brokerverse.payables.domain.InvoiceLineValues;
import com.iortatechnxt.brokerverse.payables.domain.PaymentMode;
import com.iortatechnxt.brokerverse.payables.domain.PaymentVoucher;
import com.iortatechnxt.brokerverse.payables.domain.SupplierInvoice;
import com.iortatechnxt.brokerverse.payables.service.BankAccountQueryService;
import com.iortatechnxt.brokerverse.payables.service.InvoiceCommand;
import com.iortatechnxt.brokerverse.payables.service.PaymentApprovalService;
import com.iortatechnxt.brokerverse.payables.service.PaymentCommand;
import com.iortatechnxt.brokerverse.payables.service.PaymentVoucherService;
import com.iortatechnxt.brokerverse.payables.service.SupplierInvoiceService;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.TestData;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Builds approved supplier invoices and payments for payables tests. */
@Component
public class PayablesFixtures {

  public static final LocalDate DATE = LocalDate.of(2026, 9, 1);

  private final SupplierInvoiceService invoices;
  private final PaymentVoucherService vouchers;
  private final PaymentApprovalService approvals;
  private final BankAccountQueryService banks;
  private final AsUser as;
  private final TestData data;
  private final JdbcTemplate jdbc;

  PayablesFixtures(
      SupplierInvoiceService invoices,
      PaymentVoucherService vouchers,
      PaymentApprovalService approvals,
      BankAccountQueryService banks,
      AsUser as,
      TestData data,
      JdbcTemplate jdbc) {
    this.invoices = invoices;
    this.vouchers = vouchers;
    this.approvals = approvals;
    this.banks = banks;
    this.as = as;
    this.data = data;
    this.jdbc = jdbc;
  }

  public Long companyId() {
    return data.company().getId();
  }

  public Long bankId(String code) {
    return banks.getByCode(companyId(), code).getId();
  }

  public static String unique(String prefix) {
    return prefix + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
  }

  public InvoiceCommand invoiceCommand(
      String party, LocalDate date, List<InvoiceLineValues> lines) {
    return new InvoiceCommand(
        companyId(),
        data.branch("HO").getId(),
        party,
        unique("INV"),
        date,
        null,
        null,
        true,
        "Test invoice",
        lines);
  }

  public SupplierInvoice approvedInvoice(String party, String net, LocalDate date) {
    InvoiceCommand cmd =
        invoiceCommand(
            party,
            date,
            List.of(new InvoiceLineValues("5608", "FIN", "Supplies", new BigDecimal(net))));
    SupplierInvoice draft =
        as.run("accountant", () -> invoices.submit(invoices.create(cmd).getId()));
    return as.run("checker", () -> invoices.approve(draft.getId()));
  }

  public PaymentCommand paymentCommand(
      String party,
      PaymentMode mode,
      String bank,
      LocalDate date,
      LocalDate chequeDate,
      Map<Long, BigDecimal> items) {
    return new PaymentCommand(
        companyId(),
        data.branch("HO").getId(),
        party,
        null,
        null,
        mode,
        bankId(bank),
        date,
        chequeDate,
        "FND",
        "Test payment",
        items.entrySet().stream()
            .map(e -> new PaymentCommand.ItemPayment(e.getKey(), e.getValue()))
            .toList());
  }

  public PaymentVoucher approvedPayment(PaymentCommand cmd) {
    PaymentVoucher draft =
        as.run("accountant", () -> vouchers.submit(vouchers.create(cmd).getId()));
    return as.run("checker", () -> approvals.approve(draft.getId()));
  }

  /** Signed (debit - credit) base amount posted to an account by a journal batch. */
  public BigDecimal posted(String batchNo, String account) {
    BigDecimal value =
        jdbc.queryForObject(
            """
            select coalesce(sum(e.debit_base - e.credit_base), 0) from gl_ledger_entry e
            join coa_account a on a.id = e.account_id
            where e.batch_no = ? and a.code = ?
            """,
            BigDecimal.class,
            batchNo,
            account);
    return value.setScale(2, RoundingMode.HALF_EVEN);
  }
}
