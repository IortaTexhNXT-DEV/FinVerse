package com.iortatechnxt.brokerverse.nbadmin.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A data retention rule (BRNB.106, NFR retention): records of a type in one of the statuses become
 * eligible once they have been inactive for {@code yearsOnline} years; they are then kept {@code
 * yearsArchive} more years in the archive.
 */
@Entity
@Table(name = "nba_retention_rule")
public class RetentionRule extends BaseEntity {

  @Column(name = "record_type", nullable = false, length = 40, updatable = false)
  private String recordType;

  @Column(nullable = false, length = 200)
  private String statuses;

  @Column(name = "years_online", nullable = false)
  private int yearsOnline;

  @Column(name = "years_archive", nullable = false)
  private int yearsArchive;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private RetentionAction action;

  @Column(nullable = false)
  private boolean active = true;

  @Column(nullable = false, length = 300)
  private String description;

  protected RetentionRule() {}

  /**
   * Changes the rule.
   *
   * @param terms new terms
   */
  public void change(RetentionTerms terms) {
    if (terms.yearsOnline() <= 0 || terms.yearsArchive() < 0) {
      throw new BusinessRuleException(
          "RETENTION_YEARS", "Years online must be positive and years in archive not negative");
    }
    Set<String> codes = normalise(terms.statuses());
    if (codes.isEmpty()) {
      throw new BusinessRuleException("RETENTION_STATUSES", "Enter at least one status");
    }
    this.statuses = String.join(",", codes);
    this.yearsOnline = terms.yearsOnline();
    this.yearsArchive = terms.yearsArchive();
    this.action = terms.action();
    this.active = terms.active();
    this.description = terms.description();
  }

  /**
   * Last activity date a record may have to be eligible on a business date.
   *
   * @param businessDate business date
   * @return cutoff date
   */
  public LocalDate cutoff(LocalDate businessDate) {
    return businessDate.minusYears(yearsOnline);
  }

  /**
   * The statuses as a set.
   *
   * @return status codes
   */
  public Set<String> statusSet() {
    return normalise(statuses);
  }

  private static Set<String> normalise(String text) {
    return Arrays.stream(text == null ? new String[0] : text.split(","))
        .map(s -> s.trim().toUpperCase(Locale.ROOT))
        .filter(s -> !s.isEmpty())
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  public String getRecordType() {
    return recordType;
  }

  public String getStatuses() {
    return statuses;
  }

  public int getYearsOnline() {
    return yearsOnline;
  }

  public int getYearsArchive() {
    return yearsArchive;
  }

  public RetentionAction getAction() {
    return action;
  }

  public boolean isActive() {
    return active;
  }

  public String getDescription() {
    return description;
  }
}
