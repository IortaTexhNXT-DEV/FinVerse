package com.iortatechnxt.brokerverse.collections.escalation.domain;

import com.iortatechnxt.brokerverse.collections.escalation.domain.EscalationEnums.Kind;
import com.iortatechnxt.brokerverse.collections.escalation.domain.EscalationEnums.Stage;
import com.iortatechnxt.brokerverse.collections.escalation.domain.EscalationEnums.TargetLevel;
import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.Hibernate;

/**
 * An escalation of a collection account (BRCLXN.049/050): raised automatically by a rule or
 * manually for one or several invoices of an account, routed to the team lead or the unit / section
 * head, and worked in the workflow {@code CLX_ESCALATION}, whose stage it mirrors.
 */
@Entity
@Table(name = "clx_escalation")
public class Escalation extends BaseEntity {

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "escalation_no", nullable = false, length = 40, updatable = false)
  private String escalationNo;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 10, updatable = false)
  private Kind kind;

  @Column(name = "rule_code", length = 40, updatable = false)
  private String ruleCode;

  @Column(nullable = false, length = 30, updatable = false)
  private String arn;

  @Column(name = "client_code", nullable = false, length = 30, updatable = false)
  private String clientCode;

  @Column(name = "assured_name", nullable = false, length = 250, updatable = false)
  private String assuredName;

  @Enumerated(EnumType.STRING)
  @Column(name = "target_level", nullable = false, length = 20)
  private TargetLevel targetLevel;

  @Column(name = "target_username", length = 50)
  private String targetUsername;

  @Column(name = "reason_code", nullable = false, length = 40, updatable = false)
  private String reasonCode;

  @Column(length = 1000, updatable = false)
  private String remarks;

  @Column(name = "total_balance", nullable = false, precision = 19, scale = 2, updatable = false)
  private BigDecimal totalBalance;

  @Column(nullable = false, length = 3, updatable = false)
  private String currency;

  @Column(name = "sla_hours", nullable = false, updatable = false)
  private int slaHours;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Stage status = Stage.RAISED;

  @Column(name = "stage_since", nullable = false)
  private Instant stageSince;

  @Column(name = "dedup_key", length = 120, updatable = false)
  private String dedupKey;

  @Column(name = "bulk_ref", length = 40, updatable = false)
  private String bulkRef;

  @Column(name = "resolved_at")
  private Instant resolvedAt;

  @Column(length = 1000)
  private String resolution;

  @OneToMany(mappedBy = "escalation", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("id")
  private final List<EscalationItem> items = new ArrayList<>();

  protected Escalation() {}

  /**
   * Raises an escalation.
   *
   * @param header account, origin and target
   * @param invoices the escalated invoices (at least one)
   * @param now time
   * @return the escalation
   */
  public static Escalation raise(Header header, List<EscalationItem.Facts> invoices, Instant now) {
    Escalation e = new Escalation();
    e.companyId = header.companyId();
    e.escalationNo = header.escalationNo();
    e.kind = header.kind();
    e.ruleCode = header.ruleCode();
    e.arn = header.arn();
    e.clientCode = header.clientCode();
    e.assuredName = header.assuredName();
    e.targetLevel = header.targetLevel();
    e.targetUsername = header.targetUsername();
    e.reasonCode = header.reasonCode();
    e.remarks = header.remarks();
    e.currency = header.currency();
    e.slaHours = header.slaHours();
    e.dedupKey = header.dedupKey();
    e.bulkRef = header.bulkRef();
    e.stageSince = now;
    BigDecimal sum = BigDecimal.ZERO;
    for (EscalationItem.Facts f : invoices) {
      e.items.add(new EscalationItem(e, f));
      sum = sum.add(f.balance());
    }
    e.totalBalance = sum;
    return e;
  }

  /**
   * Mirrors a stage change of the workflow.
   *
   * @param stage new stage
   * @param comment comment of the action (the resolution when resolved)
   * @param now time
   */
  public void markStage(Stage stage, String comment, Instant now) {
    this.status = stage;
    this.stageSince = now;
    if (stage == Stage.WITH_UH && !targetLevel.isHead()) {
      this.targetLevel = TargetLevel.UH;
      this.targetUsername = null;
    }
    if (stage == Stage.RESOLVED) {
      this.resolvedAt = now;
      this.resolution = comment;
    }
  }

  /**
   * Whether the escalation is past the SLA of its rule in its current stage.
   *
   * @param now time
   * @return true when overdue
   */
  public boolean isOverdue(Instant now) {
    return status.isOpen()
        && status != Stage.RAISED
        && stageSince.plus(Duration.ofHours(slaHours)).isBefore(now);
  }

  /** Loads the invoices (reads outside the persistence context). */
  public void loadItems() {
    Hibernate.initialize(items);
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getEscalationNo() {
    return escalationNo;
  }

  public Kind getKind() {
    return kind;
  }

  public String getRuleCode() {
    return ruleCode;
  }

  public String getArn() {
    return arn;
  }

  public String getClientCode() {
    return clientCode;
  }

  public String getAssuredName() {
    return assuredName;
  }

  public TargetLevel getTargetLevel() {
    return targetLevel;
  }

  public String getTargetUsername() {
    return targetUsername;
  }

  public String getReasonCode() {
    return reasonCode;
  }

  public String getRemarks() {
    return remarks;
  }

  public BigDecimal getTotalBalance() {
    return totalBalance;
  }

  public String getCurrency() {
    return currency;
  }

  public int getSlaHours() {
    return slaHours;
  }

  public Stage getStatus() {
    return status;
  }

  public Instant getStageSince() {
    return stageSince;
  }

  public String getDedupKey() {
    return dedupKey;
  }

  public String getBulkRef() {
    return bulkRef;
  }

  public Instant getResolvedAt() {
    return resolvedAt;
  }

  public String getResolution() {
    return resolution;
  }

  public List<EscalationItem> getItems() {
    return items;
  }

  /**
   * The facts of an escalation.
   *
   * @param companyId company
   * @param escalationNo number
   * @param kind automatic or manual
   * @param ruleCode rule, null for a manual escalation
   * @param arn account
   * @param clientCode client
   * @param assuredName assured
   * @param targetLevel receiving level
   * @param targetUsername designated user, may be null
   * @param reasonCode reason (LOV CLX_ESCALATION_REASON)
   * @param remarks remarks
   * @param currency currency
   * @param slaHours hours to act
   * @param dedupKey idempotency key of an automatic escalation, may be null
   * @param bulkRef bulk reference, may be null
   */
  public record Header(
      Long companyId,
      String escalationNo,
      Kind kind,
      String ruleCode,
      String arn,
      String clientCode,
      String assuredName,
      TargetLevel targetLevel,
      String targetUsername,
      String reasonCode,
      String remarks,
      String currency,
      int slaHours,
      String dedupKey,
      String bulkRef) {}
}
