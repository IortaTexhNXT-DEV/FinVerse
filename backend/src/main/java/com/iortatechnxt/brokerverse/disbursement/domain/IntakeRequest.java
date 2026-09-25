package com.iortatechnxt.brokerverse.disbursement.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.RequestSource;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.RequestStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * A payment request received by Disbursement (DIS 2.4.0-2.6.2, 3.25.0-3.25.2): from a module
 * through the gateway (with the linked Operations request), from an upload row or encoded by a
 * user. It carries the RFP number, disbursement type, payee, amount, purpose, root invoice and
 * supporting documents; its payee is matched to the payee master and a voucher is built from it.
 */
@Entity
@Table(name = "dsb_request")
public class IntakeRequest extends BaseEntity {

  private static final int MAX_REASON = 250;
  private static final String SEPARATOR = ",";

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "request_no", nullable = false, length = 30, updatable = false)
  private String requestNo;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20, updatable = false)
  private RequestSource source;

  @Column(name = "source_module", nullable = false, length = 30, updatable = false)
  private String sourceModule;

  @Column(name = "source_ref", nullable = false, length = 80, updatable = false)
  private String sourceRef;

  @Column(name = "rfp_no", length = 40, updatable = false)
  private String rfpNo;

  @Column(name = "disbursement_type", nullable = false, length = 30, updatable = false)
  private String disbursementType;

  @Column(name = "payee_class", length = 20)
  private String payeeClass;

  @Column(name = "payee_code", nullable = false, length = 30, updatable = false)
  private String payeeCode;

  @Column(name = "payee_name", length = 250)
  private String payeeName;

  @Column(name = "payee_id")
  private Long payeeId;

  @Column(nullable = false, length = 3, updatable = false)
  private String currency;

  @Column(nullable = false, precision = 19, scale = 2, updatable = false)
  private BigDecimal amount;

  @Column(length = 500, updatable = false)
  private String purpose;

  @Column(name = "received_at", nullable = false, updatable = false)
  private Instant receivedAt;

  @Column(name = "root_invoice_no", length = 40, updatable = false)
  private String rootInvoiceNo;

  @Column(name = "attachment_refs", length = 1000, updatable = false)
  private String attachmentRefs;

  @Column(name = "accounting_refs", length = 1000, updatable = false)
  private String accountingRefs;

  @Column(name = "straight_to_approval", nullable = false, updatable = false)
  private boolean straightToApproval;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private RequestStatus status = RequestStatus.RECEIVED;

  @Column(name = "status_reason", length = MAX_REASON)
  private String statusReason;

  @Column(name = "gateway_request_id", updatable = false)
  private Long gatewayRequestId;

  @Column(name = "upload_job_no", length = 30, updatable = false)
  private String uploadJobNo;

  @Column(name = "expense_account", length = 30, updatable = false)
  private String expenseAccount;

  @Column(name = "cost_center", length = 20, updatable = false)
  private String costCenter;

  @Column(name = "voucher_id")
  private Long voucherId;

  protected IntakeRequest() {}

  /**
   * A received request.
   *
   * @param requestNo request number ({@code DSR-<yyyy>-nnnnnn})
   * @param facts what was requested
   * @param receivedAt time received
   */
  public IntakeRequest(String requestNo, RequestFacts facts, Instant receivedAt) {
    this.companyId = facts.companyId();
    this.requestNo = requestNo;
    this.source = facts.source();
    this.sourceModule = facts.sourceModule();
    this.sourceRef = facts.sourceRef() == null ? requestNo : facts.sourceRef();
    this.rfpNo = facts.rfpNo();
    this.disbursementType = facts.disbursementType();
    this.payeeClass = facts.payeeClass();
    this.payeeCode = facts.payeeCode();
    this.payeeName = facts.payeeName();
    this.currency = facts.currency();
    this.amount = facts.amount();
    this.purpose = facts.purpose();
    this.rootInvoiceNo = facts.rootInvoiceNo();
    this.attachmentRefs = joined(facts.attachmentRefs());
    this.accountingRefs = joined(facts.accountingRefs());
    this.straightToApproval = facts.straightToApproval();
    this.gatewayRequestId = facts.gatewayRequestId();
    this.uploadJobNo = facts.uploadJobNo();
    this.expenseAccount = facts.expenseAccount();
    this.costCenter = facts.costCenter();
    this.receivedAt = receivedAt;
  }

  private static String joined(List<String> refs) {
    return refs == null || refs.isEmpty() ? null : String.join(SEPARATOR, refs);
  }

  /**
   * The payee was matched to the master (DIS 3.25.2).
   *
   * @param payee payee id
   * @param name payee name of the master
   * @param klass payee class of the master
   */
  public void matched(Long payee, String name, String klass) {
    requireIn(Set.of(RequestStatus.RECEIVED, RequestStatus.NO_PAYEE), "matched");
    payeeId = payee;
    payeeName = name;
    payeeClass = klass;
    status = RequestStatus.RECEIVED;
    statusReason = null;
  }

  /**
   * No payee of the master matches (DIS 3.25.0): the request waits for the payee.
   *
   * @param reason reason
   */
  public void noPayee(String reason) {
    requireIn(Set.of(RequestStatus.RECEIVED, RequestStatus.NO_PAYEE), "held for its payee");
    status = RequestStatus.NO_PAYEE;
    statusReason = cut(reason);
  }

  /**
   * A voucher was built for the request.
   *
   * @param voucher voucher id
   */
  public void inVoucher(Long voucher) {
    requireIn(Set.of(RequestStatus.RECEIVED), "put in a voucher");
    voucherId = voucher;
    status = RequestStatus.IN_VOUCHER;
  }

  /**
   * Returned to its source with a reason (DIS 3.25.0, 2.21.0).
   *
   * @param reason reason
   */
  public void returned(String reason) {
    requireIn(
        Set.of(RequestStatus.RECEIVED, RequestStatus.NO_PAYEE, RequestStatus.IN_VOUCHER),
        "returned");
    status = RequestStatus.RETURNED;
    statusReason = cut(reason);
  }

  /**
   * Cancelled with its voucher (DIS 2.9.0, 2.18.0, 2.20.0).
   *
   * @param reason reason
   */
  public void cancelled(String reason) {
    requireIn(
        Set.of(RequestStatus.RECEIVED, RequestStatus.NO_PAYEE, RequestStatus.IN_VOUCHER),
        "cancelled");
    status = RequestStatus.CANCELLED;
    statusReason = cut(reason);
  }

  /** Documents released without a voucher (BIR 2307 certificates, DBMID.001). */
  public void released() {
    requireIn(Set.of(RequestStatus.RECEIVED, RequestStatus.NO_PAYEE), "released");
    status = RequestStatus.RELEASED;
  }

  private void requireIn(Set<RequestStatus> allowed, String action) {
    if (!allowed.contains(status)) {
      throw new BusinessRuleException(
          "DISB_REQUEST_STATUS",
          "Request " + requestNo + " is " + status + " and cannot be " + action);
    }
  }

  private static String cut(String reason) {
    return reason == null || reason.length() <= MAX_REASON
        ? reason
        : reason.substring(0, MAX_REASON);
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getRequestNo() {
    return requestNo;
  }

  public RequestSource getSource() {
    return source;
  }

  public String getSourceModule() {
    return sourceModule;
  }

  public String getSourceRef() {
    return sourceRef;
  }

  public String getRfpNo() {
    return rfpNo;
  }

  public String getDisbursementType() {
    return disbursementType;
  }

  public String getPayeeClass() {
    return payeeClass;
  }

  public String getPayeeCode() {
    return payeeCode;
  }

  public String getPayeeName() {
    return payeeName;
  }

  public Long getPayeeId() {
    return payeeId;
  }

  public String getCurrency() {
    return currency;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public String getPurpose() {
    return purpose;
  }

  public Instant getReceivedAt() {
    return receivedAt;
  }

  public String getRootInvoiceNo() {
    return rootInvoiceNo;
  }

  /**
   * Supporting documents.
   *
   * @return references
   */
  public List<String> getAttachmentRefs() {
    return attachmentRefs == null ? List.of() : List.of(attachmentRefs.split(SEPARATOR));
  }

  /**
   * Accounting references the payment settles.
   *
   * @return references
   */
  public List<String> getAccountingRefs() {
    return accountingRefs == null ? List.of() : List.of(accountingRefs.split(SEPARATOR));
  }

  public boolean isStraightToApproval() {
    return straightToApproval;
  }

  public RequestStatus getStatus() {
    return status;
  }

  public String getStatusReason() {
    return statusReason;
  }

  public Long getGatewayRequestId() {
    return gatewayRequestId;
  }

  public String getUploadJobNo() {
    return uploadJobNo;
  }

  public String getExpenseAccount() {
    return expenseAccount;
  }

  public String getCostCenter() {
    return costCenter;
  }

  public Long getVoucherId() {
    return voucherId;
  }

  /**
   * What a payment request asks for.
   *
   * @param companyId company
   * @param source how it arrived
   * @param sourceModule source module (DISBURSEMENT for encoded and uploaded requests)
   * @param sourceRef source reference, null for the request number
   * @param rfpNo request-for-payment number
   * @param disbursementType disbursement type (LOV {@code DISBURSEMENT_TYPE})
   * @param payeeClass payee class, may be null
   * @param payeeCode payee party code
   * @param payeeName payee name
   * @param currency currency
   * @param amount positive amount
   * @param purpose purpose
   * @param rootInvoiceNo root invoice (DIS 3.27.2)
   * @param attachmentRefs supporting documents
   * @param accountingRefs accounting references settled
   * @param straightToApproval requested straight to the approver
   * @param gatewayRequestId linked Operations request
   * @param uploadJobNo upload that created it
   * @param expenseAccount expense account of an OTHER payment, may be null
   * @param costCenter cost centre, may be null
   */
  public record RequestFacts(
      Long companyId,
      RequestSource source,
      String sourceModule,
      String sourceRef,
      String rfpNo,
      String disbursementType,
      String payeeClass,
      String payeeCode,
      String payeeName,
      String currency,
      BigDecimal amount,
      String purpose,
      String rootInvoiceNo,
      List<String> attachmentRefs,
      List<String> accountingRefs,
      boolean straightToApproval,
      Long gatewayRequestId,
      String uploadJobNo,
      String expenseAccount,
      String costCenter) {

    /** Defensive copies. */
    public RequestFacts {
      attachmentRefs = attachmentRefs == null ? List.of() : List.copyOf(attachmentRefs);
      accountingRefs = accountingRefs == null ? List.of() : List.copyOf(accountingRefs);
    }
  }
}
