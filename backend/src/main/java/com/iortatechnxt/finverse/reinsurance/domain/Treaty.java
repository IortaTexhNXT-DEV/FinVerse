package com.iortatechnxt.finverse.reinsurance.domain;

import com.iortatechnxt.finverse.common.domain.AuthorizableEntity;
import com.iortatechnxt.finverse.common.exception.BusinessRuleException;
import com.iortatechnxt.finverse.common.util.Money;
import com.iortatechnxt.finverse.party.domain.Party;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

/**
 * Reinsurance treaty of a company's programme for one line of business and underwriting year
 * (maker-checker master data). Proportional treaties (quota share, surplus) share premium and
 * losses in proportion to the sum insured ceded; excess of loss treaties recover losses above a
 * priority per layer. Participants are the reinsurers and their shares.
 *
 * <p>Participants, layers and parties are small and always needed with the treaty, so they are
 * loaded eagerly (one sub-select per collection for a list of treaties).
 */
@Entity
@Table(name = "ri_treaty")
public class Treaty extends AuthorizableEntity {

  /** Statement frequency supported by FinVerse. */
  public static final String QUARTERLY = "QUARTERLY";

  private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(nullable = false, updatable = false, length = 20)
  private String code;

  @Column(nullable = false, length = 120)
  private String name;

  @Enumerated(EnumType.STRING)
  @Column(name = "treaty_type", nullable = false, length = 15)
  private TreatyType treatyType;

  @Column(name = "business_line", nullable = false, length = 20)
  private String businessLine;

  @Column(name = "uw_year", nullable = false)
  private int uwYear;

  @Column(name = "period_from", nullable = false)
  private LocalDate periodFrom;

  @Column(name = "period_to", nullable = false)
  private LocalDate periodTo;

  @Column(nullable = false, length = 3)
  private String currency;

  @Column(name = "quota_share_pct", precision = 19, scale = 8)
  private BigDecimal quotaSharePct;

  @Column(name = "treaty_limit", precision = 19, scale = 2)
  private BigDecimal treatyLimit;

  @Column(name = "retention_limit", precision = 19, scale = 2)
  private BigDecimal retentionLimit;

  @Column(name = "lines")
  private Integer lines;

  @Column(name = "levy_pct", nullable = false, precision = 19, scale = 8)
  private BigDecimal levyPct;

  @Column(name = "reserve_interest_pct", nullable = false, precision = 19, scale = 8)
  private BigDecimal reserveInterestPct;

  @Column(name = "loss_reserve_pct", nullable = false, precision = 19, scale = 8)
  private BigDecimal lossReservePct;

  @Column(name = "statement_frequency", nullable = false, length = 10)
  private String statementFrequency = QUARTERLY;

  @ManyToOne
  @JoinColumn(name = "broker_party_id")
  private Party broker;

  @ElementCollection(fetch = FetchType.EAGER)
  @Fetch(FetchMode.SUBSELECT)
  @CollectionTable(name = "ri_treaty_participant", joinColumns = @JoinColumn(name = "treaty_id"))
  @OrderBy("lineNo")
  private List<TreatyParticipant> participants = new ArrayList<>();

  @ElementCollection(fetch = FetchType.EAGER)
  @Fetch(FetchMode.SUBSELECT)
  @CollectionTable(name = "ri_treaty_layer", joinColumns = @JoinColumn(name = "treaty_id"))
  @OrderBy("layerNo")
  private List<TreatyLayer> layers = new ArrayList<>();

  /** For JPA. */
  protected Treaty() {}

  /**
   * Creates a treaty pending authorization.
   *
   * @param companyId company
   * @param code unique code
   * @param terms terms
   */
  public Treaty(Long companyId, String code, TreatyTerms terms) {
    this.companyId = companyId;
    this.code = code;
    apply(terms);
  }

