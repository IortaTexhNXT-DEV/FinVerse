package com.iortatechnxt.brokerverse.eb.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
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
import java.util.Collections;
import java.util.List;

/**
 * A member change of a programme line (BRID-013, 025; FR-EB-055, 056), numbered {@code
 * EBM-<yyyy>-nnnnnn}, with its {@code EB_MEMBER_CHANGE} work case whose stage it mirrors: lines
 * adding, deleting or changing members, relayed to the insurer, billed (insurer billing or direct
 * billing), validated by Processing (raising the endorsement request of a change with a premium
 * effect) and closed, when its lines are applied to the roster.
 */
@Entity
@Table(name = "eb_member_change")
public class EbMemberChange extends BaseEntity {

  /** Stage of a member change (mirror of {@code EB_MEMBER_CHANGE}). */
  public enum Status {
    /** Captured by the AO. */
    CAPTURED,
    /** Sent to the insurer. */
    RELAYED,
    /** Billing received. */
    BILLED,
    /** Validated by Processing. */
    VALIDATED,
    /** Applied to the roster. */
    CLOSED,
    /** Cancelled. */
    CANCELLED
  }

  /** Action of a line (list EB_MEMBER_CHANGE_TYPE). */
  public enum Action {
    /** New member. */
    ADD,
    /** Member leaves. */
    DELETE,
    /** Member moves to another plan. */
    CHANGE_PLAN,
    /** Member data corrected. */
    CHANGE_DATA
  }

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "change_no", nullable = false, length = 30, updatable = false)
  private String changeNo;

  @Column(name = "programme_id", nullable = false, updatable = false)
  private Long programmeId;

  @Column(name = "line_no", nullable = false, updatable = false)
  private int lineNo;

  @Column(name = "benefit_line", nullable = false, length = 30, updatable = false)
  private String benefitLine;

  @Column(name = "policy_year", nullable = false, updatable = false)
  private int policyYear;

  @Column(nullable = false, length = 10, updatable = false)
  private String source;

  @Column(nullable = false)
  private boolean financial;

  @Column(name = "direct_billed", nullable = false)
  private boolean directBilled;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Status status = Status.CAPTURED;

  @Column(length = 1000)
  private String description;

  @Column(name = "relayed_at")
  private Instant relayedAt;

  @Column(name = "message_id")
  private Long messageId;

  @Column(name = "billed_on")
  private LocalDate billedOn;

  @Column(name = "billing_ref", length = 60)
  private String billingRef;

  @Column(name = "billed_amount", precision = 19, scale = 2)
  private BigDecimal billedAmount;

  @Column(name = "validated_at")
  private Instant validatedAt;

  @Column(name = "validated_by", length = 50)
  private String validatedBy;

  @Column(name = "endorsement_request_id")
  private Long endorsementRequestId;

  @Column(name = "endorsement_request_no", length = 40)
  private String endorsementRequestNo;

  @Column(name = "closed_at")
  private Instant closedAt;

  @OneToMany(mappedBy = "change", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("sortOrder")
  private final List<Line> lines = new ArrayList<>();

  protected EbMemberChange() {}

  /**
   * Captures a member change.
   *
   * @param programme programme
   * @param changeNo number
   * @param line programme line
   * @param header policy year, source, financial flag and description
   */
  public EbMemberChange(
      EbProgramme programme, String changeNo, EbProgrammeLine line, Header header) {
    this.companyId = programme.getCompanyId();
    this.programmeId = programme.getId();
    this.changeNo = changeNo;
    this.lineNo = line.getLineNo();
    this.benefitLine = line.getBenefitLine();
    this.policyYear = header.policyYear();
    this.source = header.source();
    this.financial = header.financial();
    this.description = header.description();
  }

  /**
   * Adds a line.
   *
   * @param data line data
   * @return the line
   */
  public Line addLine(LineData data) {
    Line line = new Line(this, lines.size() + 1, data);
    lines.add(line);
    return line;
  }

  /**
   * Records the relay to the insurer.
   *
   * @param at time
   * @param message outbox message
   */
  public void relayed(Instant at, Long message) {
    this.relayedAt = at;
    this.messageId = message;
  }

  /**
   * Records the insurer's billing.
   *
   * @param billing date, reference, amount and whether the insurer bills the client directly
   */
  public void billed(Billing billing) {
    if (status != Status.RELAYED) {
      throw new BusinessRuleException(
          "EB_MEMBER_CHANGE_NOT_RELAYED", "Member change " + changeNo + " is not relayed");
    }
    this.billedOn = billing.billedOn();
    this.billingRef = billing.reference();
    this.billedAmount = billing.amount();
    this.directBilled = billing.direct();
  }

  /**
   * Records the validation by Processing.
   *
   * @param by user
   * @param at time
   */
  public void validated(String by, Instant at) {
    this.validatedBy = by;
    this.validatedAt = at;
  }

  /**
   * Keeps the endorsement request raised in Operations.
   *
   * @param id request id
   * @param number request number
   */
  public void endorsementRaised(Long id, String number) {
    this.endorsementRequestId = id;
    this.endorsementRequestNo = number;
  }

  /**
   * Mirrors the stage of the work case.
   *
   * @param stage new stage
   * @param at time
   */
  public void mirror(Status stage, Instant at) {
    this.status = stage;
    if (stage == Status.CLOSED || stage == Status.CANCELLED) {
      this.closedAt = at;
    }
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getChangeNo() {
    return changeNo;
  }

  public Long getProgrammeId() {
    return programmeId;
  }

  public int getLineNo() {
    return lineNo;
  }

  public String getBenefitLine() {
    return benefitLine;
  }

  public int getPolicyYear() {
    return policyYear;
  }

  public String getSource() {
    return source;
  }

  public boolean isFinancial() {
    return financial;
  }

  public boolean isDirectBilled() {
    return directBilled;
  }

  public Status getStatus() {
    return status;
  }

  public String getDescription() {
    return description;
  }

  public Instant getRelayedAt() {
    return relayedAt;
  }

  public Long getMessageId() {
    return messageId;
  }

  public LocalDate getBilledOn() {
    return billedOn;
  }

  public String getBillingRef() {
    return billingRef;
  }

  public BigDecimal getBilledAmount() {
    return billedAmount;
  }

  public Instant getValidatedAt() {
    return validatedAt;
  }

  public String getValidatedBy() {
    return validatedBy;
  }

  public Long getEndorsementRequestId() {
    return endorsementRequestId;
  }

  public String getEndorsementRequestNo() {
    return endorsementRequestNo;
  }

  public Instant getClosedAt() {
    return closedAt;
  }

  public List<Line> getLines() {
    return Collections.unmodifiableList(lines);
  }

  /**
   * Header of a member change.
   *
   * @param policyYear policy year of the roster
   * @param source AO or CLIENT (the client's request entered by the AO)
   * @param financial whether it has a premium effect
   * @param description description, may be null
   */
  public record Header(int policyYear, String source, boolean financial, String description) {}

  /**
   * The insurer's billing of a member change.
   *
   * @param billedOn billing date
   * @param reference insurer billing reference
   * @param amount amount billed (signed), may be null
   * @param direct whether the insurer bills the client directly
   */
  public record Billing(LocalDate billedOn, String reference, BigDecimal amount, boolean direct) {}

  /**
   * Data of a line.
   *
   * @param action action
   * @param employeeNo employee number
   * @param member member data (ADD, CHANGE_DATA; plan for CHANGE_PLAN), may be null for DELETE
   * @param effectiveDate effective date
   * @param memberId roster member (DELETE, CHANGE_*), null for ADD
   */
  public record LineData(
      Action action,
      String employeeNo,
      EbMember.Data member,
      LocalDate effectiveDate,
      Long memberId) {}

  /** A line of a member change. */
  @Entity(name = "EbMemberChangeLine")
  @Table(name = "eb_member_change_line")
  public static class Line extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_change_id", nullable = false, updatable = false)
    private EbMemberChange change;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Action action;

    @Column(name = "employee_no", nullable = false, length = 30)
    private String employeeNo;

    @Column(name = "member_id")
    private Long memberId;

    @Column(name = "last_name", length = 100)
    private String lastName;

    @Column(name = "first_name", length = 100)
    private String firstName;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(length = 10)
    private String gender;

    @Column(name = "civil_status", length = 20)
    private String civilStatus;

    @Column(name = "plan_code", length = 30)
    private String planCode;

    @Column private Integer dependants;

    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;

    protected Line() {}

    Line(EbMemberChange change, int sortOrder, LineData data) {
      this.change = change;
      this.sortOrder = sortOrder;
      this.action = data.action();
      this.employeeNo = data.employeeNo();
      this.memberId = data.memberId();
      this.effectiveDate = data.effectiveDate();
      EbMember.Data m = data.member();
      if (m != null) {
        this.lastName = m.lastName();
        this.firstName = m.firstName();
        this.birthDate = m.birthDate();
        this.gender = m.gender();
        this.civilStatus = m.civilStatus();
        this.planCode = m.planCode();
        this.dependants = m.dependants();
      }
    }

    /**
     * Keeps the roster member created by an addition.
     *
     * @param id member
     */
    public void member(Long id) {
      this.memberId = id;
    }

    /**
     * The member data of the line.
     *
     * @return data
     */
    public EbMember.Data memberData() {
      return new EbMember.Data(
          lastName, firstName, birthDate, gender, civilStatus, planCode, dependants);
    }

    public EbMemberChange getChange() {
      return change;
    }

    public int getSortOrder() {
      return sortOrder;
    }

    public Action getAction() {
      return action;
    }

    public String getEmployeeNo() {
      return employeeNo;
    }

    public Long getMemberId() {
      return memberId;
    }

    public String getLastName() {
      return lastName;
    }

    public String getFirstName() {
      return firstName;
    }

    public LocalDate getBirthDate() {
      return birthDate;
    }

    public String getGender() {
      return gender;
    }

    public String getCivilStatus() {
      return civilStatus;
    }

    public String getPlanCode() {
      return planCode;
    }

    public Integer getDependants() {
      return dependants;
    }

    public LocalDate getEffectiveDate() {
      return effectiveDate;
    }
  }
}
