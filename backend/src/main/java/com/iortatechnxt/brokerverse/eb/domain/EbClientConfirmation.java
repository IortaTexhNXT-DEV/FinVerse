package com.iortatechnxt.brokerverse.eb.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The client's confirmation of the chosen proposals (BRID-017; FR-EB-046): channel (e-mail or
 * signed document; no portal), date and the evidence, and per programme line the chosen proposal
 * and insurer, later the account created at placement. A confirmation can be voided before the
 * placement is triggered and recorded again.
 */
@Entity
@Table(name = "eb_client_confirmation")
public class EbClientConfirmation extends EbCycleRecord {

  /** Active confirmation. */
  public static final String ACTIVE = "ACTIVE";

  /** Voided confirmation. */
  public static final String VOIDED = "VOIDED";

  @Column(name = "comparative_id", updatable = false)
  private Long comparativeId;

  @Column(nullable = false, length = 20, updatable = false)
  private String channel;

  @Column(name = "confirmed_on", nullable = false, updatable = false)
  private LocalDate confirmedOn;

  @Column(name = "evidence_attachment_id", nullable = false, updatable = false)
  private Long evidenceAttachmentId;

  @Column(nullable = false, length = 20)
  private String status = ACTIVE;

  @Column(name = "void_reason", length = 500)
  private String voidReason;

  @Column(length = 1000)
  private String remarks;

  @OneToMany(mappedBy = "confirmation", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("lineNo")
  private final List<Line> lines = new ArrayList<>();

  protected EbClientConfirmation() {}

  /**
   * Records a confirmation.
   *
   * @param cycle cycle
   * @param comparativeId comparative presented, may be null
   * @param evidence channel, date, evidence and remarks
   */
  public EbClientConfirmation(EbCycle cycle, Long comparativeId, Evidence evidence) {
    super(cycle);
    this.comparativeId = comparativeId;
    this.channel = evidence.channel();
    this.confirmedOn = evidence.confirmedOn();
    this.evidenceAttachmentId = evidence.attachmentId();
    this.remarks = evidence.remarks();
  }

  /**
   * Adds the chosen proposal of a programme line.
   *
   * @param choice line, proposal, insurer, premium and TSI
   * @return the line
   */
  public Line addLine(Choice choice) {
    Line line = new Line(this, choice);
    lines.add(line);
    return line;
  }

  /**
   * Voids the confirmation before the placement.
   *
   * @param reason why
   */
  public void voidWith(String reason) {
    if (reason == null || reason.isBlank()) {
      throw new BusinessRuleException(
          "EB_CONFIRMATION_REASON_REQUIRED", "Enter why the confirmation is voided");
    }
    this.status = VOIDED;
    this.voidReason = reason.strip();
  }

  public Long getComparativeId() {
    return comparativeId;
  }

  public String getChannel() {
    return channel;
  }

  public LocalDate getConfirmedOn() {
    return confirmedOn;
  }

  public Long getEvidenceAttachmentId() {
    return evidenceAttachmentId;
  }

  public String getStatus() {
    return status;
  }

  public String getVoidReason() {
    return voidReason;
  }

  public String getRemarks() {
    return remarks;
  }

  public List<Line> getLines() {
    return Collections.unmodifiableList(lines);
  }

  /**
   * How the client confirmed.
   *
   * @param channel EMAIL or SIGNED_DOCUMENT
   * @param confirmedOn date of the confirmation
   * @param attachmentId the stored evidence
   * @param remarks remarks, may be null
   */
  public record Evidence(
      String channel, LocalDate confirmedOn, Long attachmentId, String remarks) {}

  /**
   * The chosen proposal of a programme line.
   *
   * @param lineNo programme line
   * @param benefitLine benefit line
   * @param proposalId chosen validated proposal
   * @param insurerCode its insurer
   * @param annualPremium annual premium of the line
   * @param sumInsured TSI of the line, may be null
   */
  public record Choice(
      int lineNo,
      String benefitLine,
      Long proposalId,
      String insurerCode,
      BigDecimal annualPremium,
      BigDecimal sumInsured) {}

  /** A programme line of the confirmation. */
  @Entity(name = "EbConfirmationLine")
  @Table(name = "eb_confirmation_line")
  public static class Line extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "confirmation_id", nullable = false, updatable = false)
    private EbClientConfirmation confirmation;

    @Column(name = "line_no", nullable = false)
    private int lineNo;

    @Column(name = "benefit_line", nullable = false, length = 30)
    private String benefitLine;

    @Column(name = "proposal_id", nullable = false)
    private Long proposalId;

    @Column(name = "insurer_code", nullable = false, length = 30)
    private String insurerCode;

    @Column(name = "annual_premium", nullable = false, precision = 19, scale = 2)
    private BigDecimal annualPremium;

    @Column(name = "sum_insured", precision = 19, scale = 2)
    private BigDecimal sumInsured;

    @Column(name = "account_arn", length = 30)
    private String accountArn;

    protected Line() {}

    Line(EbClientConfirmation confirmation, Choice choice) {
      this.confirmation = confirmation;
      this.lineNo = choice.lineNo();
      this.benefitLine = choice.benefitLine();
      this.proposalId = choice.proposalId();
      this.insurerCode = choice.insurerCode();
      this.annualPremium = choice.annualPremium();
      this.sumInsured = choice.sumInsured();
    }

    /**
     * Keeps the account created for the line.
     *
     * @param arn account
     */
    public void placedAs(String arn) {
      this.accountArn = arn;
    }

    public int getLineNo() {
      return lineNo;
    }

    public String getBenefitLine() {
      return benefitLine;
    }

    public Long getProposalId() {
      return proposalId;
    }

    public String getInsurerCode() {
      return insurerCode;
    }

    public BigDecimal getAnnualPremium() {
      return annualPremium;
    }

    public BigDecimal getSumInsured() {
      return sumInsured;
    }

    public String getAccountArn() {
      return accountArn;
    }
  }
}
