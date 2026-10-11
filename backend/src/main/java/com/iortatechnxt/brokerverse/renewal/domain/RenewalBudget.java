package com.iortatechnxt.brokerverse.renewal.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A record of the Annual Renewal Budget (BDOI Renewal FRS FRRN.042.02): a fiscal year, a market
 * segment and the hierarchy it is set for, with the new, renewal and organic budget of each month.
 * The measure tells whether the amounts are basic premium or gross commission (FRRN.002.02.01).
 */
@Entity
@Table(name = "rnw_budget")
public class RenewalBudget extends BaseEntity {

  /** Months of a fiscal year. */
  public static final int MONTHS = 12;

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "fiscal_year", nullable = false, updatable = false)
  private int fiscalYear;

  @Column(nullable = false, length = 20, updatable = false)
  private String measure;

  @Column(nullable = false, length = 40, updatable = false)
  private String segment;

  @Column(length = 40, updatable = false)
  private String region;

  @Column(length = 40, updatable = false)
  private String team;

  @Column(name = "sub_team", length = 40, updatable = false)
  private String subTeam;

  @Column(name = "unit_head", length = 50)
  private String unitHead;

  @Column(name = "section_head", length = 50)
  private String sectionHead;

  @Column(name = "team_head", length = 50)
  private String teamHead;

  @Column(name = "team_lead", length = 50)
  private String teamLead;

  @Column(name = "account_officer", length = 50, updatable = false)
  private String accountOfficer;

  @Column(name = "natural_key", nullable = false, length = 400, updatable = false)
  private String naturalKey;

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "rnw_budget_month", joinColumns = @JoinColumn(name = "budget_id"))
  @OrderBy("monthNo")
  private final List<Month> months = new ArrayList<>();

  protected RenewalBudget() {}

  /**
   * Creates a budget record with zero amounts.
   *
   * @param companyId company
   * @param key fiscal year, measure, segment and hierarchy
   */
  public RenewalBudget(Long companyId, Key key) {
    this.companyId = companyId;
    this.fiscalYear = key.fiscalYear();
    this.measure = key.measure();
    this.segment = key.segment();
    this.region = key.region();
    this.team = key.team();
    this.subTeam = key.subTeam();
    this.accountOfficer = key.accountOfficer();
    this.naturalKey = key.text();
    for (int m = 1; m <= MONTHS; m++) {
      months.add(new Month(m, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
    }
  }

  /**
   * Sets the heads of the hierarchy.
   *
   * @param heads unit head, section head, team head and team lead
   */
  public void heads(Heads heads) {
    this.unitHead = heads.unitHead();
    this.sectionHead = heads.sectionHead();
    this.teamHead = heads.teamHead();
    this.teamLead = heads.teamLead();
  }

  /**
   * Replaces the amounts of a month.
   *
   * @param month new amounts
   */
  public void month(Month month) {
    months.replaceAll(m -> m.monthNo() == month.monthNo() ? month : m);
  }

  /**
   * The amounts of a month.
   *
   * @param monthNo 1 to 12
   * @return amounts
   */
  public Month month(int monthNo) {
    return months.stream().filter(m -> m.monthNo() == monthNo).findFirst().orElseThrow();
  }

  /**
   * The annual totals: new, renewal, organic and the grand total (FRRN.042.02).
   *
   * @return totals
   */
  public Totals totals() {
    BigDecimal n = BigDecimal.ZERO;
    BigDecimal r = BigDecimal.ZERO;
    BigDecimal o = BigDecimal.ZERO;
    for (Month m : months) {
      n = n.add(m.newAmount());
      r = r.add(m.renewalAmount());
      o = o.add(m.organicAmount());
    }
    return new Totals(n, r, o, n.add(r).add(o));
  }

  public Long getCompanyId() {
    return companyId;
  }

  public int getFiscalYear() {
    return fiscalYear;
  }

  public String getMeasure() {
    return measure;
  }

  public String getSegment() {
    return segment;
  }

  public String getRegion() {
    return region;
  }

  public String getTeam() {
    return team;
  }

  public String getSubTeam() {
    return subTeam;
  }

  public String getUnitHead() {
    return unitHead;
  }

  public String getSectionHead() {
    return sectionHead;
  }

  public String getTeamHead() {
    return teamHead;
  }

  public String getTeamLead() {
    return teamLead;
  }

  public String getAccountOfficer() {
    return accountOfficer;
  }

  public List<Month> getMonths() {
    return List.copyOf(months);
  }

  /**
   * The amounts of one month.
   *
   * @param monthNo 1 to 12
   * @param newAmount new business budget
   * @param renewalAmount renewal budget
   * @param organicAmount organic budget
   */
  @Embeddable
  public record Month(
      @Column(name = "month_no") int monthNo,
      @Column(name = "new_amount") BigDecimal newAmount,
      @Column(name = "renewal_amount") BigDecimal renewalAmount,
      @Column(name = "organic_amount") BigDecimal organicAmount) {}

  /**
   * Annual totals.
   *
   * @param newTotal annual new budget
   * @param renewalTotal annual renewal budget
   * @param organicTotal annual organic budget
   * @param grandTotal sum of the three
   */
  public record Totals(
      BigDecimal newTotal,
      BigDecimal renewalTotal,
      BigDecimal organicTotal,
      BigDecimal grandTotal) {}

  /**
   * Heads of the hierarchy of a budget record.
   *
   * @param unitHead unit head
   * @param sectionHead section head
   * @param teamHead team head
   * @param teamLead team lead
   */
  public record Heads(String unitHead, String sectionHead, String teamHead, String teamLead) {}

  /**
   * The natural key of a budget record.
   *
   * @param fiscalYear fiscal year
   * @param measure PREMIUM or COMMISSION
   * @param segment market segment
   * @param region region (not for Corporate)
   * @param team team (Corporate)
   * @param subTeam sub-team (Corporate)
   * @param accountOfficer account officer (not for CBG)
   */
  public record Key(
      int fiscalYear,
      String measure,
      String segment,
      String region,
      String team,
      String subTeam,
      String accountOfficer) {

    /**
     * The key as one text, upper case.
     *
     * @return text
     */
    public String text() {
      return Stream.of(
              String.valueOf(fiscalYear), measure, segment, region, team, subTeam, accountOfficer)
          .map(v -> Objects.toString(v, "").strip().toUpperCase(Locale.ROOT))
          .collect(Collectors.joining("|"));
    }
  }
}
