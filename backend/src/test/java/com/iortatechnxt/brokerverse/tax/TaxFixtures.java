package com.iortatechnxt.brokerverse.tax;

import com.iortatechnxt.brokerverse.payables.PayablesFixtures;
import com.iortatechnxt.brokerverse.payables.domain.InvoiceLineValues;
import com.iortatechnxt.brokerverse.payables.domain.SupplierInvoice;
import com.iortatechnxt.brokerverse.payables.service.InvoiceCommand;
import com.iortatechnxt.brokerverse.payables.service.SupplierInvoiceService;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.TestData;
import com.iortatechnxt.brokerverse.tax.seed.TaxSeedMasters;
import com.iortatechnxt.brokerverse.underwriting.UwFixtures;
import com.iortatechnxt.brokerverse.underwriting.api.dto.PolicyRequest;
import com.iortatechnxt.brokerverse.underwriting.domain.BusinessType;
import com.iortatechnxt.brokerverse.underwriting.domain.Policy;
import com.iortatechnxt.brokerverse.underwriting.domain.Product;
import com.iortatechnxt.brokerverse.underwriting.domain.SourceType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Builds tax scenarios in the seed company FVI through the owning modules' services: a fire policy
 * through a broker (VAT, DST, LGT, FST and commission withholding) and a VAT-registered supplier
 * invoice with 2 % EWT.
 */
@Component
public class TaxFixtures {

  private final TaxSeedMasters masters;
  private final UwFixtures uw;
  private final SupplierInvoiceService invoices;
  private final AsUser as;
  private final TestData data;
  private final JdbcTemplate jdbc;

  TaxFixtures(
      TaxSeedMasters masters,
      UwFixtures uw,
      SupplierInvoiceService invoices,
      AsUser as,
      TestData data,
      JdbcTemplate jdbc) {
    this.masters = masters;
    this.uw = uw;
    this.invoices = invoices;
    this.as = as;
    this.data = data;
    this.jdbc = jdbc;
  }

  public Long companyId() {
    return data.company().getId();
  }

  /** Ensures the tax masters of FVI (idempotent). */
  public void masters() {
    masters.ensure(companyId());
  }

  /** Fire policy through broker B-0001, issued and approved on a date. */
  public Policy firePolicy(LocalDate date, String premium) {
    Product fire = uw.product("FIRE", false);
    PolicyRequest request =
        new PolicyRequest(
            companyId(),
            uw.branchId(),
            fire.getId(),
            "C-000201",
            "Luzon Steel Manufacturing Corp.",
            SourceType.BROKER,
            "B-0001",
            date,
            date,
            date.plusYears(1).minusDays(1),
            "PHP",
            BusinessType.DIRECT,
            new BigDecimal("100"),
            null,
            false,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            null,
            List.of(uw.risk("20000000", premium, "NCR-TAX")));
    return uw.issue(request, date);
  }

  /** Approved VAT-registered invoice of S-0002 (2 % EWT) booked to IT expenses. */
  public SupplierInvoice supplierInvoice(LocalDate date, String net) {
    InvoiceCommand cmd =
        new InvoiceCommand(
            companyId(),
            data.branch("HO").getId(),
            "S-0002",
            PayablesFixtures.unique("TAX"),
            date,
            null,
            null,
            true,
            "Tax test invoice",
            List.of(new InvoiceLineValues("5610", "IT", "Cloud services", new BigDecimal(net))));
    SupplierInvoice draft =
        as.run("accountant", () -> invoices.submit(invoices.create(cmd).getId()));
    return as.run("checker", () -> invoices.approve(draft.getId()));
  }

  /** Net (credit − debit) base movement of an account in a period, every journal included. */
  public BigDecimal creditMovement(String account, LocalDate from, LocalDate to) {
    BigDecimal value =
        jdbc.queryForObject(
            """
            select coalesce(sum(e.credit_base - e.debit_base), 0) from gl_ledger_entry e
            join coa_account a on a.id = e.account_id
            where e.company_id = ? and a.code = ? and e.value_date between ? and ?
            """,
            BigDecimal.class,
            companyId(),
            account,
            from,
            to);
    return value.setScale(2, RoundingMode.HALF_EVEN);
  }

  /** Signed (debit − credit) base amount posted to an account by a journal batch. */
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
