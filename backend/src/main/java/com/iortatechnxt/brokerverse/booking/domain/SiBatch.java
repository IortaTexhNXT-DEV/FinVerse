package com.iortatechnxt.brokerverse.booking.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * The service invoices issued to one insurer on one business date, sent to the insurer in one
 * e-mail (G09). There is one batch per company, insurer and date; its delivery status follows the
 * e-mail and a failed batch can be sent again.
 */
@Entity
@Table(name = "bkg_si_batch")
public class SiBatch extends BaseEntity {

  private static final int MAX_ERROR = 1000;

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "insurer_code", nullable = false, length = 30, updatable = false)
  private String insurerCode;

  @Column(name = "insurer_name", nullable = false, length = 250, updatable = false)
  private String insurerName;

  @Column(name = "batch_date", nullable = false, updatable = false)
  private LocalDate batchDate;

  @Column(name = "sequence_no", nullable = false, updatable = false)
  private int sequenceNo = 1;

  @Column(name = "item_count", nullable = false, updatable = false)
  private int itemCount;

  @Column(length = 3, updatable = false)
  private String currency;

  @Column(name = "commission_total", nullable = false, precision = 19, scale = 2, updatable = false)
  private BigDecimal commissionTotal;

  @Column(name = "net_total", nullable = false, precision = 19, scale = 2, updatable = false)
  private BigDecimal netTotal;

  @Enumerated(EnumType.STRING)
  @Column(name = "delivery_status", nullable = false, length = 12)
  private SiBatchStatus status = SiBatchStatus.GENERATED;

  @Column(nullable = false)
  private int attempts;

  @Column(name = "recipient_email", length = 300)
  private String recipientEmail;

  @Column(name = "message_id")
  private Long messageId;

  @Column(name = "last_error", length = MAX_ERROR)
  private String lastError;

  @Column(name = "sent_at")
  private Instant sentAt;

  @ElementCollection
  @CollectionTable(name = "bkg_si_batch_item", joinColumns = @JoinColumn(name = "batch_id"))
  @Column(name = "service_invoice_id", nullable = false)
  private final List<Long> serviceInvoiceIds = new ArrayList<>();

  protected SiBatch() {}

  /**
   * Generates the batch of an insurer and date.
   *
   * @param companyId company
   * @param insurerCode insurer party
   * @param insurerName insurer name
   * @param batchDate business date of the service invoices
   * @param sequenceNo 1 for the daily batch, 2 and up for a supplementary batch
   * @param items the service invoices, at least one
   */
  public SiBatch(
      Long companyId,
      String insurerCode,
      String insurerName,
      LocalDate batchDate,
      int sequenceNo,
      List<ServiceInvoice> items) {
    if (items.isEmpty()) {
      throw new BusinessRuleException("SI_BATCH_EMPTY", "A batch needs at least one invoice");
    }
    this.companyId = companyId;
    this.insurerCode = insurerCode;
    this.insurerName = insurerName;
    this.batchDate = batchDate;
    this.sequenceNo = sequenceNo;
    this.itemCount = items.size();
    List<String> currencies = items.stream().map(ServiceInvoice::getCurrency).distinct().toList();
    this.currency = currencies.size() == 1 ? currencies.get(0) : null;
    this.commissionTotal =
        items.stream().map(ServiceInvoice::getCommission).reduce(BigDecimal.ZERO, BigDecimal::add);
    this.netTotal =
        items.stream().map(ServiceInvoice::getNetAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    items.forEach(si -> serviceInvoiceIds.add(si.getId()));
  }

  /**
   * The e-mail of the batch was queued.
   *
   * @param email billing address
   * @param message outbox message
   */
  public void queued(String email, Long message) {
    this.recipientEmail = email;
    this.messageId = message;
    this.attempts++;
    this.lastError = null;
  }

  /**
   * The batch is sent again by a user: only a failed batch, or one never sent.
   *
   * @return true when the batch was failed (a retry)
   */
  public boolean retry() {
    if (status == SiBatchStatus.SENT || status == SiBatchStatus.RETRIED) {
      throw new BusinessRuleException(
          "SI_BATCH_NOT_RESENDABLE", "Only a failed or unsent batch can be sent again");
    }
    boolean retried = status == SiBatchStatus.FAILED;
    if (retried) {
      this.status = SiBatchStatus.RETRIED;
    }
    return retried;
  }

  /**
   * The batch was delivered.
   *
   * @param when delivery time
   */
  public void sent(Instant when) {
    this.status = SiBatchStatus.SENT;
    this.sentAt = when;
    this.lastError = null;
  }

  /**
   * The batch could not be delivered.
   *
   * @param reason reason
   */
  public void failed(String reason) {
    this.status = SiBatchStatus.FAILED;
    this.lastError =
        reason == null || reason.length() <= MAX_ERROR ? reason : reason.substring(0, MAX_ERROR);
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getInsurerCode() {
    return insurerCode;
  }

  public String getInsurerName() {
    return insurerName;
  }

  public LocalDate getBatchDate() {
    return batchDate;
  }

  public int getItemCount() {
    return itemCount;
  }

  public String getCurrency() {
    return currency;
  }

  public BigDecimal getCommissionTotal() {
    return commissionTotal;
  }

  public BigDecimal getNetTotal() {
    return netTotal;
  }

  public SiBatchStatus getStatus() {
    return status;
  }

  public int getAttempts() {
    return attempts;
  }

  public String getRecipientEmail() {
    return recipientEmail;
  }

  public Long getMessageId() {
    return messageId;
  }

  public String getLastError() {
    return lastError;
  }

  public Instant getSentAt() {
    return sentAt;
  }

  public List<Long> getServiceInvoiceIds() {
    return List.copyOf(serviceInvoiceIds);
  }

  /**
   * Number of the batch of its insurer and date: 1 for the daily batch, 2 and up for a
   * supplementary batch.
   *
   * @return sequence
   */
  public int getSequenceNo() {
    return sequenceNo;
  }
}
