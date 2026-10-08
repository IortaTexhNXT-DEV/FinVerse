package com.iortatechnxt.brokerverse.issuance.domain;

import com.iortatechnxt.brokerverse.common.domain.AuthorizableEntity;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

/**
 * The recipient of the Insurance Advices of a mortgagee bank (FR-NB-107): the e-mail addresses and
 * the enrolment for automatic sending, for the bank as a whole or for one market segment of it.
 * Effective-dated, maker-checker.
 */
@Entity
@Table(name = "iss_advice_recipient")
public class AdviceRecipient extends AuthorizableEntity {

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "mortgagee_bank", nullable = false, length = 40, updatable = false)
  private String mortgageeBank;

  @Column(name = "market_segment", length = 30, updatable = false)
  private String marketSegment;

  @Column(name = "to_addresses", length = 1000)
  private String toAddresses;

  @Column(name = "cc_addresses", length = 1000)
  private String ccAddresses;

  @Column(name = "auto_send", nullable = false)
  private boolean autoSend;

  @Column(name = "effective_from", nullable = false)
  private LocalDate effectiveFrom;

  @Column(name = "effective_to")
  private LocalDate effectiveTo;

  protected AdviceRecipient() {}

  /**
   * Creates a set-up pending authorization.
   *
   * @param companyId company
   * @param data values
   */
  public AdviceRecipient(Long companyId, Data data) {
    this.companyId = companyId;
    this.mortgageeBank = data.mortgageeBank();
    this.marketSegment = data.marketSegment();
    apply(data);
  }

  /**
   * Changes the addresses, enrolment or dates; the set-up must be authorized again.
   *
   * @param data new values (bank and segment are kept)
   */
  public void update(Data data) {
    apply(data);
    markModified();
  }

  private void apply(Data data) {
    if (data.effectiveTo() != null && data.effectiveTo().isBefore(data.effectiveFrom())) {
      throw new BusinessRuleException(
          "LOV_EFFECTIVITY_INVALID", "The effective-to date is before the effective-from date");
    }
    this.toAddresses = join(data.to());
    this.ccAddresses = join(data.cc());
    this.autoSend = data.autoSend();
    this.effectiveFrom = data.effectiveFrom();
    this.effectiveTo = data.effectiveTo();
  }

  /**
   * Whether the set-up is authorized and in force on a date.
   *
   * @param date date
   * @return true when it counts
   */
  public boolean inForce(LocalDate date) {
    return isActive()
        && !date.isBefore(effectiveFrom)
        && (effectiveTo == null || !date.isAfter(effectiveTo));
  }

  private static String join(List<String> addresses) {
    return addresses == null || addresses.isEmpty() ? null : String.join(",", addresses);
  }

  private static List<String> split(String addresses) {
    return addresses == null ? List.of() : Arrays.asList(addresses.split(","));
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getMortgageeBank() {
    return mortgageeBank;
  }

  public String getMarketSegment() {
    return marketSegment;
  }

  public List<String> getTo() {
    return split(toAddresses);
  }

  public List<String> getCc() {
    return split(ccAddresses);
  }

  public boolean isAutoSend() {
    return autoSend;
  }

  public LocalDate getEffectiveFrom() {
    return effectiveFrom;
  }

  public LocalDate getEffectiveTo() {
    return effectiveTo;
  }

  /**
   * Values of a set-up.
   *
   * @param mortgageeBank mortgagee bank (list of values code)
   * @param marketSegment market segment, null for every segment of the bank
   * @param to recipients
   * @param cc copy recipients
   * @param autoSend enrolled for automatic sending
   * @param effectiveFrom first day
   * @param effectiveTo last day, null when open
   */
  public record Data(
      String mortgageeBank,
      String marketSegment,
      List<String> to,
      List<String> cc,
      boolean autoSend,
      LocalDate effectiveFrom,
      LocalDate effectiveTo) {

    /** Defensive copies. */
    public Data {
      to = to == null ? List.of() : List.copyOf(to);
      cc = cc == null ? List.of() : List.copyOf(cc);
    }
  }
}
