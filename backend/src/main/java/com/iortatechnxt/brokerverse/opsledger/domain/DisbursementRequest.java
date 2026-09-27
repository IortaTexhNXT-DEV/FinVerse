package com.iortatechnxt.brokerverse.opsledger.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * A payment request in the in-app Disbursement queue, the default {@code DisbursementGateway} until
 * the Disbursement system and BRD are known (OQ02): remittances to insurers (RMTID.011), refunds
 * (CSHID.024), BIR 2307 reports (DBMID.001) and incentive pass-on (CMRID.006). Status: SENT,
 * ACKNOWLEDGED, DV_ASSIGNED, PAID, RETURNED or CANCELLED.
 *
 * <p>BRD-5 (DIS 3.25.0, 2.20.0, 3.27.2) extends the request with the RFP number, payee class,
 * disbursement type, root invoice, supporting documents, accounting references to settle, the
 * straight-to-approval routing and the DV / instrument statuses reported by Disbursement (V765).
 */
@Entity
@Table(name = "ops_disbursement_request")
public class DisbursementRequest extends BaseEntity {

  private static final int MAX_REASON = 250;
  private static final int MAX_REFS = 1000;
  private static final String SEPARATOR = ",";

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "request_no", nullable = false, length = 30, updatable = false)
  private String requestNo;

  @Enumerated(EnumType.STRING)
  @Column(name = "request_type", nullable = false, length = 20, updatable = false)
  private Type requestType;

  @Column(name = "source_module", nullable = false, length = 30, updatable = false)
  private String sourceModule;

  @Column(name = "source_ref", nullable = false, length = 80, updatable = false)
  private String sourceRef;

  @Column(name = "payee_code", nullable = false, length = 30, updatable = false)
  private String payeeCode;

  @Column(name = "payee_name", length = 250, updatable = false)
  private String payeeName;

  @Column(nullable = false, length = 3, updatable = false)
  private String currency;

  @Column(nullable = false, precision = 19, scale = 2, updatable = false)
  private BigDecimal amount;

  @Column(length = 500, updatable = false)
  private String description;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Status status = Status.SENT;

  @Column(name = "dv_no", length = 40)
  private String dvNo;

  @Column(name = "sent_at", nullable = false, updatable = false)
  private Instant sentAt;

  @Column(name = "acknowledged_at")
  private Instant acknowledgedAt;

  @Column(name = "dv_assigned_at")
  private Instant dvAssignedAt;

  @Column(name = "paid_at")
  private Instant paidAt;

  @Column(name = "returned_at")
  private Instant returnedAt;

  @Column(name = "return_reason", length = MAX_REASON)
  private String returnReason;

  @Column(name = "attachment_ref", length = 80, updatable = false)
  private String attachmentRef;

  @Column(name = "rfp_no", length = 40, updatable = false)
  private String rfpNo;

  @Column(name = "payee_class", length = 30, updatable = false)
  private String payeeClass;

  @Column(name = "disbursement_type", length = 40, updatable = false)
  private String disbursementType;

  @Column(name = "root_invoice_no", length = 40, updatable = false)
  private String rootInvoiceNo;

  @Column(name = "attachment_refs", length = MAX_REFS, updatable = false)
  private String attachmentRefs;

  @Column(name = "accounting_refs", length = MAX_REFS, updatable = false)
  private String accountingRefs;

  @Column(name = "straight_to_approval", nullable = false, updatable = false)
  private boolean straightToApproval;

  @Column(name = "dv_status", length = 30)
  private String dvStatus;

  @Column(name = "instrument_status", length = 30)
  private String instrumentStatus;

  @Column(name = "cancelled_at")
  private Instant cancelledAt;

  @Column(name = "cancel_reason", length = MAX_REASON)
  private String cancelReason;

  protected DisbursementRequest() {}

  /**
   * A request sent to the queue.
   *
   * @param companyId company
   * @param requestNo request number
   * @param spec what to pay
   * @param at time
   */
  public DisbursementRequest(Long companyId, String requestNo, Spec spec, Instant at) {
    this.companyId = companyId;
    this.requestNo = requestNo;
    this.requestType = spec.type();
    this.sourceModule = spec.sourceModule();
    this.sourceRef = spec.sourceRef();
    this.payeeCode = spec.payeeCode();
    this.payeeName = spec.payeeName();
    this.currency = spec.currency();
    this.amount = spec.amount();
    this.description = spec.description();
    this.attachmentRef = spec.attachmentRef();
    this.rfpNo = spec.rfpNo();
    this.payeeClass = spec.payeeClass();
    this.disbursementType = spec.disbursementType();
    this.rootInvoiceNo = spec.rootInvoiceNo();
    this.attachmentRefs = joined(spec.attachmentRefs());
    this.accountingRefs = joined(spec.accountingRefs());
    this.straightToApproval = spec.straightToApproval();
    this.sentAt = at;
  }

  private static String joined(List<String> refs) {
    if (refs.isEmpty()) {
      return null;
    }
    String text = String.join(SEPARATOR, refs);
    if (text.length() > MAX_REFS) {
      throw new BusinessRuleException(
          "DISBURSEMENT_REFERENCES", "Too many references on one payment request");
    }
    return text;
  }

  private static List<String> split(String refs) {
    return refs == null ? List.of() : List.of(refs.split(SEPARATOR));
  }

  /**
   * Disbursement acknowledges receipt.
   *
   * @param at time
   */
  public void acknowledge(Instant at) {
    requireIn(Set.of(Status.SENT), "acknowledged");
    status = Status.ACKNOWLEDGED;
    acknowledgedAt = at;
  }

  /**
   * Disbursement assigns the disbursement voucher number (RMTID.019).
   *
   * @param number DV number
   * @param at time
   */
  public void assignDv(String number, Instant at) {
    requireIn(Set.of(Status.SENT, Status.ACKNOWLEDGED), "given a DV number");
    if (acknowledgedAt == null) {
      acknowledgedAt = at;
    }
    status = Status.DV_ASSIGNED;
    dvNo = number;
    dvAssignedAt = at;
  }

  /**
   * The payment was released.
   *
   * @param at time
   */
  public void markPaid(Instant at) {
    requireIn(Set.of(Status.DV_ASSIGNED), "paid");
    status = Status.PAID;
    paidAt = at;
  }

  /**
   * Disbursement returns the request to its source with a reason.
   *
   * @param reason reason
   * @param at time
   */
  public void returnToSource(String reason, Instant at) {
    requireIn(Set.of(Status.SENT, Status.ACKNOWLEDGED, Status.DV_ASSIGNED), "returned");
    status = Status.RETURNED;
    returnReason = reason.length() <= MAX_REASON ? reason : reason.substring(0, MAX_REASON);
    returnedAt = at;
  }

  /**
   * Disbursement cancels the request or its approved DV (DIS 2.20.0): the source module restores
   * its records on the {@code DisbursementStatusChanged} event with status CANCELLED.
   *
   * @param reason reason
   * @param at time
   */
  public void cancel(String reason, Instant at) {
    requireIn(
        Set.of(Status.SENT, Status.ACKNOWLEDGED, Status.DV_ASSIGNED, Status.PAID), "cancelled");
    status = Status.CANCELLED;
    cancelReason = reason.length() <= MAX_REASON ? reason : reason.substring(0, MAX_REASON);
    cancelledAt = at;
  }

  /**
   * Records the DV stage and instrument status reported by Disbursement (DIS 2.8, 3.26; design
   * section 7.2) without changing the gateway status.
   *
   * @param dvStage DV stage (IN_PROCESS, FOR_REVIEW, FOR_APPROVAL, APPROVED, ...), may be null
   * @param instrument instrument status (PRINTED, RELEASED, CREDITED, ...), may be null
   */
  public void track(String dvStage, String instrument) {
    requireIn(Set.of(Status.SENT, Status.ACKNOWLEDGED, Status.DV_ASSIGNED, Status.PAID), "tracked");
    if (dvStage != null) {
      dvStatus = dvStage;
    }
    if (instrument != null) {
      instrumentStatus = instrument;
    }
  }

  private void requireIn(Set<Status> allowed, String action) {
    if (!allowed.contains(status)) {
      throw new BusinessRuleException(
          "DISBURSEMENT_STATUS",
          "Payment request " + requestNo + " is " + status + " and cannot be " + action);
    }
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getRequestNo() {
    return requestNo;
  }

  public Type getRequestType() {
    return requestType;
  }

  public String getSourceModule() {
    return sourceModule;
  }

  public String getSourceRef() {
    return sourceRef;
  }

  public String getPayeeCode() {
    return payeeCode;
  }

  public String getPayeeName() {
    return payeeName;
  }

  public String getCurrency() {
    return currency;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public String getDescription() {
    return description;
  }

  public Status getStatus() {
    return status;
  }

  public String getDvNo() {
    return dvNo;
  }

  public Instant getSentAt() {
    return sentAt;
  }

  public Instant getAcknowledgedAt() {
    return acknowledgedAt;
  }

  public Instant getDvAssignedAt() {
    return dvAssignedAt;
  }

  public Instant getPaidAt() {
    return paidAt;
  }

  public Instant getReturnedAt() {
    return returnedAt;
  }

  public String getReturnReason() {
    return returnReason;
  }

  public String getAttachmentRef() {
    return attachmentRef;
  }

  public String getRfpNo() {
    return rfpNo;
  }

  public String getPayeeClass() {
    return payeeClass;
  }

  public String getDisbursementType() {
    return disbursementType;
  }

  public String getRootInvoiceNo() {
    return rootInvoiceNo;
  }

  /**
   * Supporting documents (attachment or extract ids), the main one first.
   *
   * @return references, empty when none
   */
  public List<String> getAttachmentRefs() {
    return split(attachmentRefs);
  }

  /**
   * Open items or accounting event references the payment settles.
   *
   * @return references, empty when none
   */
  public List<String> getAccountingRefs() {
    return split(accountingRefs);
  }

  public boolean isStraightToApproval() {
    return straightToApproval;
  }

  public String getDvStatus() {
    return dvStatus;
  }

  public String getInstrumentStatus() {
    return instrumentStatus;
  }

  public Instant getCancelledAt() {
    return cancelledAt;
  }

  public String getCancelReason() {
    return cancelReason;
  }

  /** What is paid. */
  public enum Type {
    /** Remittance to an insurer (RMTID.011). */
    REMITTANCE,
    /** Refund to a client (CSHID.024). */
    REFUND,
    /** BIR 2307 report and certificates to release to insurers (DBMID.001). */
    CWT2307,
    /** Incentive pass-on to branches (CMRID.006). */
    PASS_ON,
    /** Payment to a supplier (DIS 2.6.1). */
    SUPPLIER,
    /** Payment to a government agency (DIS 2.6.1). */
    GOVERNMENT,
    /** Payment to another BDO unit (DIS 2.6.1). */
    OTHER_BANK_UNIT,
    /** Employee-related payment (DIS 2.6.1). */
    EMPLOYEE,
    /** Employee cash advance (MKT, request for payment). */
    CASH_ADVANCE,
    /** Service fee to units or referrers (FRBS 2.10). */
    SERVICE_FEE,
    /** Any other disbursement (DIS 2.6.1). */
    OTHER
  }

  /** Status in the Disbursement queue. */
  public enum Status {
    /** Sent to Disbursement. */
    SENT,
    /** Received by Disbursement. */
    ACKNOWLEDGED,
    /** Disbursement voucher number assigned. */
    DV_ASSIGNED,
    /** Payment released. */
    PAID,
    /** Returned to the source module. */
    RETURNED,
    /**
     * Cancelled by Disbursement, also after the DV was approved (DIS 2.20.0): the source restores
     * its records (remittance lines extractable again, refund re-opened).
     */
    CANCELLED
  }

  /**
   * A payment request to create (DIS 3.25.0).
   *
   * @param type request type
   * @param sourceModule source module
   * @param sourceRef source reference (unique per module)
   * @param payeeCode payee party code
   * @param payeeName payee name
   * @param currency currency
   * @param amount positive amount
   * @param description description
   * @param attachmentRef main supporting document (attachment or extract id), may be null
   * @param rfpNo request-for-payment number of the source (DIS 3.25.0), may be null
   * @param payeeClass payee class (LOV {@code PAYEE_CLASS}), may be null
   * @param disbursementType disbursement type (LOV {@code DISBURSEMENT_TYPE}), may be null
   * @param attachmentRefs every supporting document; the main one is listed first
   * @param rootInvoiceNo root invoice of the family the payment belongs to (DIS 3.27.2), may be
   *     null
   * @param accountingRefs open-item or accounting event references the payment settles
   * @param straightToApproval the DV is built automatically and goes straight to the approver
   *     (refunds to clients and remittances to insurers, DIS 3.25.0)
   */
  public record Spec(
      Type type,
      String sourceModule,
      String sourceRef,
      String payeeCode,
      String payeeName,
      String currency,
      BigDecimal amount,
      String description,
      String attachmentRef,
      String rfpNo,
      String payeeClass,
      String disbursementType,
      List<String> attachmentRefs,
      String rootInvoiceNo,
      List<String> accountingRefs,
      boolean straightToApproval) {

    /** Defensive copies; the main document heads the document list. */
    public Spec {
      List<String> docs = new ArrayList<>();
      if (attachmentRef != null) {
        docs.add(attachmentRef);
      }
      if (attachmentRefs != null) {
        attachmentRefs.stream().filter(r -> !docs.contains(r)).forEach(docs::add);
      }
      attachmentRefs = List.copyOf(docs);
      accountingRefs = accountingRefs == null ? List.of() : List.copyOf(accountingRefs);
    }

    /**
     * A request of the Operations modules (BRD-2 contract, kept so their calls compile unchanged):
     * no RFP number, payee class, disbursement type, root invoice or accounting references, normal
     * routing.
     *
     * @param type request type
     * @param sourceModule source module
     * @param sourceRef source reference (unique per module)
     * @param payeeCode payee party code
     * @param payeeName payee name
     * @param currency currency
     * @param amount positive amount
     * @param description description
     * @param attachmentRef supporting document (attachment or extract id), may be null
     */
    public Spec(
        Type type,
        String sourceModule,
        String sourceRef,
        String payeeCode,
        String payeeName,
        String currency,
        BigDecimal amount,
        String description,
        String attachmentRef) {
      this(
          type,
          sourceModule,
          sourceRef,
          payeeCode,
          payeeName,
          currency,
          amount,
          description,
          attachmentRef,
          null,
          null,
          null,
          List.of(),
          null,
          List.of(),
          false);
    }

    /**
     * The same request with the Disbursement routing of BRD-5 (DIS 3.25.0, 3.27.2).
     *
     * @param rfp request-for-payment number
     * @param klass payee class (LOV {@code PAYEE_CLASS})
     * @param kind disbursement type (LOV {@code DISBURSEMENT_TYPE})
     * @param root root invoice number
     * @param straight straight to the approver
     * @return a new spec
     */
    public Spec routed(String rfp, String klass, String kind, String root, boolean straight) {
      return new Spec(
          type,
          sourceModule,
          sourceRef,
          payeeCode,
          payeeName,
          currency,
          amount,
          description,
          attachmentRef,
          rfp,
          klass,
          kind,
          attachmentRefs,
          root,
          accountingRefs,
          straight);
    }

    /**
     * The same request with its supporting documents and the accounting references it settles.
     *
     * @param documents supporting documents
     * @param settles open-item or accounting event references
     * @return a new spec
     */
    public Spec withReferences(List<String> documents, List<String> settles) {
      return new Spec(
          type,
          sourceModule,
          sourceRef,
          payeeCode,
          payeeName,
          currency,
          amount,
          description,
          attachmentRef,
          rfpNo,
          payeeClass,
          disbursementType,
          documents,
          rootInvoiceNo,
          settles,
          straightToApproval);
    }
  }
}
