package com.iortatechnxt.brokerverse.crm.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.crm.domain.ClientDetails.Contact;
import com.iortatechnxt.brokerverse.crm.domain.ClientDetails.Identity;
import com.iortatechnxt.brokerverse.crm.domain.ClientDetails.PersonName;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A broking client. It starts as a <b>prospect</b> with a prospect code (minimum data, may be
 * quoted) and becomes a <b>confirmed</b> client with a client code once onboarded; both codes are
 * kept so history stays traceable (BRNB.090/101). Confirmation links the client to its sub-ledger
 * party ({@code partyCode}) for premium receivables.
 */
@Entity
@Table(name = "crm_client")
public class Client extends BaseEntity {

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "prospect_code", nullable = false, length = 30, updatable = false)
  private String prospectCode;

  @Column(name = "client_code", length = 30)
  private String clientCode;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private ClientStatus status = ClientStatus.PROSPECT;

  @Enumerated(EnumType.STRING)
  @Column(name = "client_type", nullable = false, length = 20)
  private ClientType clientType;

  @Column(name = "last_name", length = 100)
  private String lastName;

  @Column(name = "first_name", length = 100)
  private String firstName;

  @Column(name = "middle_name", length = 100)
  private String middleName;

  @Column(length = 20)
  private String suffix;

  @Column(name = "corporate_name", length = 200)
  private String corporateName;

  @Column(name = "display_name", nullable = false, length = 250)
  private String displayName;

  @Column(name = "birth_date")
  private LocalDate birthDate;

  @Column(length = 20)
  private String tin;

  @Column(name = "id_type", length = 40)
  private String idType;

  @Column(name = "id_number", length = 60)
  private String idNumber;

  @Column(length = 120)
  private String email;

  @Column(length = 30)
  private String mobile;

  @Column(length = 30)
  private String phone;

  @Column(name = "address_line", length = 300)
  private String addressLine;

  @Column(length = 80)
  private String city;

  @Column(length = 80)
  private String province;

  @Column(name = "postal_code", length = 10)
  private String postalCode;

  @Column(name = "market_segment", length = 40)
  private String marketSegment;

  @Column(name = "bank_client", nullable = false)
  private boolean bankClient;

  @Column(name = "bank_cif", length = 40)
  private String bankCif;

  @Column(name = "party_code", length = 30)
  private String partyCode;

  @Enumerated(EnumType.STRING)
  @Column(name = "kyc_status", nullable = false, length = 20)
  private KycStatus kycStatus = KycStatus.NOT_STARTED;

  @Column(name = "kyc_verified_by", length = 50)
  private String kycVerifiedBy;

  @Column(name = "kyc_verified_at")
  private Instant kycVerifiedAt;

  @Column(name = "kyc_review_due")
  private LocalDate kycReviewDue;

  @Column(name = "confirmed_by", length = 50)
  private String confirmedBy;

  @Column(name = "confirmed_at")
  private Instant confirmedAt;

  @Column(length = 40)
  private String nationality;

  @Column(name = "civil_status", length = 40)
  private String civilStatus;

  @Column(length = 120)
  private String occupation;

  @Column(name = "source_of_funds", length = 40)
  private String sourceOfFunds;

  @Column(name = "risk_rating", length = 40)
  private String riskRating;

  @Column(name = "onboarding_stage", length = 40)
  private String onboardingStage;

  @Column(name = "kyc_submitted_by", length = 50)
  private String kycSubmittedBy;

  @Column(name = "kyc_submitted_at")
  private Instant kycSubmittedAt;

  @Column(name = "deactivation_reason", length = 40)
  private String deactivationReason;

  @Column(name = "deactivation_note", length = 500)
  private String deactivationNote;

  @Column(name = "deactivated_by", length = 50)
  private String deactivatedBy;

  @Column(name = "deactivated_at")
  private Instant deactivatedAt;

  @Embedded private ClientKeys keys;

  protected Client() {}

  /**
   * Creates a prospect.
   *
   * @param companyId company
   * @param prospectCode prospect code
   * @param details client data
   */
  public Client(Long companyId, String prospectCode, ClientDetails details) {
    this.companyId = companyId;
    this.prospectCode = prospectCode;
    apply(details);
  }

  /**
   * Updates the client data.
   *
   * @param details new data
   */
  public void update(ClientDetails details) {
    if (status == ClientStatus.INACTIVE) {
      throw new BusinessRuleException("CLIENT_INACTIVE", "An inactive client cannot be changed");
    }
    if (status == ClientStatus.CONFIRMED && details.clientType() != clientType) {
      throw new BusinessRuleException(
          "CLIENT_TYPE_LOCKED", "The client type of a confirmed client cannot be changed");
    }
    apply(details);
  }

  private void apply(ClientDetails d) {
    PersonName n = d.name();
    this.clientType = d.clientType();
    if (clientType == ClientType.INDIVIDUAL) {
      requireText(n.lastName(), "last name");
      requireText(n.firstName(), "first name");
      this.lastName = n.lastName();
      this.firstName = n.firstName();
      this.middleName = n.middleName();
      this.suffix = n.suffix();
      this.corporateName = null;
    } else {
      requireText(n.corporateName(), "corporate name");
      this.corporateName = n.corporateName();
      this.lastName = null;
      this.firstName = null;
      this.middleName = null;
      this.suffix = null;
    }
    this.displayName = displayNameOf(clientType, n);
    this.birthDate = d.birthDate();
    Identity i = d.identity() == null ? new Identity(null, null, null) : d.identity();
    this.tin = i.tin();
    this.idType = i.idType();
    this.idNumber = i.idNumber();
    Contact c =
        d.contact() == null ? new Contact(null, null, null, null, null, null, null) : d.contact();
    this.email = c.email();
    this.mobile = c.mobile();
    this.phone = c.phone();
    this.addressLine = c.addressLine();
    this.city = c.city();
    this.province = c.province();
    this.postalCode = c.postalCode();
    this.marketSegment = d.marketSegment();
    this.bankClient = d.bankClient();
    this.bankCif = d.bankCif();
    this.keys =
        ClientKeys.of(
            new Identity(null, idType, idNumber),
            mobile,
            new PersonName(lastName, firstName, null, null, corporateName));
  }

  /**
   * Sets the KYC profile (nationality, civil status, occupation, source of funds, risk rating).
   *
   * @param profile profile; null keeps nothing
   */
  public void applyProfile(ClientProfile profile) {
    if (status == ClientStatus.INACTIVE) {
      throw new BusinessRuleException("CLIENT_INACTIVE", "An inactive client cannot be changed");
    }
    ClientProfile p = profile == null ? ClientProfile.EMPTY : profile;
    this.nationality = p.nationality();
    this.civilStatus = p.civilStatus();
    this.occupation = p.occupation();
    this.sourceOfFunds = p.sourceOfFunds();
    this.riskRating = p.riskRating();
  }

  /**
   * Sets the KYC risk rating from risk profiling (SNSRP-302, 304) without touching the other
   * profile fields; a review date earlier than the current one brings the periodic KYC review
   * forward (BRNB.110).
   *
   * @param code risk rating (list of values KYC_RISK_RATING)
   * @param reviewDue review date implied by the new rating, null to keep the current one
   */
  public void applyRiskRating(String code, LocalDate reviewDue) {
    if (status == ClientStatus.INACTIVE) {
      throw new BusinessRuleException("CLIENT_INACTIVE", "An inactive client cannot be changed");
    }
    this.riskRating = code;
    if (reviewDue != null && (kycReviewDue == null || reviewDue.isBefore(kycReviewDue))) {
      this.kycReviewDue = reviewDue;
    }
  }

  /**
   * The KYC profile.
   *
   * @return profile
   */
  public ClientProfile profile() {
    return new ClientProfile(nationality, civilStatus, occupation, sourceOfFunds, riskRating);
  }

  /**
   * Records the submission of the KYC documents for verification (maker step).
   *
   * @param user submitting user
   * @param when time
   */
  public void submitKyc(String user, Instant when) {
    this.kycSubmittedBy = user;
    this.kycSubmittedAt = when;
    this.kycStatus = KycStatus.PENDING;
  }

  /**
   * Mirrors the onboarding workflow stage (BRNB.090).
   *
   * @param stage stage code of workflow NB_CLIENT
   */
  public void mirrorStage(String stage) {
    this.onboardingStage = stage;
  }

  /**
   * Deactivates the client with a reason (no new business; history is kept).
   *
   * @param reason reason code (list of values CLIENT_DEACTIVATION_REASON)
   * @param note comment
   * @param user deactivating user
   * @param when time
   */
  public void deactivate(String reason, String note, String user, Instant when) {
    if (status == ClientStatus.INACTIVE) {
      throw new BusinessRuleException("CLIENT_INACTIVE", "The client is already inactive");
    }
    this.status = ClientStatus.INACTIVE;
    this.deactivationReason = reason;
    this.deactivationNote = note;
    this.deactivatedBy = user;
    this.deactivatedAt = when;
  }

  private static void requireText(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new BusinessRuleException("CLIENT_NAME_REQUIRED", "Enter the " + field);
    }
  }

  /**
   * Display name: "LAST, First Middle Suffix" for individuals, the corporate name otherwise.
   *
   * @param type client type
   * @param n name
   * @return display name
   */
  public static String displayNameOf(ClientType type, PersonName n) {
    if (type == ClientType.CORPORATE) {
      return n.corporateName().trim();
    }
    String rest =
        Stream.of(n.firstName(), n.middleName(), n.suffix())
            .filter(s -> s != null && !s.isBlank())
            .map(String::trim)
            .collect(Collectors.joining(" "));
    return n.lastName().trim() + ", " + rest;
  }

  /**
   * Records KYC verification (checker, not the maker of the last change).
   *
   * @param verifier user
   * @param when time
   * @param reviewDue next periodic review date
   */
  public void verifyKyc(String verifier, Instant when, LocalDate reviewDue) {
    this.kycStatus = KycStatus.VERIFIED;
    this.kycVerifiedBy = verifier;
    this.kycVerifiedAt = when;
    this.kycReviewDue = reviewDue;
  }

  /**
   * Sets the KYC status (documents received, review due...).
   *
   * @param newStatus status
   */
  public void setKycStatus(KycStatus newStatus) {
    this.kycStatus = newStatus;
  }

  /**
   * Converts the prospect into a confirmed client (BRNB.101); the prospect code is kept.
   *
   * @param code client code
   * @param party sub-ledger party code
   * @param user confirming user
   * @param when time
   */
  public void confirm(String code, String party, String user, Instant when) {
    if (status != ClientStatus.PROSPECT) {
      throw new BusinessRuleException("CLIENT_NOT_PROSPECT", "Only a prospect can be confirmed");
    }
    this.clientCode = code;
    this.partyCode = party;
    this.status = ClientStatus.CONFIRMED;
    this.confirmedBy = user;
    this.confirmedAt = when;
  }

  /**
   * The code shown to users: client code once confirmed, prospect code before.
   *
   * @return code
   */
  public String getCode() {
    return clientCode != null ? clientCode : prospectCode;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getProspectCode() {
    return prospectCode;
  }

  public String getClientCode() {
    return clientCode;
  }

  public ClientStatus getStatus() {
    return status;
  }

  public ClientType getClientType() {
    return clientType;
  }

  public String getLastName() {
    return lastName;
  }

  public String getFirstName() {
    return firstName;
  }

  public String getMiddleName() {
    return middleName;
  }

  public String getSuffix() {
    return suffix;
  }

  public String getCorporateName() {
    return corporateName;
  }

  public String getDisplayName() {
    return displayName;
  }

  public LocalDate getBirthDate() {
    return birthDate;
  }

  public String getTin() {
    return tin;
  }

  public String getIdType() {
    return idType;
  }

  public String getIdNumber() {
    return idNumber;
  }

  public String getEmail() {
    return email;
  }

  public String getMobile() {
    return mobile;
  }

  public String getPhone() {
    return phone;
  }

  public String getAddressLine() {
    return addressLine;
  }

  public String getCity() {
    return city;
  }

  public String getProvince() {
    return province;
  }

  public String getPostalCode() {
    return postalCode;
  }

  public String getMarketSegment() {
    return marketSegment;
  }

  public boolean isBankClient() {
    return bankClient;
  }

  public String getBankCif() {
    return bankCif;
  }

  public String getPartyCode() {
    return partyCode;
  }

  public KycStatus getKycStatus() {
    return kycStatus;
  }

  public String getKycVerifiedBy() {
    return kycVerifiedBy;
  }

  public Instant getKycVerifiedAt() {
    return kycVerifiedAt;
  }

  public LocalDate getKycReviewDue() {
    return kycReviewDue;
  }

  public String getConfirmedBy() {
    return confirmedBy;
  }

  public Instant getConfirmedAt() {
    return confirmedAt;
  }

  public String getRiskRating() {
    return riskRating;
  }

  public String getOnboardingStage() {
    return onboardingStage;
  }

  public String getKycSubmittedBy() {
    return kycSubmittedBy;
  }

  public Instant getKycSubmittedAt() {
    return kycSubmittedAt;
  }

  public String getDeactivationReason() {
    return deactivationReason;
  }

  public String getDeactivationNote() {
    return deactivationNote;
  }

  public String getDeactivatedBy() {
    return deactivatedBy;
  }

  public Instant getDeactivatedAt() {
    return deactivatedAt;
  }
}
