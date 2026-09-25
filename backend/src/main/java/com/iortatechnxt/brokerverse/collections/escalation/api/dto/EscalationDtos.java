package com.iortatechnxt.brokerverse.collections.escalation.api.dto;

import com.iortatechnxt.brokerverse.collections.escalation.domain.Escalation;
import com.iortatechnxt.brokerverse.collections.escalation.domain.EscalationEnums.Basis;
import com.iortatechnxt.brokerverse.collections.escalation.domain.EscalationEnums.Kind;
import com.iortatechnxt.brokerverse.collections.escalation.domain.EscalationEnums.Stage;
import com.iortatechnxt.brokerverse.collections.escalation.domain.EscalationEnums.TargetLevel;
import com.iortatechnxt.brokerverse.collections.escalation.domain.EscalationItem;
import com.iortatechnxt.brokerverse.collections.escalation.domain.EscalationRule;
import com.iortatechnxt.brokerverse.collections.escalation.service.EscalationCandidates.Candidate;
import com.iortatechnxt.brokerverse.common.domain.RecordStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Requests and responses of the escalation endpoints (BRCLXN.049/050). */
@SuppressWarnings("PMD.MissingStaticMethodInNonInstantiatableClass") // holder of nested types
public final class EscalationDtos {

  private static final int MAX_SLA_HOURS = 720;

  private EscalationDtos() {}

  /**
   * An escalation.
   *
   * @param id id
   * @param escalationNo number
   * @param kind automatic or manual
   * @param ruleCode rule
   * @param arn account
   * @param clientCode client
   * @param assuredName assured
   * @param targetLevel receiving level
   * @param targetUsername designated user
   * @param reasonCode reason
   * @param remarks remarks
   * @param totalBalance outstanding when escalated
   * @param currency currency
   * @param slaHours SLA
   * @param status stage
   * @param stageSince in the stage since
   * @param overdue past its SLA
   * @param bulkRef bulk reference
   * @param resolvedAt resolution time
   * @param resolution resolution
   * @param raisedBy raiser (SYSTEM for the job)
   * @param createdAt raised
   * @param items invoices (detail only; empty in lists)
   */
  public record EscalationResponse(
      Long id,
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
      BigDecimal totalBalance,
      String currency,
      int slaHours,
      Stage status,
      Instant stageSince,
      boolean overdue,
      String bulkRef,
      Instant resolvedAt,
      String resolution,
      String raisedBy,
      Instant createdAt,
      List<ItemResponse> items) {

    /**
     * Maps an escalation without its invoices.
     *
     * @param e escalation
     * @param now time (SLA)
     * @return DTO
     */
    public static EscalationResponse from(Escalation e, Instant now) {
      return of(e, now, List.of());
    }

    /**
     * Maps an escalation with its invoices (loaded).
     *
     * @param e escalation
     * @param now time (SLA)
     * @return DTO
     */
    public static EscalationResponse detail(Escalation e, Instant now) {
      return of(e, now, e.getItems().stream().map(ItemResponse::from).toList());
    }

    private static EscalationResponse of(Escalation e, Instant now, List<ItemResponse> items) {
      return new EscalationResponse(
          e.getId(),
          e.getEscalationNo(),
          e.getKind(),
          e.getRuleCode(),
          e.getArn(),
          e.getClientCode(),
          e.getAssuredName(),
          e.getTargetLevel(),
          e.getTargetUsername(),
          e.getReasonCode(),
          e.getRemarks(),
          e.getTotalBalance(),
          e.getCurrency(),
          e.getSlaHours(),
          e.getStatus(),
          e.getStageSince(),
          e.isOverdue(now),
          e.getBulkRef(),
          e.getResolvedAt(),
          e.getResolution(),
          e.getCreatedBy(),
          e.getCreatedAt(),
          items);
    }
  }

  /**
   * An escalated invoice.
   *
   * @param invoiceNo invoice
   * @param policyNo policy number
   * @param balance outstanding when escalated
   * @param agingDays days since booking when escalated
   */
  public record ItemResponse(String invoiceNo, String policyNo, BigDecimal balance, int agingDays) {

    /**
     * Maps an item.
     *
     * @param i item
     * @return DTO
     */
    public static ItemResponse from(EscalationItem i) {
      return new ItemResponse(i.getInvoiceNo(), i.getPolicyNo(), i.getBalance(), i.getAgingDays());
    }
  }

  /**
   * A business action on an escalation.
   *
   * @param reasonCode reason (escalate further)
   * @param comment comment (the resolution when resolving)
   */
  public record ActionRequest(
      @Size(max = 40) String reasonCode, @Size(max = 1000) String comment) {}

