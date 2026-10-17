package com.iortatechnxt.brokerverse.payables.service;

import com.iortatechnxt.brokerverse.accounting.service.AccountingEventPublisher;
import com.iortatechnxt.brokerverse.accounting.service.BusinessEvent;
import com.iortatechnxt.brokerverse.payables.domain.InvoicePosting;
import com.iortatechnxt.brokerverse.payables.domain.PaymentCategory;
import com.iortatechnxt.brokerverse.payables.domain.SupplierInvoice;
import com.iortatechnxt.brokerverse.payables.domain.SupplierInvoiceLine;
import com.iortatechnxt.brokerverse.subledger.domain.ItemDirection;
import com.iortatechnxt.brokerverse.subledger.domain.OpenItem;
import com.iortatechnxt.brokerverse.subledger.domain.OpenItemValues;
import com.iortatechnxt.brokerverse.subledger.service.OpenItemService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.springframework.stereotype.Component;

/**
 * Accounts for an approved supplier invoice.
 *
 * <p>Design: the {@code SUPPLIER_INVOICE} event carries one {@code @EXPENSE} account and one cost
 * centre, so the invoice lines are grouped by (expense account, cost centre) and one event is
 * published per group, with the group's NET_AMOUNT, INPUT_VAT, WITHHOLDING_TAX and PAYABLE. The
 * standard rule posts Dr expense (net) + Dr input VAT / Cr supplier payable (party line) + Cr EWT
 * payable. A single CREDIT open item for the whole payable amount is then recorded against the
 * supplier, so the sub-ledger shows one document per invoice while each GL journal stays balanced.
 * Source references {@code SUPINV:<id>:<group>} keep the events idempotent.
 */
@Component
public class SupplierInvoicePoster {

  /** Event type published per expense group. */
  public static final String EVENT = "SUPPLIER_INVOICE";

  private static final String REF = "SUPINV:";
  private static final int NARRATION_MAX = 250;

  private final AccountingEventPublisher publisher;
  private final OpenItemService openItems;
  private final PayablesSupport support;

  /**
   * Creates the poster.
   *
   * @param publisher accounting engine
   * @param openItems sub-ledger
   * @param support shared helpers
   */
  public SupplierInvoicePoster(
      AccountingEventPublisher publisher, OpenItemService openItems, PayablesSupport support) {
    this.publisher = publisher;
    this.openItems = openItems;
    this.support = support;
  }

  /**
   * Publishes the events and records the open item.
   *
   * @param inv invoice being approved
   * @return posting results
   */
  public InvoicePosting post(SupplierInvoice inv) {
    Map<GroupKey, List<SupplierInvoiceLine>> groups = new LinkedHashMap<>();
    for (SupplierInvoiceLine line : inv.getLines()) {
      groups
          .computeIfAbsent(
              new GroupKey(line.getExpenseAccountCode(), line.getCostCenter()),
              k -> new ArrayList<>())
          .add(line);
    }
    List<String> batchNos = new ArrayList<>();
    int group = 1;
    for (Map.Entry<GroupKey, List<SupplierInvoiceLine>> e : groups.entrySet()) {
      batchNos.add(publisher.publish(event(inv, e.getKey(), e.getValue(), group++)).getBatchNo());
    }
    BigDecimal base =
        support.toBase(
            inv.getCompanyId(), inv.getCurrency(), inv.getPayableAmount(), inv.getInvoiceDate());
    OpenItem item =
        openItems.record(
            new OpenItemValues(
                inv.getCompanyId(),
                inv.getBranchId(),
                inv.getPartyId(),
                inv.getPartyCode(),
                ItemDirection.CREDIT,
                PaymentCategory.SUPPLIER_INVOICE_DOCUMENT,
                inv.getDocumentNo(),
                inv.getInvoiceDate(),
                inv.getDueDate(),
                inv.getCurrency(),
                inv.getPayableAmount(),
                base,
                PayablesSupport.MODULE,
                REF + inv.getId(),
                batchNos.get(0),
                narration(inv)));
    return new InvoicePosting(item.getId(), batchNos, base);
  }

  private static BusinessEvent event(
      SupplierInvoice inv, GroupKey key, List<SupplierInvoiceLine> lines, int group) {
    BigDecimal net = sum(lines, SupplierInvoiceLine::getNetAmount);
    BigDecimal vat = sum(lines, SupplierInvoiceLine::getVatAmount);
    BigDecimal wht = sum(lines, SupplierInvoiceLine::getWhtAmount);
    return new BusinessEvent(
        EVENT,
        inv.getCompanyId(),
        inv.getBranchId(),
        inv.getInvoiceDate(),
        inv.getCurrency(),
        PayablesSupport.MODULE,
        REF + inv.getId() + ":" + group,
        inv.getDocumentNo(),
        inv.getPartyCode(),
        null,
        key.costCenter(),
        narration(inv),
        Map.of(
            "NET_AMOUNT", net,
            "INPUT_VAT", vat,
            "WITHHOLDING_TAX", wht,
            "PAYABLE", net.add(vat).subtract(wht)),
        Map.of("EXPENSE", key.account()));
  }

  private static String narration(SupplierInvoice inv) {
    String text =
        "Supplier invoice "
            + inv.getSupplierInvoiceNo()
            + " of "
            + inv.getPartyCode()
            + (inv.getNarration() == null ? "" : " - " + inv.getNarration());
    return text.length() > NARRATION_MAX ? text.substring(0, NARRATION_MAX) : text;
  }

  private static BigDecimal sum(
      List<SupplierInvoiceLine> lines, Function<SupplierInvoiceLine, BigDecimal> f) {
    return lines.stream().map(f).reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private record GroupKey(String account, String costCenter) {}
}
