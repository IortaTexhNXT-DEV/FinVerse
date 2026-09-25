package com.iortatechnxt.brokerverse.disbursement.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.CwtDirection;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.TagKind;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A tag on a voucher (DIS 2.10.0-2.11.2): the official / acknowledgement receipt received from the
 * payee (number, date, date received), or a creditable withholding tax certificate received from an
 * insurer or released to a supplier (period covered, date received / released, amount, certificate
 * reference).
 */
@Entity
@Table(name = "dsb_voucher_tag")
public class VoucherTag extends BaseEntity {

  @Column(name = "voucher_id", nullable = false, updatable = false)
  private Long voucherId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 10, updatable = false)
  private TagKind kind;

  @Enumerated(EnumType.STRING)
  @Column(length = 10, updatable = false)
  private CwtDirection direction;

  @Column(name = "doc_no", length = 40, updatable = false)
  private String docNo;

  @Column(name = "doc_date", updatable = false)
  private LocalDate docDate;

  @Column(name = "received_on", updatable = false)
  private LocalDate receivedOn;

  @Column(name = "released_on", updatable = false)
  private LocalDate releasedOn;

  @Column(name = "period_from", updatable = false)
  private LocalDate periodFrom;

  @Column(name = "period_to", updatable = false)
  private LocalDate periodTo;

  @Column(precision = 19, scale = 2, updatable = false)
  private BigDecimal amount;

  @Column(name = "certificate_ref", length = 60, updatable = false)
  private String certificateRef;

  @Column(length = 500, updatable = false)
  private String remarks;

  protected VoucherTag() {}

  /**
   * An OR / AR tag (DIS 2.10.2).
   *
   * @param voucherId voucher
   * @param receipt receipt details
   * @return tag
   */
  public static VoucherTag receipt(Long voucherId, ReceiptTag receipt) {
    VoucherTag t = new VoucherTag();
    t.voucherId = voucherId;
    t.kind = TagKind.OR_AR;
    t.docNo = receipt.receiptNo();
    t.docDate = receipt.receiptDate();
    t.receivedOn = receipt.receivedOn();
    t.amount = receipt.amount();
    t.remarks = receipt.remarks();
    return t;
  }

  /**
   * A CWT tag (DIS 2.11.2).
   *
   * @param voucherId voucher
   * @param cwt certificate details
   * @return tag
   */
  public static VoucherTag cwt(Long voucherId, CwtTag cwt) {
    VoucherTag t = new VoucherTag();
    t.voucherId = voucherId;
    t.kind = TagKind.CWT;
    t.direction = cwt.direction();
    t.docNo = cwt.certificateNo();
    t.periodFrom = cwt.periodFrom();
    t.periodTo = cwt.periodTo();
    t.receivedOn = cwt.direction() == CwtDirection.RECEIVED ? cwt.on() : null;
    t.releasedOn = cwt.direction() == CwtDirection.RELEASED ? cwt.on() : null;
    t.amount = cwt.amount();
    t.certificateRef = cwt.certificateRef();
    t.remarks = cwt.remarks();
    return t;
  }

  public Long getVoucherId() {
    return voucherId;
  }

  public TagKind getKind() {
    return kind;
  }

  public CwtDirection getDirection() {
    return direction;
  }

  public String getDocNo() {
    return docNo;
  }

  public LocalDate getDocDate() {
    return docDate;
  }

  public LocalDate getReceivedOn() {
    return receivedOn;
  }

  public LocalDate getReleasedOn() {
    return releasedOn;
  }

  public LocalDate getPeriodFrom() {
    return periodFrom;
  }

  public LocalDate getPeriodTo() {
    return periodTo;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public String getCertificateRef() {
    return certificateRef;
  }

  public String getRemarks() {
    return remarks;
  }

  /**
   * An OR / AR received.
   *
   * @param receiptNo OR / AR number
   * @param receiptDate receipt date
   * @param receivedOn date received
   * @param amount amount, may be null
   * @param remarks remarks
   */
  public record ReceiptTag(
      String receiptNo,
      LocalDate receiptDate,
      LocalDate receivedOn,
      BigDecimal amount,
      String remarks) {}

  /**
   * A CWT certificate received or released.
   *
   * @param direction received (insurer) or released (supplier)
   * @param certificateNo certificate number
   * @param periodFrom period covered from
   * @param periodTo period covered to
   * @param on date received or released
   * @param amount amount withheld
   * @param certificateRef register reference (tax certificate), may be null
   * @param remarks remarks
   */
  public record CwtTag(
      CwtDirection direction,
      String certificateNo,
      LocalDate periodFrom,
      LocalDate periodTo,
      LocalDate on,
      BigDecimal amount,
      String certificateRef,
      String remarks) {}
}
