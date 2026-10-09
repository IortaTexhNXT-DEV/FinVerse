package com.iortatechnxt.brokerverse.catalog.domain;

import com.iortatechnxt.brokerverse.common.domain.AuthorizableEntity;
import com.iortatechnxt.brokerverse.common.domain.RecordOrigin;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

/**
 * Panel insurer: the broking profile of a party of type INSURER (accreditation, placement channel
 * and mailboxes, default credit days). Branches carry the LGT rate (Appendix A). Placement by
 * e-mail only; SFTP and API channels are parked (Q06).
 */
@Entity
@Table(name = "cat_insurer")
public class InsurerProfile extends AuthorizableEntity implements CatalogRecord {

  @Embedded private RecordOrigin recordOrigin = RecordOrigin.BIBS;

  private static final String SEPARATOR = ",";
  private static final int MAX_CREDIT_DAYS = 365;

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "party_code", nullable = false, length = 30, updatable = false)
  private String partyCode;

  @Column(nullable = false, length = 200)
  private String name;

  @Column(name = "short_name", length = 40)
  private String shortName;

  @Column(name = "accreditation_no", length = 40)
  private String accreditationNo;

  @Column(name = "accredited_until")
  private LocalDate accreditedUntil;

  @Enumerated(EnumType.STRING)
  @Column(name = "placement_channel", nullable = false, length = 10)
  private PlacementChannel placementChannel;

  @Column(name = "placement_emails", length = 500)
  private String placementEmails;

  @Column(name = "default_credit_days", nullable = false)
  private int defaultCreditDays;

  @Enumerated(EnumType.STRING)
  @Column(name = "tax_status", length = 15)
  private InsurerTaxStatus taxStatus;

  protected InsurerProfile() {}

  /**
   * Creates a profile, pending authorization.
   *
   * @param companyId company
   * @param partyCode insurer party code
   * @param details profile attributes
   */
  public InsurerProfile(Long companyId, String partyCode, InsurerDetails details) {
    this.companyId = companyId;
    this.partyCode = partyCode;
    apply(details);
  }

  /**
   * Changes the profile; it must be authorized again.
   *
   * @param details new attributes
   */
  public void update(InsurerDetails details) {
    apply(details);
    markModified();
  }

  private void apply(InsurerDetails d) {
    if (d.placementChannel() != PlacementChannel.EMAIL) {
      throw new BusinessRuleException(
          "PLACEMENT_CHANNEL_PARKED",
          "Only e-mail placement is available; choose the e-mail placement channel");
    }
    if (d.defaultCreditDays() < 0 || d.defaultCreditDays() > MAX_CREDIT_DAYS) {
      throw new BusinessRuleException(
          "CREDIT_DAYS_INVALID", "Default credit days must be between 0 and 365");
    }
    this.name = d.name();
    this.shortName = d.shortName();
    this.accreditationNo = d.accreditationNo();
    this.accreditedUntil = d.accreditedUntil();
    this.placementChannel = d.placementChannel();
    this.placementEmails =
        d.placementEmails() == null || d.placementEmails().isEmpty()
            ? null
            : String.join(SEPARATOR, d.placementEmails());
    this.defaultCreditDays = d.defaultCreditDays();
    this.taxStatus = d.taxStatus();
  }

  /**
   * Whether the insurer's accreditation covers a date (no expiry recorded = accredited).
   *
   * @param date date
   * @return true when accredited
   */
  public boolean isAccreditedOn(LocalDate date) {
    return accreditedUntil == null || !date.isAfter(accreditedUntil);
  }

  /**
   * Placement mailboxes.
   *
   * @return e-mail addresses
   */
  public List<String> getPlacementEmailList() {
    return placementEmails == null ? List.of() : Arrays.asList(placementEmails.split(SEPARATOR));
  }

  @Override
  public String catalogReference() {
    return partyCode;
  }

  @Override
  public String catalogDescription() {
    return name;
  }

  @Override
  public Long catalogCompanyId() {
    return companyId;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getPartyCode() {
    return partyCode;
  }

  public String getName() {
    return name;
  }

  public String getShortName() {
    return shortName;
  }

  public String getAccreditationNo() {
    return accreditationNo;
  }

  public LocalDate getAccreditedUntil() {
    return accreditedUntil;
  }

  public PlacementChannel getPlacementChannel() {
    return placementChannel;
  }

  public int getDefaultCreditDays() {
    return defaultCreditDays;
  }

  /**
   * VAT registration of the insurer; null follows the taxes of the product line.
   *
   * @return tax status, may be null
   */
  public InsurerTaxStatus getTaxStatus() {
    return taxStatus;
  }

  /**
   * Maintainable attributes of an insurer profile.
   *
   * @param name insurer name
   * @param shortName short name for lists
   * @param accreditationNo Insurance Commission accreditation number
   * @param accreditedUntil accreditation expiry
   * @param placementChannel channel (EMAIL; others parked)
   * @param placementEmails placement mailboxes
   * @param defaultCreditDays default credit days
   * @param taxStatus VAT registration of the insurer, null to follow the product line
   */
  public record InsurerDetails(
      String name,
      String shortName,
      String accreditationNo,
      LocalDate accreditedUntil,
      PlacementChannel placementChannel,
      List<String> placementEmails,
      int defaultCreditDays,
      InsurerTaxStatus taxStatus) {

    /**
     * Details without a tax status (the taxes of the product line apply).
     *
     * @param name name
     * @param shortName short name
     * @param accreditationNo accreditation number
     * @param accreditedUntil accreditation end
     * @param placementChannel placement channel
     * @param placementEmails placement e-mails
     * @param defaultCreditDays default credit days
     */
    public InsurerDetails(
        String name,
        String shortName,
        String accreditationNo,
        LocalDate accreditedUntil,
        PlacementChannel placementChannel,
        List<String> placementEmails,
        int defaultCreditDays) {
      this(
          name,
          shortName,
          accreditationNo,
          accreditedUntil,
          placementChannel,
          placementEmails,
          defaultCreditDays,
          null);
    }
  }

  /**
   * Marks a record loaded from a legacy system (DATA_MIGRATION_DESIGN section 10).
   *
   * @param origin source system, legacy reference and batch
   */
  public void markMigrated(RecordOrigin origin) {
    this.recordOrigin = origin;
  }

  /**
   * Where the record comes from.
   *
   * @return BIBS or the legacy origin
   */
  public RecordOrigin getRecordOrigin() {
    return recordOrigin == null ? RecordOrigin.BIBS : recordOrigin;
  }
}
