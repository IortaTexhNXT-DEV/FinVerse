package com.iortatechnxt.brokerverse.booking.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A service invoice or credit (BRNB.100/100b): BIR-sequential {@code SI-<branch>-<yyyy>} number,
 * commission, VAT on commission and withholding tax lines, the PDF generated from the document
 * template (stored as issued) and its e-mail dispatch.
 */
@Entity
@Table(name = "bkg_service_invoice")
public class ServiceInvoice extends BaseEntity {

  private static final int MAX_ERROR = 1000;

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "branch_id", nullable = false, updatable = false)
  private Long branchId;

  @Column(name = "si_no", nullable = false, length = 40, updatable = false)
  private String siNo;

  @Column(name = "type_code", nullable = false, length = 40, updatable = false)
  private String typeCode;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 10, updatable = false)
  private SiKind kind;

  @Column(name = "invoice_no", length = 40, updatable = false)
  private String invoiceNo;

  @Column(length = 30, updatable = false)
  private String arn;

  @Column(name = "recipient_code", nullable = false, length = 30, updatable = false)
  private String recipientCode;

  @Column(name = "recipient_name", nullable = false, length = 250, updatable = false)
  private String recipientName;

  @Column(name = "recipient_email", length = 300)
  private String recipientEmail;

  @Column(name = "issue_date", nullable = false, updatable = false)
  private LocalDate issueDate;

  @Column(nullable = false, length = 3, updatable = false)
  private String currency;

  @Column(nullable = false, precision = 19, scale = 2, updatable = false)
  private BigDecimal commission;

  @Column(
      name = "vat_on_commission",
      nullable = false,
      precision = 19,
      scale = 2,
      updatable = false)
  private BigDecimal vatOnCommission;

  @Column(name = "wtax_amount", nullable = false, precision = 19, scale = 2, updatable = false)
  private BigDecimal wtaxAmount;

  @Column(name = "net_amount", nullable = false, precision = 19, scale = 2, updatable = false)
  private BigDecimal netAmount;

  @Column(name = "template_code", nullable = false, length = 40, updatable = false)
  private String templateCode;

  @Column(name = "template_version", nullable = false, updatable = false)
  private int templateVersion;

  @Basic(fetch = FetchType.LAZY)
  @Column(nullable = false)
  private byte[] document;

  @Column(name = "owner_username", length = 50, updatable = false)
  private String ownerUsername;

  @Column(name = "owner_permission", length = 60, updatable = false)
  private String ownerPermission;

  @Column(name = "message_id")
  private Long messageId;

  @Enumerated(EnumType.STRING)
  @Column(name = "dispatch_status", nullable = false, length = 20)
  private DispatchStatus dispatchStatus = DispatchStatus.NOT_SENT;

  @Column(name = "dispatch_error", length = MAX_ERROR)
  private String dispatchError;

  @Column(name = "credit_of", length = 40, updatable = false)
  private String creditOf;

  @Column(length = 500, updatable = false)
  private String remarks;

  protected ServiceInvoice() {}

  /**
   * Issues a service invoice.
   *
   * @param companyId company
   * @param branchId issuing branch
   * @param siNo number
   * @param values content
   */
  public ServiceInvoice(Long companyId, Long branchId, String siNo, Values values) {
    this.companyId = companyId;
    this.branchId = branchId;
    this.siNo = siNo;
    this.typeCode = values.typeCode();
    this.kind = values.kind();
    this.invoiceNo = values.invoiceNo();
    this.arn = values.arn();
    this.recipientCode = values.recipientCode();
    this.recipientName = values.recipientName();
    this.recipientEmail = values.recipientEmail();
    this.issueDate = values.issueDate();
    this.currency = values.currency();
    this.commission = values.commission();
    this.vatOnCommission = values.vatOnCommission();
    this.wtaxAmount = values.wtaxAmount();
    this.netAmount =
        values.commission().add(values.vatOnCommission()).subtract(values.wtaxAmount());
    this.ownerUsername = values.ownerUsername();
    this.ownerPermission = values.ownerPermission();
    this.creditOf = values.creditOf();
    this.remarks = values.remarks();
  }

  /**
   * Stores the generated document with the template version used (BRNB.004).
   *
   * @param template template code
   * @param version template version
   * @param pdf document
   */
  public void attachDocument(String template, int version, byte[] pdf) {
    this.templateCode = template;
    this.templateVersion = version;
    this.document = pdf.clone();
  }

  /**
   * The e-mail was queued.
   *
   * @param message outbox message
   * @param email recipient address
   */
  public void queued(Long message, String email) {
    this.messageId = message;
    this.recipientEmail = email;
    this.dispatchStatus = DispatchStatus.QUEUED;
    this.dispatchError = null;
  }

  /**
   * Outcome of the delivery (BRNB.100b).
   *
   * @param status SENT or FAILED (QUEUED while retrying)
   * @param error failure reason
   */
  public void dispatched(DispatchStatus status, String error) {
    this.dispatchStatus = status;
    this.dispatchError =
        error == null || error.length() <= MAX_ERROR ? error : error.substring(0, MAX_ERROR);
  }

  /**
   * Not e-mailed.
   *
   * @param reason why
   */
  public void notSent(String reason) {
    this.dispatchStatus = DispatchStatus.NOT_SENT;
    this.dispatchError = reason;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public Long getBranchId() {
    return branchId;
  }

  public String getSiNo() {
    return siNo;
  }

  public String getTypeCode() {
    return typeCode;
  }

  public SiKind getKind() {
    return kind;
  }

  public String getInvoiceNo() {
    return invoiceNo;
  }

  public String getArn() {
    return arn;
  }

  public String getRecipientCode() {
    return recipientCode;
  }

  public String getRecipientName() {
    return recipientName;
  }

  public String getRecipientEmail() {
    return recipientEmail;
  }

  public LocalDate getIssueDate() {
    return issueDate;
  }

  public String getCurrency() {
    return currency;
  }

  public BigDecimal getCommission() {
    return commission;
  }

  public BigDecimal getVatOnCommission() {
    return vatOnCommission;
  }

  public BigDecimal getWtaxAmount() {
    return wtaxAmount;
  }

  public BigDecimal getNetAmount() {
    return netAmount;
  }

  public String getTemplateCode() {
    return templateCode;
  }

  public int getTemplateVersion() {
    return templateVersion;
  }

  public byte[] getDocument() {
    return document == null ? new byte[0] : document.clone();
  }

  public String getOwnerUsername() {
    return ownerUsername;
  }

  public String getOwnerPermission() {
    return ownerPermission;
  }

  public Long getMessageId() {
    return messageId;
  }

  public DispatchStatus getDispatchStatus() {
    return dispatchStatus;
  }

  public String getDispatchError() {
    return dispatchError;
  }

  public String getCreditOf() {
    return creditOf;
  }

  public String getRemarks() {
    return remarks;
  }

  /**
   * Content of a service invoice.
   *
   * @param typeCode type
   * @param kind invoice or credit
   * @param invoiceNo booked invoice, null for a manual one
   * @param arn account, null for a manual one
   * @param recipientCode recipient (insurer party code or internal unit)
   * @param recipientName recipient name
   * @param recipientEmail billing e-mail, may be null
   * @param issueDate issue date
   * @param currency currency
   * @param commission commission line
   * @param vatOnCommission VAT line
   * @param wtaxAmount withholding tax line
   * @param ownerUsername owning user
   * @param ownerPermission owning team (permission)
   * @param creditOf service invoice credited, null for an invoice
   * @param remarks remarks
   */
  public record Values(
      String typeCode,
      SiKind kind,
      String invoiceNo,
      String arn,
      String recipientCode,
      String recipientName,
      String recipientEmail,
      LocalDate issueDate,
      String currency,
      BigDecimal commission,
      BigDecimal vatOnCommission,
      BigDecimal wtaxAmount,
      String ownerUsername,
      String ownerPermission,
      String creditOf,
      String remarks) {}
}
