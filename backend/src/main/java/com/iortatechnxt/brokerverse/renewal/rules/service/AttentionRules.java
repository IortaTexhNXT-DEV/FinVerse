package com.iortatechnxt.brokerverse.renewal.rules.service;

import com.iortatechnxt.brokerverse.common.util.DisplayFormat;
import com.iortatechnxt.brokerverse.renewal.domain.AttentionFlag;
import com.iortatechnxt.brokerverse.renewal.domain.Bucket;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalStage;
import com.iortatechnxt.brokerverse.renewal.service.RenewalParameters;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * The attention flags of the renewal listing (Annex BRRN.036; FR-RN-102): overdue (past the
 * effective expiry date and not accepted), high risk (Exception bucket or open claims) and ageing
 * (not accepted within the escalation days of the segment before the effective expiry), each with
 * the rule that set it and the priority portfolio (IBG, Leasing) named. Escalation is by visibility
 * only: the flags raise no alert, the Account Officer escalates outside the system.
 */
@Component
public class AttentionRules {

  /** Stages of a renewal not yet accepted. */
  static final Set<RenewalStage> BEFORE_ACCEPTANCE =
      EnumSet.of(
          RenewalStage.UNASSIGNED,
          RenewalStage.FOR_DISPOSITION,
          RenewalStage.TRANSFER_PENDING,
          RenewalStage.FOR_TL_REVIEW,
          RenewalStage.FOR_PROCESSING,
          RenewalStage.IN_PROCESSING,
          RenewalStage.WITH_INSURER,
          RenewalStage.RA_READY,
          RenewalStage.RA_GENERATED,
          RenewalStage.RA_SENT);

  private final RenewalParameters parameters;

  /**
   * Creates the rules.
   *
   * @param parameters escalation days and priority segments
   */
  public AttentionRules(RenewalParameters parameters) {
    this.parameters = parameters;
  }

  /**
   * Sets or clears the attention flag of a renewal.
   *
   * @param c renewal
   * @param today business date
   */
  public void apply(RenewalCandidate c, LocalDate today) {
    Attention a = evaluate(c, today);
    c.getAttention().set(a == null ? null : a.flag(), a == null ? null : a.rule(), today);
  }

  /**
   * The attention a renewal needs.
   *
   * @param c renewal
   * @param today business date
   * @return flag and rule, null when none
   */
  public Attention evaluate(RenewalCandidate c, LocalDate today) {
    if (!BEFORE_ACCEPTANCE.contains(c.getStage())) {
      return null;
    }
    String segment = c.getSnapshot().product() == null ? null : c.getSnapshot().product().segment();
    String priority = priority(segment);
    LocalDate expiry = c.effectiveExpiry();
    long days = ChronoUnit.DAYS.between(today, expiry);
    if (days < 0) {
      return new Attention(
          AttentionFlag.OVERDUE,
          "Past its effective expiry date of " + DisplayFormat.date(expiry) + priority);
    }
    if (c.getBucket() == Bucket.EXCEPTION || c.getFlags().isClaims()) {
      return new Attention(
          AttentionFlag.HIGH_RISK,
          (c.getBucket() == Bucket.EXCEPTION
                  ? "In the Exception bucket"
                  : "Claims on the expiring term")
              + priority);
    }
    return ageing(segment, days, priority);
  }

  /** AGEING when the renewal is within the escalation days of its segment, else none. */
  private Attention ageing(String segment, long days, String priority) {
    int limit = parameters.escalationDays(segment);
    return days <= limit
        ? new Attention(
            AttentionFlag.AGEING,
            "Not accepted " + days + " day(s) before expiry (limit " + limit + " days)" + priority)
        : null;
  }

  private String priority(String segment) {
    if (segment == null) {
      return "";
    }
    String code = segment.strip();
    boolean listed = parameters.prioritySegments().stream().anyMatch(s -> s.strip().equals(code));
    return listed ? "; priority portfolio " + code : "";
  }

  /**
   * An attention flag with its rule.
   *
   * @param flag flag
   * @param rule rule that set it, as the listing shows it
   */
  public record Attention(AttentionFlag flag, String rule) {}
}