  /**
   * An escalation rule.
   *
   * @param id id
   * @param code code
   * @param name name
   * @param basis basis
   * @param threshold threshold
   * @param segment segment filter
   * @param salesUnit sales unit filter
   * @param productLine product line filter
   * @param amountFrom lowest outstanding
   * @param amountTo highest outstanding
   * @param targetLevel receiving level
   * @param targetUsername designated user
   * @param reasonCode reason
   * @param slaHours SLA
   * @param notifyTarget notify
   * @param effectiveFrom first day
   * @param effectiveTo last day
   * @param recordStatus maker-checker status
   * @param maker last maintained by
   * @param authorizedBy checker
   */
  public record RuleResponse(
      Long id,
      String code,
      String name,
      Basis basis,
      BigDecimal threshold,
      String segment,
      String salesUnit,
      String productLine,
      BigDecimal amountFrom,
      BigDecimal amountTo,
      TargetLevel targetLevel,
      String targetUsername,
      String reasonCode,
      int slaHours,
      boolean notifyTarget,
      LocalDate effectiveFrom,
      LocalDate effectiveTo,
      RecordStatus recordStatus,
      String maker,
      String authorizedBy) {

    /**
     * Maps a rule.
     *
     * @param r rule
     * @return DTO
     */
    public static RuleResponse from(EscalationRule r) {
      EscalationRule.Filters f = r.filters();
      return new RuleResponse(
          r.getId(),
          r.getCode(),
          r.getName(),
          r.getBasis(),
          r.getThreshold(),
          f.segment(),
          f.salesUnit(),
          f.productLine(),
          f.amountFrom(),
          f.amountTo(),
          r.getTargetLevel(),
          r.getTargetUsername(),
          r.getReasonCode(),
          r.getSlaHours(),
          r.isNotify(),
          r.getEffectiveFrom(),
          r.getEffectiveTo(),
          r.getRecordStatus(),
          r.getMaker(),
          r.getAuthorizedBy());
    }
  }

  /**
   * A new or changed rule.
   *
   * @param companyId company (new rules)
   * @param code code (new rules)
   * @param name name
   * @param basis basis
   * @param threshold threshold
   * @param segment segment filter
   * @param salesUnit sales unit filter
   * @param productLine product line filter
   * @param amountFrom lowest outstanding
   * @param amountTo highest outstanding
   * @param targetLevel receiving level
   * @param targetUsername designated user
   * @param reasonCode reason
   * @param slaHours SLA
   * @param notifyTarget notify
   * @param effectiveFrom first day
   * @param effectiveTo last day
   */
  public record RuleRequest(
      Long companyId,
      @Size(max = 40) String code,
      @NotBlank @Size(max = 200) String name,
      @NotNull Basis basis,
      @NotNull @Positive BigDecimal threshold,
      @Size(max = 40) String segment,
      @Size(max = 20) String salesUnit,
      @Size(max = 30) String productLine,
      BigDecimal amountFrom,
      BigDecimal amountTo,
      @NotNull TargetLevel targetLevel,
      @Size(max = 50) String targetUsername,
      @NotBlank @Size(max = 40) String reasonCode,
      @Positive @Max(MAX_SLA_HOURS) int slaHours,
      boolean notifyTarget,
      @NotNull LocalDate effectiveFrom,
      LocalDate effectiveTo) {

    /**
     * The service terms (blank filters mean "any").
     *
     * @return terms
     */
    public EscalationRule.Terms toTerms() {
      return new EscalationRule.Terms(
          name.strip(),
          basis,
          threshold,
          new EscalationRule.Filters(
              blank(segment), blank(salesUnit), blank(productLine), amountFrom, amountTo),
          targetLevel,
          blank(targetUsername),
          reasonCode,
          slaHours,
          notifyTarget,
          effectiveFrom,
          effectiveTo);
    }

    private static String blank(String text) {
      return text == null || text.isBlank() ? null : text.strip();
    }
  }

  /**
   * An account a rule would escalate (preview).
   *
   * @param invoiceNo invoice
   * @param arn account
   * @param assuredName assured
   * @param bookingDate booking date
   * @param inceptionDate inception date
   * @param balance outstanding
   * @param currency currency
   */
  public record MatchResponse(
      String invoiceNo,
      String arn,
      String assuredName,
      LocalDate bookingDate,
      LocalDate inceptionDate,
      BigDecimal balance,
      String currency) {

    /**
     * Maps a candidate.
     *
     * @param c candidate
     * @return DTO
     */
    public static MatchResponse from(Candidate c) {
      return new MatchResponse(
          c.invoiceNo(),
          c.arn(),
          c.assuredName(),
          c.dates().booking(),
          c.dates().inception(),
          c.balance(),
          c.currency());
    }
  }
}
