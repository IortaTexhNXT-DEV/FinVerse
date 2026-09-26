package com.iortatechnxt.brokerverse.prodrecon.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import com.iortatechnxt.brokerverse.prodrecon.domain.ReconEnums.MatchMethod;
import com.iortatechnxt.brokerverse.prodrecon.domain.ReconEnums.ReconStatus;
import com.iortatechnxt.brokerverse.prodrecon.domain.ReconEnums.UnbookedStatus;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * One reconciliation item of a cycle (PRCID.012-030/033): a booked invoice (BDOI side), an insurer
 * production line (insurer side) or both once paired, with its status bucket, the fields that
 * differ beyond the tolerance, the pre-booked account found for insurer production, and the
 * feedback and disposition recorded by the reconciliation handler.
 */
@Entity
@Table(name = "prc_item")
public class ReconItem extends BaseEntity {

  private static final int MAX_DISCREPANCIES = 1000;

  @Column(name = "cycle_id", nullable = false, updatable = false)
  private Long cycleId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private ReconStatus status;

  @Column(name = "invoice_no", length = 40)
  private String invoiceNo;

  @Column(name = "extract_line_id")
  private Long extractLineId;

  @Column(length = 30)
  private String arn;

  @Column(name = "booking_date")
  private LocalDate bookingDate;

  @Column(name = "ao_username", length = 50)
  private String aoUsername;

  @Column(name = "sales_unit", length = 20)
  private String salesUnit;

  @Column(name = "branch_id")
  private Long branchId;

  @Column(length = 40)
  private String segment;

  @Column(name = "product_line", length = 30)
  private String productLine;

  @Embedded
  @AttributeOverride(name = "policyNo", column = @Column(name = "bdoi_policy_no"))
  @AttributeOverride(name = "referenceNo", column = @Column(name = "bdoi_reference_no"))
  @AttributeOverride(name = "pnNo", column = @Column(name = "bdoi_pn_no"))
  @AttributeOverride(name = "periodFrom", column = @Column(name = "bdoi_period_from"))
  @AttributeOverride(name = "periodTo", column = @Column(name = "bdoi_period_to"))
  @AttributeOverride(name = "assuredName", column = @Column(name = "bdoi_assured_name"))
  @AttributeOverride(name = "commission", column = @Column(name = "bdoi_commission"))
  @AttributeOverride(name = "basicPremium", column = @Column(name = "bdoi_basic_premium"))
  @AttributeOverride(name = "grossPremium", column = @Column(name = "bdoi_gross_premium"))
  private ReconSide bdoi;

  @Column(name = "upload_id")
  private Long uploadId;

  @Column(name = "ins_row_no")
  private Integer insRowNo;

  @Embedded
  @AttributeOverride(name = "policyNo", column = @Column(name = "ins_policy_no"))
  @AttributeOverride(name = "referenceNo", column = @Column(name = "ins_reference_no"))
  @AttributeOverride(name = "pnNo", column = @Column(name = "ins_pn_no"))
  @AttributeOverride(name = "periodFrom", column = @Column(name = "ins_period_from"))
  @AttributeOverride(name = "periodTo", column = @Column(name = "ins_period_to"))
  @AttributeOverride(name = "assuredName", column = @Column(name = "ins_assured_name"))
  @AttributeOverride(name = "commission", column = @Column(name = "ins_commission"))
  @AttributeOverride(name = "basicPremium", column = @Column(name = "ins_basic_premium"))
  @AttributeOverride(name = "grossPremium", column = @Column(name = "ins_gross_premium"))
  private ReconSide insurer;

  @Column(name = "ins_incentive", precision = 19, scale = 2)
  private BigDecimal insurerIncentive;

  @Column(name = "ins_remarks", length = 500)
  private String insurerRemarks;

  @Column(name = "in_original_extract", nullable = false)
  private boolean inOriginalExtract;

  @Column(name = "prebooked_arn", length = 30)
  private String prebookedArn;

  @Column(length = MAX_DISCREPANCIES)
  private String discrepancies;

  @Enumerated(EnumType.STRING)
  @Column(name = "match_method", length = 10)
  private MatchMethod matchMethod;

  @Column(name = "matched_at")
  private Instant matchedAt;

  @Column(name = "matched_by", length = 50)
  private String matchedBy;

  @Column(name = "company_concerned", length = 40)
  private String companyConcerned;

  @Column(length = 500)
  private String instruction;

  @Column(name = "insurer_feedback", length = 1000)
  private String insurerFeedback;

  @Column(name = "marketing_feedback", length = 1000)
  private String marketingFeedback;

  @Column(length = 40)
  private String disposition;

