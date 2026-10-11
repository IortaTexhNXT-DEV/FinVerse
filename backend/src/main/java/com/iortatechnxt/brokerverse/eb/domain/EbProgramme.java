package com.iortatechnxt.brokerverse.eb.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

/**
 * An Employee Benefits programme of a client (BRID-006, 022.01; design 4.2): the benefit lines
 * (HMO, GLI, GPA) with the incumbent insurer and current policy, the team, the funding, the HR
 * contacts and the account officer. Numbered {@code EBP-<yyyy>-nnnnnn}; one cycle per policy year
 * ({@link EbCycle}). Created by wave E1-B; read by every EB wave.
 */
@Entity
@Table(name = "eb_programme")
public class EbProgramme extends BaseEntity {

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "programme_no", nullable = false, length = 30, updatable = false)
  private String programmeNo;

  @Column(name = "client_id", nullable = false)
  private Long clientId;

  @Column(name = "client_code", nullable = false, length = 30)
  private String clientCode;

  @Column(name = "client_name", nullable = false, length = 250)
  private String clientName;

  @Column(nullable = false, length = 200)
  private String name;

  @Column(name = "team_code", nullable = false, length = 40)
  private String teamCode;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private EbFunding funding;

  @Column(name = "account_officer", nullable = false, length = 50)
  private String accountOfficer;

  @Column(name = "sales_unit", length = 20)
  private String salesUnit;

  @Column(name = "renewal_eligible", nullable = false)
  private boolean renewalEligible;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private EbProgrammeStatus status = EbProgrammeStatus.PROSPECT;

  @OneToMany(mappedBy = "programme", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("lineNo")
  private final List<EbProgrammeLine> lines = new ArrayList<>();

  @OneToMany(mappedBy = "programme", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("id")
  private final List<EbProgrammeContact> contacts = new ArrayList<>();

  protected EbProgramme() {}

  /**
   * Creates a programme in status PROSPECT.
   *
   * @param companyId company
   * @param programmeNo programme number (immutable)
   * @param client client (prospect or confirmed)
   * @param profile name, team, funding, account officer, sales unit, renewal flag
   */
  public EbProgramme(Long companyId, String programmeNo, ClientRef client, Profile profile) {
    this.companyId = companyId;
    this.programmeNo = programmeNo;
    this.clientId = client.id();
    this.clientCode = client.code();
    this.clientName = client.name();
    apply(profile);
  }

  /**
   * Replaces the maintainable data of the programme.
   *
   * @param profile name, team, funding, account officer, sales unit, renewal flag
   */
  public void update(Profile profile) {
    apply(profile);
  }

  private void apply(Profile profile) {
    this.name = profile.name();
    this.teamCode = profile.teamCode();
    this.funding = profile.funding();
    this.accountOfficer = profile.accountOfficer();
    this.salesUnit = profile.salesUnit();
    this.renewalEligible = profile.renewalEligible();
  }

  /**
   * Adds a benefit line, numbered after the existing ones.
   *
   * @param data line data
   * @return the line
   */
  public EbProgrammeLine addLine(EbProgrammeLine.Data data) {
    EbProgrammeLine line = new EbProgrammeLine(this, lines.size() + 1, data);
    lines.add(line);
    return line;
  }

  /**
   * Adds a client contact.
   *
   * @param data contact data
   * @return the contact
   */
  public EbProgrammeContact addContact(EbProgrammeContact.Data data) {
    EbProgrammeContact contact = new EbProgrammeContact(this, data);
    contacts.add(contact);
    return contact;
  }

  /**
   * Changes the status (placement makes a prospect ACTIVE; an outcome may make it LAPSED or LOST).
   *
   * @param newStatus status
   */
  public void markStatus(EbProgrammeStatus newStatus) {
    this.status = newStatus;
  }

  /**
   * The line with a number.
   *
   * @param lineNo line number
   * @return line
   */
  public EbProgrammeLine line(int lineNo) {
    return lines.stream()
        .filter(l -> l.getLineNo() == lineNo)
        .findFirst()
        .orElseThrow(
            () ->
                new BusinessRuleException(
                    "EB_LINE_NOT_FOUND", "Programme " + programmeNo + " has no line " + lineNo));
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getProgrammeNo() {
    return programmeNo;
  }

  public Long getClientId() {
    return clientId;
  }

  public String getClientCode() {
    return clientCode;
  }

  public String getClientName() {
    return clientName;
  }

  public String getName() {
    return name;
  }

  public String getTeamCode() {
    return teamCode;
  }

  public EbFunding getFunding() {
    return funding;
  }

  public String getAccountOfficer() {
    return accountOfficer;
  }

  public String getSalesUnit() {
    return salesUnit;
  }

  public boolean isRenewalEligible() {
    return renewalEligible;
  }

  public EbProgrammeStatus getStatus() {
    return status;
  }

  public List<EbProgrammeLine> getLines() {
    return List.copyOf(lines);
  }

  public List<EbProgrammeContact> getContacts() {
    return List.copyOf(contacts);
  }

  /**
   * The client of a programme (plain values of the crm client).
   *
   * @param id client id
   * @param code client or prospect code
   * @param name display name
   */
  public record ClientRef(Long id, String code, String name) {}

  /**
   * Maintainable data of a programme.
   *
   * @param name programme name
   * @param teamCode team (list EB_TEAM)
   * @param funding employer or voluntary
   * @param accountOfficer AO user name
   * @param salesUnit sales team of the AO
   * @param renewalEligible whether the RA job renews it
   */
  public record Profile(
      String name,
      String teamCode,
      EbFunding funding,
      String accountOfficer,
      String salesUnit,
      boolean renewalEligible) {}
}
