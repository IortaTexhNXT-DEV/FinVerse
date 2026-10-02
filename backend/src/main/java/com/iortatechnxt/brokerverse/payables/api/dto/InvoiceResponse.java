package com.iortatechnxt.brokerverse.payables.api.dto;

import com.iortatechnxt.brokerverse.payables.domain.InvoiceStatus;
import com.iortatechnxt.brokerverse.payables.domain.SupplierInvoice;
import com.iortatechnxt.brokerverse.payables.domain.SupplierInvoiceLine;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Supplier invoice view ({@code lines} empty in list results).
 *
 * @param id id
 * @param documentNo internal document number
 * @param companyId company
 * @param branchId branch
 * @param partyCode supplier
 * @param supplierInvoiceNo supplier's invoice number
 * @param invoiceDate invoice date
 * @param dueDate due date
 * @param currency currency
 * @param vatApplicable VAT applies
 * @param whtRate withholding rate (percent)
 * @param netAmount net amount
 * @param vatAmount input VAT
 * @param whtAmount withholding tax
 * @param payableAmount payable to the supplier
 * @param narration narration
 * @param status status
 * @param statusReason last rejection / cancellation reason
 * @param createdBy maker
 * @param submittedBy submitter
 * @param approvedBy checker
 * @param approvedAt approval time
 * @param journalBatchNos GL journals
 * @param lines lines
 */
public record InvoiceResponse(
    Long id,
    String documentNo,
    Long companyId,
    Long branchId,
    String partyCode,
    String supplierInvoiceNo,
    LocalDate invoiceDate,
    LocalDate dueDate,
    String currency,
    boolean vatApplicable,
    BigDecimal whtRate,
    BigDecimal netAmount,
    BigDecimal vatAmount,
    BigDecimal whtAmount,
    BigDecimal payableAmount,
    String narration,
    InvoiceStatus status,
    String statusReason,
    String createdBy,
    String submittedBy,
    String approvedBy,
    Instant approvedAt,
    String journalBatchNos,
    List<LineResponse> lines) {

  /**
   * Maps an invoice with its lines.
   *
   * @param i invoice (lines loaded)
   * @return response
   */
  public static InvoiceResponse from(SupplierInvoice i) {
    return of(i, i.getLines().stream().map(LineResponse::from).toList());
  }

  /**
   * Maps an invoice without lines (lists).
   *
   * @param i invoice
   * @return response
   */
  public static InvoiceResponse summary(SupplierInvoice i) {
    return of(i, List.of());
  }

  private static InvoiceResponse of(SupplierInvoice i, List<LineResponse> lines) {
    return new InvoiceResponse(
        i.getId(),
        i.getDocumentNo(),
        i.getCompanyId(),
        i.getBranchId(),
        i.getPartyCode(),
        i.getSupplierInvoiceNo(),
        i.getInvoiceDate(),
        i.getDueDate(),
        i.getCurrency(),
        i.isVatApplicable(),
        i.getWhtRate(),
        i.getNetAmount(),
        i.getVatAmount(),
        i.getWhtAmount(),
        i.getPayableAmount(),
        i.getNarration(),
        i.getStatus(),
        i.getStatusReason(),
        i.getCreatedBy(),
        i.getSubmittedBy(),
        i.getApprovedBy(),
        i.getApprovedAt(),
        i.getJournalBatchNos(),
        lines);
  }

  /**
   * Invoice line view.
   *
   * @param lineNo line number
   * @param expenseAccountCode GL account
   * @param costCenter cost centre
   * @param description description
   * @param netAmount net amount
   * @param vatAmount VAT
   * @param whtAmount withholding tax
   */
  public record LineResponse(
      int lineNo,
      String expenseAccountCode,
      String costCenter,
      String description,
      BigDecimal netAmount,
      BigDecimal vatAmount,
      BigDecimal whtAmount) {

    /**
     * Maps a line.
     *
     * @param l line
     * @return response
     */
    public static LineResponse from(SupplierInvoiceLine l) {
      return new LineResponse(
          l.getLineNo(),
          l.getExpenseAccountCode(),
          l.getCostCenter(),
          l.getDescription(),
          l.getNetAmount(),
          l.getVatAmount(),
          l.getWhtAmount());
    }
  }
}