  @Column(name = "for_closure", nullable = false)
  private boolean forClosure;

  @Enumerated(EnumType.STRING)
  @Column(name = "unbooked_status", length = 20)
  private UnbookedStatus unbookedStatus;

  protected ReconItem() {}

  /**
   * A booked invoice of the cycle, not (yet) in the insurer's report.
   *
   * @param cycleId cycle
   * @param facts booked invoice facts
   * @return the item
   */
  public static ReconItem booked(Long cycleId, BdoiFacts facts) {
    ReconItem i = new ReconItem();
    i.cycleId = cycleId;
    i.attachBdoi(facts);
    i.status = ReconStatus.BDOI_ONLY;
    return i;
  }

  /**
   * Insurer production without a booked invoice yet.
   *
   * @param cycleId cycle
   * @param row insurer line
   * @return the item (status set by {@link #unbooked})
   */
  public static ReconItem insurerOnly(Long cycleId, InsurerRow row) {
    ReconItem i = new ReconItem();
    i.cycleId = cycleId;
    i.attachInsurer(row);
    i.status = ReconStatus.UNMATCHED_NO_BOOKING;
    i.unbookedStatus = UnbookedStatus.OPEN;
    return i;
  }

  /**
   * Sets the BDOI side.
   *
   * @param facts booked invoice facts
   */
  public final void attachBdoi(BdoiFacts facts) {
    this.invoiceNo = facts.invoiceNo();
    this.extractLineId = facts.extractLineId();
    this.arn = facts.arn();
    this.bookingDate = facts.bookingDate();
    this.aoUsername = facts.aoUsername();
    this.salesUnit = facts.salesUnit();
    this.branchId = facts.branchId();
    this.segment = facts.segment();
    this.productLine = facts.productLine();
    this.bdoi = facts.side();
    if (unbookedStatus != null) {
      this.unbookedStatus = UnbookedStatus.BOOKED;
    }
  }

  /**
   * Sets the insurer side.
   *
   * @param row insurer line
   */
  public final void attachInsurer(InsurerRow row) {
    this.uploadId = row.uploadId();
    this.insRowNo = row.rowNo();
    this.insurer = row.side();
    this.insurerIncentive = row.incentive();
    this.insurerRemarks = row.remarks();
    this.inOriginalExtract = row.inOriginalExtract();
  }

  /** Removes the insurer side (a new upload replaces it); the item is BDOI only again. */
  public void detachInsurer() {
    this.uploadId = null;
    this.insRowNo = null;
    this.insurer = null;
    this.insurerIncentive = null;
    this.insurerRemarks = null;
    this.inOriginalExtract = false;
    this.discrepancies = null;
    this.matchMethod = null;
    this.matchedAt = null;
    this.matchedBy = null;
    this.status = ReconStatus.BDOI_ONLY;
  }

  /**
   * Records the comparison of a paired item: matched when no field differs (PRCID.026/027).
   *
   * @param differing fields beyond the tolerance, empty when matched
   * @param method auto or manual
   * @param at time
   * @param by user or SYSTEM
   */
  public void paired(List<String> differing, MatchMethod method, Instant at, String by) {
    this.status = differing.isEmpty() ? ReconStatus.MATCHED : ReconStatus.MATCHED_WITH_DISCREPANCY;
    String text = String.join("; ", differing);
    this.discrepancies =
        text.isEmpty() ? null : text.substring(0, Math.min(text.length(), MAX_DISCREPANCIES));
    this.matchMethod = method;
    this.matchedAt = at;
    this.matchedBy = by;
  }

  /**
   * Classifies insurer production without a booked invoice (PRCID.023): pre-booked when an account
   * exists that is not booked yet.
   *
   * @param prebooked ARN of the pre-booked account, null when none
   */
  public void unbooked(String prebooked) {
    this.prebookedArn = prebooked;
    this.status =
        prebooked == null ? ReconStatus.UNMATCHED_NO_BOOKING : ReconStatus.UNMATCHED_PREBOOKED;
    if (unbookedStatus != UnbookedStatus.CLOSED) {
      this.unbookedStatus = prebooked == null ? UnbookedStatus.OPEN : UnbookedStatus.PREBOOKED;
    }
  }

  /**
   * Records the handler's feedback and disposition (PRCID.015/016/018/033).
   *
   * @param f feedback
   */
  public void feedback(Feedback f) {
    this.companyConcerned = f.companyConcerned();
    this.instruction = f.instruction();
    this.insurerFeedback = f.insurerFeedback();
    this.marketingFeedback = f.marketingFeedback();
    this.disposition = f.disposition();
    this.forClosure = f.forClosure();
    if (status.isInsurerOnly()) {
      this.unbookedStatus =
          f.forClosure()
              ? UnbookedStatus.CLOSED
              : prebookedArn == null ? UnbookedStatus.OPEN : UnbookedStatus.PREBOOKED;
    }
  }

