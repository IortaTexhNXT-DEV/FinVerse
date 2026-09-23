package com.iortatechnxt.finverse.payables;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.finverse.common.exception.BusinessRuleException;
import com.iortatechnxt.finverse.common.exception.DuplicateResourceException;
import com.iortatechnxt.finverse.payables.domain.InvoiceLineValues;
import com.iortatechnxt.finverse.payables.domain.InvoiceStatus;
import com.iortatechnxt.finverse.payables.domain.SupplierInvoice;
import com.iortatechnxt.finverse.payables.service.InvoiceCommand;
import com.iortatechnxt.finverse.payables.service.SupplierInvoiceService;
import com.iortatechnxt.finverse.subledger.domain.ItemDirection;
import com.iortatechnxt.finverse.subledger.domain.OpenItem;
import com.iortatechnxt.finverse.subledger.service.OpenItemService;
import com.iortatechnxt.finverse.support.AsUser;
import com.iortatechnxt.finverse.support.IntegrationTest;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

@IntegrationTest
class SupplierInvoiceIT {

  @Autowired private SupplierInvoiceService invoices;
  @Autowired private OpenItemService openItems;
  @Autowired private PayablesFixtures fx;
  @Autowired private AsUser as;

  @Test
  void approvalPostsOneJournalPerExpenseGroupAndRecordsTheCreditItem() {
    // G-0001 (garage): EWT 2 %, VAT registered.
    InvoiceCommand cmd =
        fx.invoiceCommand(
            "G-0001",
            PayablesFixtures.DATE,
            List.of(
                new InvoiceLineValues("5607", "CLM", "Aircon repair", new BigDecimal("10000.00")),
                new InvoiceLineValues("5607", "CLM", "Parts", new BigDecimal("2500.00")),
                new InvoiceLineValues("5613", "FIN", "Towing", new BigDecimal("1000.00"))));
    SupplierInvoice draft = as.run("accountant", () -> invoices.create(cmd));
    assertThat(draft.getStatus()).isEqualTo(InvoiceStatus.DRAFT);
    assertThat(draft.getNetAmount()).isEqualByComparingTo("13500.00");
    assertThat(draft.getVatAmount()).isEqualByComparingTo("1620.00");
    assertThat(draft.getWhtAmount()).isEqualByComparingTo("270.00");
    assertThat(draft.getPayableAmount()).isEqualByComparingTo("14850.00");
    assertThat(draft.getDueDate()).isEqualTo(PayablesFixtures.DATE.plusDays(30));

    as.run("accountant", () -> invoices.submit(draft.getId()));
    assertThatThrownBy(() -> as.run("accountant", () -> invoices.approve(draft.getId())))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("prepared");

    SupplierInvoice approved = as.run("checker", () -> invoices.approve(draft.getId()));
    assertThat(approved.getStatus()).isEqualTo(InvoiceStatus.APPROVED);
    assertThat(approved.getApprovedBy()).isEqualTo("checker");
    String[] batches = approved.getJournalBatchNos().split(",");
    assertThat(batches).hasSize(2);
    assertThat(fx.posted(batches[0], "5607")).isEqualByComparingTo("12500.00");
    assertThat(fx.posted(batches[0], "1603")).isEqualByComparingTo("1500.00");
    assertThat(fx.posted(batches[0], "2508")).isEqualByComparingTo("-250.00");
    assertThat(fx.posted(batches[0], "2501")).isEqualByComparingTo("-13750.00");
    assertThat(fx.posted(batches[1], "5613")).isEqualByComparingTo("1000.00");
    assertThat(fx.posted(batches[1], "2501")).isEqualByComparingTo("-1100.00");

    OpenItem item = openItems.get(approved.getOpenItemId());
    assertThat(item.getDirection()).isEqualTo(ItemDirection.CREDIT);
    assertThat(item.getAmount()).isEqualByComparingTo("14850.00");
    assertThat(item.getDocumentType()).isEqualTo("SUPPLIER_INVOICE");
    assertThat(item.getDueDate()).isEqualTo(approved.getDueDate());

    assertThat(
            invoices
                .search(
                    fx.companyId(),
                    InvoiceStatus.APPROVED,
                    "G-0001",
                    PayablesFixtures.DATE,
                    PayablesFixtures.DATE,
                    PageRequest.of(0, 50))
                .getContent())
        .extracting(SupplierInvoice::getId)
        .contains(approved.getId());
  }

  @Test
  void duplicateSupplierNumberAndMissingCostCentreAreRejected() {
    InvoiceCommand cmd =
        fx.invoiceCommand(
            "G-0002",
            PayablesFixtures.DATE,
            List.of(new InvoiceLineValues("5607", "CLM", "Repair", new BigDecimal("500.00"))));
    as.run("accountant", () -> invoices.create(cmd));
    assertThatThrownBy(() -> as.run("accountant", () -> invoices.create(cmd)))
        .isInstanceOf(DuplicateResourceException.class);

    InvoiceCommand noCostCentre =
        fx.invoiceCommand(
            "G-0002",
            PayablesFixtures.DATE,
            List.of(new InvoiceLineValues("5607", null, "Repair", new BigDecimal("500.00"))));
    assertThatThrownBy(() -> as.run("accountant", () -> invoices.create(noCostCentre)))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("Cost centre");

    InvoiceCommand client =
        fx.invoiceCommand(
            "C-000101",
            PayablesFixtures.DATE,
            List.of(new InvoiceLineValues("5613", "FIN", "x", new BigDecimal("1.00"))));
    assertThatThrownBy(() -> as.run("accountant", () -> invoices.create(client)))
        .isInstanceOf(BusinessRuleException.class);
  }

  @Test
  void rejectReturnsToDraftForEditingAndCancelClosesIt() {
    InvoiceCommand cmd =
        fx.invoiceCommand(
            "S-0002",
            PayablesFixtures.DATE,
            List.of(new InvoiceLineValues("5610", "IT", "Licences", new BigDecimal("800.00"))));
    SupplierInvoice created =
        as.run("accountant", () -> invoices.submit(invoices.create(cmd).getId()));
    SupplierInvoice rejected =
        as.run("checker", () -> invoices.reject(created.getId(), "Wrong amount"));
    assertThat(rejected.getStatus()).isEqualTo(InvoiceStatus.DRAFT);
    assertThat(rejected.getStatusReason()).isEqualTo("Wrong amount");

    InvoiceCommand corrected =
        new InvoiceCommand(
            cmd.companyId(),
            cmd.branchId(),
            cmd.partyCode(),
            cmd.supplierInvoiceNo(),
            cmd.invoiceDate(),
            cmd.invoiceDate().plusDays(10),
            "PHP",
            false,
            "Corrected",
            List.of(new InvoiceLineValues("5610", "IT", "Licences", new BigDecimal("900.00"))));
    SupplierInvoice updated =
        as.run("accountant", () -> invoices.update(created.getId(), corrected));
    assertThat(updated.getPayableAmount()).isEqualByComparingTo("882.00");
    assertThat(invoices.get(created.getId()).getLines()).hasSize(1);

    SupplierInvoice cancelled =
        as.run("accountant", () -> invoices.cancel(created.getId(), "Duplicate billing"));
    assertThat(cancelled.getStatus()).isEqualTo(InvoiceStatus.CANCELLED);
    assertThatThrownBy(() -> as.run("accountant", () -> invoices.submit(created.getId())))
        .isInstanceOf(BusinessRuleException.class);
  }
}