  /**
   * Replaces the terms, broker, participants and layers after validating them; a modified active
   * treaty returns to pending authorization.
   *
   * @param terms terms
   * @param newBroker reinsurance broker, null when placed direct
   * @param newParticipants reinsurers and shares (must total 100 %)
   * @param newLayers excess of loss layers (excess of loss only)
   */
  public void define(
      TreatyTerms terms,
      Party newBroker,
      List<TreatyParticipant> newParticipants,
      List<TreatyLayer> newLayers) {
    if (getId() != null) {
      apply(terms);
      markModified();
    }
    requireTypeTerms();
    requireShares(newParticipants);
    if (treatyType == TreatyType.XOL && newLayers.isEmpty()) {
      throw new BusinessRuleException("TREATY_LAYERS", "An excess of loss treaty needs a layer");
    }
    this.broker = newBroker;
    participants.clear();
    participants.addAll(newParticipants);
    layers.clear();
    if (treatyType == TreatyType.XOL) {
      layers.addAll(newLayers);
    }
  }

  /**
   * Whether the treaty covers a date.
   *
   * @param date date
   * @return true when within the treaty period
   */
  public boolean covers(LocalDate date) {
    return !date.isBefore(periodFrom) && !date.isAfter(periodTo);
  }

  /**
   * Quota share rate as a fraction.
   *
   * @return quota share % / 100 (zero for other types)
   */
  public BigDecimal quotaShareFraction() {
    return treatyType == TreatyType.QUOTA_SHARE
        ? quotaSharePct.divide(HUNDRED, Money.RATE_SCALE, Money.ROUNDING)
        : BigDecimal.ZERO;
  }

  private void apply(TreatyTerms t) {
    this.name = t.name();
    this.treatyType = t.treatyType();
    this.businessLine = t.businessLine();
    this.uwYear = t.uwYear();
    this.periodFrom = t.periodFrom();
    this.periodTo = t.periodTo();
    this.currency = t.currency();
    this.quotaSharePct = t.quotaSharePct();
    this.treatyLimit = t.treatyLimit();
    this.retentionLimit = t.retentionLimit();
    this.lines = t.lines();
    this.levyPct = Money.nz(t.levyPct());
    this.reserveInterestPct = Money.nz(t.reserveInterestPct());
    this.lossReservePct = Money.nz(t.lossReservePct());
  }

  private void requireTypeTerms() {
    if (periodTo.isBefore(periodFrom)) {
      throw new BusinessRuleException("TREATY_PERIOD", "Treaty period ends before it starts");
    }
    boolean valid =
        switch (treatyType) {
          case QUOTA_SHARE ->
              Money.isPositive(quotaSharePct) && quotaSharePct.compareTo(HUNDRED) <= 0;
          case SURPLUS -> Money.isPositive(retentionLimit) && lines != null && lines > 0;
          case XOL -> true;
        };
    if (!valid) {
      throw new BusinessRuleException(
          "TREATY_TERMS",
          "A quota share needs a quota share % (0-100]; a surplus needs a retention and lines");
    }
  }

  private static void requireShares(List<TreatyParticipant> list) {
    BigDecimal total =
        list.stream().map(TreatyParticipant::getSharePct).reduce(BigDecimal.ZERO, BigDecimal::add);
    if (list.isEmpty() || total.compareTo(HUNDRED) != 0) {
      throw new BusinessRuleException(
          "TREATY_SHARES", "Participants' shares must total 100 % (found " + total + ")");
    }
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getCode() {
    return code;
  }

  public String getName() {
    return name;
  }

  public TreatyType getTreatyType() {
    return treatyType;
  }

  public String getBusinessLine() {
    return businessLine;
  }

  public int getUwYear() {
    return uwYear;
  }

  public LocalDate getPeriodFrom() {
    return periodFrom;
  }

  public LocalDate getPeriodTo() {
    return periodTo;
  }

  public String getCurrency() {
    return currency;
  }

  public BigDecimal getQuotaSharePct() {
    return quotaSharePct;
  }

  public BigDecimal getTreatyLimit() {
    return treatyLimit;
  }

  public BigDecimal getRetentionLimit() {
    return retentionLimit;
  }

  public Integer getLines() {
    return lines;
  }

  public BigDecimal getLevyPct() {
    return levyPct;
  }

  public BigDecimal getReserveInterestPct() {
    return reserveInterestPct;
  }

  public BigDecimal getLossReservePct() {
    return lossReservePct;
  }

  public String getStatementFrequency() {
    return statementFrequency;
  }

  public Party getBroker() {
    return broker;
  }

  public List<TreatyParticipant> getParticipants() {
    return List.copyOf(participants);
  }

  public List<TreatyLayer> getLayers() {
    return List.copyOf(layers);
  }
}