  /**
   * Whether the item needs no more work (matched or flagged for closure).
   *
   * @return true when settled
   */
  public boolean isSettled() {
    return status == ReconStatus.MATCHED || forClosure;
  }

  /**
   * Whether both sides are present.
   *
   * @return true when paired
   */
  public boolean isPaired() {
    return invoiceNo != null && insurer != null;
  }

  /**
   * The current feedback of the item.
   *
   * @return feedback
   */
  public Feedback currentFeedback() {
    return new Feedback(
        companyConcerned, instruction, insurerFeedback, marketingFeedback, disposition, forClosure);
  }

  public Long getCycleId() {
    return cycleId;
  }

  public ReconStatus getStatus() {
    return status;
  }

  public String getInvoiceNo() {
    return invoiceNo;
  }

  public Long getExtractLineId() {
    return extractLineId;
  }

  public String getArn() {
    return arn;
  }

  public LocalDate getBookingDate() {
    return bookingDate;
  }

  public String getAoUsername() {
    return aoUsername;
  }

  public String getSalesUnit() {
    return salesUnit;
  }

  public Long getBranchId() {
    return branchId;
  }

  public String getSegment() {
    return segment;
  }

  public String getProductLine() {
    return productLine;
  }

  public ReconSide getBdoi() {
    return bdoi;
  }

  public Long getUploadId() {
    return uploadId;
  }

  public Integer getInsRowNo() {
    return insRowNo;
  }

  public ReconSide getInsurer() {
    return insurer;
  }

  public BigDecimal getInsurerIncentive() {
    return insurerIncentive;
  }

  public String getInsurerRemarks() {
    return insurerRemarks;
  }

  public boolean isInOriginalExtract() {
    return inOriginalExtract;
  }

  public String getPrebookedArn() {
    return prebookedArn;
  }

  public String getDiscrepancies() {
    return discrepancies;
  }

  public MatchMethod getMatchMethod() {
    return matchMethod;
  }

  public Instant getMatchedAt() {
    return matchedAt;
  }

  public String getMatchedBy() {
    return matchedBy;
  }

  public String getCompanyConcerned() {
    return companyConcerned;
  }

  public String getInstruction() {
    return instruction;
  }

  public String getInsurerFeedback() {
    return insurerFeedback;
  }

  public String getMarketingFeedback() {
    return marketingFeedback;
  }

  public String getDisposition() {
    return disposition;
  }

  public boolean isForClosure() {
    return forClosure;
  }

  public UnbookedStatus getUnbookedStatus() {
    return unbookedStatus;
  }

  /**
   * Facts of the booked invoice side.
   *
   * @param invoiceNo invoice number
   * @param extractLineId register line it was sent on, may be null
   * @param arn ARN
   * @param bookingDate booking date
   * @param aoUsername account officer (PRCID.014/037)
   * @param salesUnit sales unit / AB
   * @param branchId booking branch (location, PRCID.036)
   * @param segment market segment
   * @param productLine product line
   * @param side compared fields
   */
  public record BdoiFacts(
      String invoiceNo,
      Long extractLineId,
      String arn,
      LocalDate bookingDate,
      String aoUsername,
      String salesUnit,
      Long branchId,
      String segment,
      String productLine,
      ReconSide side) {}

  /**
   * A line of an insurer production report.
   *
   * @param uploadId upload attempt
   * @param rowNo row in the file
   * @param side compared fields
   * @param incentive early incentive the insurer reports, may be null (PRCID.028)
   * @param remarks insurer remarks
   * @param inOriginalExtract whether BDOI sent the line in its register
   */
  public record InsurerRow(
      Long uploadId,
      int rowNo,
      ReconSide side,
      BigDecimal incentive,
      String remarks,
      boolean inOriginalExtract) {}

  /**
   * Feedback and disposition.
   *
   * @param companyConcerned company concerned (LOV RECON_COMPANY_CONCERNED)
   * @param instruction instruction or notation
   * @param insurerFeedback insurer's feedback
   * @param marketingFeedback Marketing's feedback
   * @param disposition disposition (LOV RECON_DISPOSITION)
   * @param forClosure item confirmed for closure
   */
  public record Feedback(
      String companyConcerned,
      String instruction,
      String insurerFeedback,
      String marketingFeedback,
      String disposition,
      boolean forClosure) {}
}
