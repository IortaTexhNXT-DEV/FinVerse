package com.iortatechnxt.brokerverse.eb.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * An insurer statement of account of a programme (BRID-021; FR-EB-053), numbered {@code
 * EBS-<yyyy>-nnnnnn}, with its {@code EB_SOA} work case whose stage it mirrors: the insurer's SOA
 * number, period, amount, the stored file and its hash (duplicate check) and the booked invoices it
 * bills. The payment status is read from the invoice ledger, never copied.
 */
@Entity
@Table(name = "eb_soa")
public class EbSoa extends BaseEntity {

  /** Stage of an SOA (mirror of {@code EB_SOA}). */
  public enum Status {
    /** Registered, to be validated. */
    RECEIVED,
    /** Validated by Processing. */
    VALIDATED,
    /** Released to the client and Collection. */
    RELEASED,
    /** Rejected with a reason. */
    REJECTED
  }

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "soa_no", nullable = false, length = 30, updatable = false)
  private String soaNo;

  @Column(name = "programme_id", nullable = false, updatable = false)
  private Long programmeId;

  @Column(name = "insurer_code", nullable = false, length = 30, updatable = false)
  private String insurerCode;

  @Column(name = "insurer_soa_no", nullable = false, length = 60, updatable = false)
  private String insurerSoaNo;

  @Column(name = "period_from", nullable = false)
  private LocalDate periodFrom;

  @Column(name = "period_to", nullable = false)
  private LocalDate periodTo;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal amount;

  @Column(nullable = false, length = 3)
  private String currency;

  @Column(name = "attachment_id", nullable = false, updatable = false)
  private Long attachmentId;

  @Column(name = "file_hash", nullable = false, length = 64, updatable = false)
  private String fileHash;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Status status = Status.RECEIVED;

  @Column(name = "received_on", nullable = false)
  private LocalDate receivedOn;

  @Column(name = "validated_at")
  private Instant validatedAt;

  @Column(name = "validated_by", length = 50)
  private String validatedBy;

  @Column(name = "released_at")
  private Instant releasedAt;

  @Column(name = "released_by", length = 50)
  private String releasedBy;

  @Column(name = "reject_reason", length = 40)
  private String rejectReason;

  @Column(length = 1000)
  private String remarks;

  @Column(name = "paid_notified_at")
  private Instant paidNotifiedAt;

  @OneToMany(mappedBy = "soa", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("invoiceNo")
  private final List<Invoice> invoices = new ArrayList<>();

  protected EbSoa() {}

  /**
   * Registers a received SOA.
   *
   * @param programme programme
   * @param soaNo intake number
   * @param intake insurer, SOA number, period, amount, currency, date received and remarks
   * @param file stored file and its hash
   */
  public EbSoa(EbProgramme programme, String soaNo, Intake intake, StoredFile file) {
    this.companyId = programme.getCompanyId();
    this.programmeId = programme.getId();
    this.soaNo = soaNo;
    this.insurerCode = intake.insurerCode();
    this.insurerSoaNo = intake.insurerSoaNo();
    this.periodFrom = intake.periodFrom();
    this.periodTo = intake.periodTo();
    this.amount = intake.amount();
    this.currency = intake.currency();
    this.receivedOn = intake.receivedOn();
    this.remarks = intake.remarks();
    this.attachmentId = file.attachmentId();
    this.fileHash = file.hash();
  }

  /**
   * Replaces the invoices billed by the SOA.
   *
   * @param invoiceNos booked invoices
   */
  public void linkInvoices(Collection<String> invoiceNos) {
    invoices.clear();
    invoiceNos.forEach(n -> invoices.add(new Invoice(this, n)));
  }

  /**
   * Records the validation.
   *
   * @param by user
   * @param at time
   */
  public void validated(String by, Instant at) {
    this.validatedBy = by;
    this.validatedAt = at;
  }

  /**
   * Records the release.
   *
   * @param by user
   * @param at time
   */
  public void released(String by, Instant at) {
    this.releasedBy = by;
    this.releasedAt = at;
  }

  /**
   * Records the rejection reason.
   *
   * @param reason list EB_SOA_REJECT_REASON
   */
  public void rejected(String reason) {
    this.rejectReason = reason;
  }

  /**
   * Records that the payment was notified.
   *
   * @param at time
   */
  public void paidNotified(Instant at) {
    this.paidNotifiedAt = at;
  }

  /**
   * Mirrors the stage of the work case.
   *
   * @param stage new stage
   */
  public void mirror(Status stage) {
    this.status = stage;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getSoaNo() {
    return soaNo;
  }

  public Long getProgrammeId() {
    return programmeId;
  }

  public String getInsurerCode() {
    return insurerCode;
  }

  public String getInsurerSoaNo() {
    return insurerSoaNo;
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

  public String getCurrency() {
    return currency;
  }

  public Long getAttachmentId() {
    return attachmentId;
  }

  public String getFileHash() {
    return fileHash;
  }

  public Status getStatus() {
    return status;
  }

  public LocalDate getReceivedOn() {
    return receivedOn;
  }

  public Instant getValidatedAt() {
    return validatedAt;
  }

  public String getValidatedBy() {
    return validatedBy;
  }

  public Instant getReleasedAt() {
    return releasedAt;
  }

  public String getReleasedBy() {
    return releasedBy;
  }

  public String getRejectReason() {
    return rejectReason;
  }

  public String getRemarks() {
    return remarks;
  }

  public Instant getPaidNotifiedAt() {
    return paidNotifiedAt;
  }

  public List<String> getInvoiceNos() {
    return invoices.stream().map(Invoice::getInvoiceNo).toList();
  }

  /**
   * What the insurer's SOA states.
   *
   * @param insurerCode insurer
   * @param insurerSoaNo insurer's SOA number
   * @param periodFrom period from
   * @param periodTo period to
   * @param amount amount
   * @param currency currency
   * @param receivedOn date received
   * @param remarks remarks, may be null
   */
  public record Intake(
      String insurerCode,
      String insurerSoaNo,
      LocalDate periodFrom,
      LocalDate periodTo,
      BigDecimal amount,
      String currency,
      LocalDate receivedOn,
      String remarks) {}

  /**
   * The stored SOA file.
   *
   * @param attachmentId file
   * @param hash SHA-256 of the content
   */
  public record StoredFile(Long attachmentId, String hash) {}

  /** An invoice billed by an SOA. */
  @Entity(name = "EbSoaInvoice")
  @Table(name = "eb_soa_invoice")
  public static class Invoice extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "soa_id", nullable = false, updatable = false)
    private EbSoa soa;

    @Column(name = "invoice_no", nullable = false, length = 40)
    private String invoiceNo;

    protected Invoice() {}

    Invoice(EbSoa soa, String invoiceNo) {
      this.soa = soa;
      this.invoiceNo = invoiceNo;
    }

    public String getInvoiceNo() {
      return invoiceNo;
    }
  }
}
