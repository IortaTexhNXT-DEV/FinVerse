package com.iortatechnxt.brokerverse.collections.unapplied.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/**
 * A collector disposition of an unapplied payment (BRCLXN.031/033, 040): the value of LOV {@code
 * CLX_UPP_DISPOSITION}, the target invoice when the value requires one (047/048), the amount and
 * remarks, and the request sent to Cashiering when the value has a Cashiering action. Append-only:
 * a later disposition of the same item is a new row, so the history is kept after the payment is
 * applied or refunded.
 */
@Entity
@Table(name = "clx_unapplied_disposition")
public class UnappliedDisposition extends BaseEntity {

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "unapplied_ref", nullable = false, length = 30, updatable = false)
  private String unappliedRef;

  @Column(name = "disposition_code", nullable = false, length = 40, updatable = false)
  private String dispositionCode;

  @Column(name = "cashiering_action", nullable = false, length = 20, updatable = false)
  private String cashieringAction;

  @Column(name = "invoice_no", length = 40, updatable = false)
  private String invoiceNo;

  @Column(precision = 19, scale = 2, updatable = false)
  private BigDecimal amount;

  @Column(length = 1000, updatable = false)
  private String remarks;

  @Column(name = "request_id")
  private Long requestId;

  protected UnappliedDisposition() {}

  /**
   * Records a disposition.
   *
   * @param companyId company
   * @param unappliedRef Cashiering reference of the item
   * @param content disposition, action, invoice, amount and remarks
   */
  public UnappliedDisposition(Long companyId, String unappliedRef, Content content) {
    this.companyId = companyId;
    this.unappliedRef = unappliedRef;
    this.dispositionCode = content.dispositionCode();
    this.cashieringAction = content.cashieringAction();
    this.invoiceNo = content.invoiceNo();
    this.amount = content.amount();
    this.remarks = content.remarks();
  }

  /**
   * Links the request sent to Cashiering.
   *
   * @param id request
   */
  public void linkRequest(Long id) {
    this.requestId = id;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getUnappliedRef() {
    return unappliedRef;
  }

  public String getDispositionCode() {
    return dispositionCode;
  }

  public String getCashieringAction() {
    return cashieringAction;
  }

  public String getInvoiceNo() {
    return invoiceNo;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public String getRemarks() {
    return remarks;
  }

  public Long getRequestId() {
    return requestId;
  }

  /**
   * What a disposition holds.
   *
   * @param dispositionCode value of CLX_UPP_DISPOSITION
   * @param cashieringAction APPLY_TO_INVOICE, REFUND, RECLASS, TRANSFER or NONE
   * @param invoiceNo target invoice, may be null
   * @param amount amount, null for the whole balance
   * @param remarks remarks, may be null
   */
  public record Content(
      String dispositionCode,
      String cashieringAction,
      String invoiceNo,
      BigDecimal amount,
      String remarks) {}
}
