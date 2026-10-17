package com.iortatechnxt.brokerverse.catalog.domain;

import com.iortatechnxt.brokerverse.common.domain.AuthorizableEntity;
import com.iortatechnxt.brokerverse.common.security.OwnedBy;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * A participating insurer of an insurer's default co-insurance arrangement (BRD-00 CRQ08, Risk
 * Participation): its share in percent, whether it leads, and the effective dates. Maker-checker;
 * the authorized shares effective on a booking date are the default co-insurance split of an
 * invoice of that insurer (FR-OP-007) when they total 100 %.
 */
@OwnedBy(value = InsurerProfile.class, key = "insurerId")
@Entity
@Table(name = "cat_insurer_participation")
public class InsurerParticipation extends AuthorizableEntity implements CatalogRecord {

  private static final int SCALE = 4;

  @Column(name = "insurer_id", nullable = false, updatable = false)
  private Long insurerId;

  @Column(name = "participant_code", nullable = false, length = 30)
  private String participantCode;

  @Column(name = "share_pct", nullable = false, precision = 9, scale = 4)
  private BigDecimal sharePct;

  @Column(nullable = false)
  private boolean lead;

  @Column(name = "effective_from", nullable = false)
  private LocalDate effectiveFrom;

  @Column(name = "effective_to")
  private LocalDate effectiveTo;

  protected InsurerParticipation() {}

  /**
   * Creates a participation, pending authorization.
   *
   * @param insurerId insurer profile of the arrangement
   * @param details participation
   */
  public InsurerParticipation(Long insurerId, ParticipationDetails details) {
    this.insurerId = insurerId;
    apply(details);
  }

  /**
   * Changes the participation; it must be authorized again.
   *
   * @param details participation
   */
  public void update(ParticipationDetails details) {
    apply(details);
    markModified();
  }

  private void apply(ParticipationDetails d) {
    this.participantCode = d.participantCode();
    this.sharePct = d.sharePct().setScale(SCALE, RoundingMode.HALF_UP);
    this.lead = d.lead();
    this.effectiveFrom = d.effectiveFrom();
    this.effectiveTo = d.effectiveTo();
  }

  /**
   * Whether the participation is effective on a date.
   *
   * @param date date
   * @return true when the date is within the effective dates
   */
  public boolean effectiveOn(LocalDate date) {
    return !date.isBefore(effectiveFrom) && (effectiveTo == null || !date.isAfter(effectiveTo));
  }

  /**
   * Whether the effective dates of two participations overlap.
   *
   * @param from start of the other
   * @param to end of the other, null when open
   * @return true when they overlap
   */
  public boolean overlaps(LocalDate from, LocalDate to) {
    boolean startsBeforeOtherEnds = to == null || !effectiveFrom.isAfter(to);
    boolean endsAfterOtherStarts = effectiveTo == null || !effectiveTo.isBefore(from);
    return startsBeforeOtherEnds && endsAfterOtherStarts;
  }

  @Override
  public String catalogReference() {
    return insurerId + "/" + participantCode + "/" + effectiveFrom;
  }

  @Override
  public String catalogDescription() {
    return participantCode
        + " "
        + sharePct.stripTrailingZeros().toPlainString()
        + "%"
        + (lead ? " (lead)" : "");
  }

  public Long getInsurerId() {
    return insurerId;
  }

  public String getParticipantCode() {
    return participantCode;
  }

  public BigDecimal getSharePct() {
    return sharePct;
  }

  public boolean isLead() {
    return lead;
  }

  public LocalDate getEffectiveFrom() {
    return effectiveFrom;
  }

  public LocalDate getEffectiveTo() {
    return effectiveTo;
  }

  /**
   * Maintainable attributes of a participation.
   *
   * @param participantCode party code of the participating insurer
   * @param sharePct share in percent
   * @param lead whether it leads the arrangement
   * @param effectiveFrom first day
   * @param effectiveTo last day, null when open
   */
  public record ParticipationDetails(
      String participantCode,
      BigDecimal sharePct,
      boolean lead,
      LocalDate effectiveFrom,
      LocalDate effectiveTo) {}
}
