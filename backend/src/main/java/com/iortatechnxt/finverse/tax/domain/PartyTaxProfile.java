package com.iortatechnxt.finverse.tax.domain;

import com.iortatechnxt.finverse.common.domain.AuthorizableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/**
 * Tax profile of a business partner (maker-checker), kept beside the party master so the party
 * module is not altered: BIR TIN and branch code, registered name (split into last, first and
 * middle name for individuals, as the alphalists require), registered address, VAT treatment and
 * the default ATC applied to income payments to the party.
 *
 * <p>Parties without a profile are reported with the TIN of the party master and their name; their
 * withholding is shown under ATC "UNMAPPED" until a profile is authorized.
 */
@Entity
@Table(name = "tax_party_profile")
public class PartyTaxProfile extends AuthorizableEntity {

  @Column(name = "company_id", nullable = false)
  private Long companyId;

  @Column(name = "party_id", nullable = false)
  private Long partyId;

  @Column(name = "party_code", nullable = false, length = 30)
  private String partyCode;

  @Column(nullable = false, length = 9)
  private String tin;

  @Column(name = "branch_code", nullable = false, length = 5)
  private String branchCode;

  @Enumerated(EnumType.STRING)
  @Column(name = "payee_class", nullable = false, length = 12)
  private PayeeClass payeeClass;

  @Column(name = "registered_name", nullable = false, length = 200)
  private String registeredName;

  @Column(name = "last_name", length = 60)
  private String lastName;

  @Column(name = "first_name", length = 60)
  private String firstName;

  @Column(name = "middle_name", length = 60)
  private String middleName;

  @Column(name = "registered_address", length = 300)
  private String registeredAddress;

  @Column(name = "zip_code", length = 10)
  private String zipCode;

  @Enumerated(EnumType.STRING)
  @Column(name = "vat_treatment", nullable = false, length = 12)
  private VatTreatment vatTreatment = VatTreatment.REGULAR;

  @Column(name = "default_atc_code", length = 20)
  private String defaultAtcCode;

  protected PartyTaxProfile() {}

  /**
   * Creates a profile (pending authorization).
   *
   * @param companyId company
   * @param partyId party id
   * @param partyCode party code
   */
  public PartyTaxProfile(Long companyId, Long partyId, String partyCode) {
    this.companyId = companyId;
    this.partyId = partyId;
    this.partyCode = partyCode;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public Long getPartyId() {
    return partyId;
  }

  public String getPartyCode() {
    return partyCode;
  }

  public String getTin() {
    return tin;
  }

  public void setTin(String tin) {
    this.tin = tin;
  }

  public String getBranchCode() {
    return branchCode;
  }

  public void setBranchCode(String branchCode) {
    this.branchCode = branchCode;
  }

  public PayeeClass getPayeeClass() {
    return payeeClass;
  }

  public void setPayeeClass(PayeeClass payeeClass) {
    this.payeeClass = payeeClass;
  }

  public String getRegisteredName() {
    return registeredName;
  }

  public void setRegisteredName(String registeredName) {
    this.registeredName = registeredName;
  }

  public String getLastName() {
    return lastName;
  }

  public void setLastName(String lastName) {
    this.lastName = lastName;
  }

  public String getFirstName() {
    return firstName;
  }

  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }

  public String getMiddleName() {
    return middleName;
  }

  public void setMiddleName(String middleName) {
    this.middleName = middleName;
  }

  public String getRegisteredAddress() {
    return registeredAddress;
  }

  public void setRegisteredAddress(String registeredAddress) {
    this.registeredAddress = registeredAddress;
  }

  public String getZipCode() {
    return zipCode;
  }

  public void setZipCode(String zipCode) {
    this.zipCode = zipCode;
  }

  public VatTreatment getVatTreatment() {
    return vatTreatment;
  }

  public void setVatTreatment(VatTreatment vatTreatment) {
    this.vatTreatment = vatTreatment;
  }

  public String getDefaultAtcCode() {
    return defaultAtcCode;
  }

  public void setDefaultAtcCode(String defaultAtcCode) {
    this.defaultAtcCode = defaultAtcCode;
  }
}
