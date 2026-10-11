package com.iortatechnxt.brokerverse.identity.domain;

import java.util.List;

/**
 * An account as the Enterprise SSO platform and UIDM-ISC describe it (BDOI FRS FRUM.002.01: the
 * user information retrieved).
 *
 * @param windowsId Windows ID (the identity of the account)
 * @param userId user ID in the system (a letter followed by nine digits)
 * @param email AD e-mail
 * @param firstName first name
 * @param lastName last name
 * @param displayName display name
 * @param adGroup AD group
 * @param status AD status
 * @param hierarchy team leader, team head, section head and unit head names
 * @param organisation unit / segment, department and location
 * @param uidmRequestNo UIDM request number
 * @param groupProfiles group profiles (role codes) named by UIDM-ISC, used only with the setting
 *     UAM_PROVISIONING_ROLES
 */
public record DirectoryAccount(
    String windowsId,
    String userId,
    String email,
    String firstName,
    String lastName,
    String displayName,
    String adGroup,
    DirectoryStatus status,
    Hierarchy hierarchy,
    Organisation organisation,
    String uidmRequestNo,
    List<String> groupProfiles) {

  /** Defaults. */
  public DirectoryAccount {
    hierarchy = hierarchy == null ? new Hierarchy(null, null, null, null) : hierarchy;
    organisation = organisation == null ? new Organisation(null, null, null) : organisation;
    groupProfiles = groupProfiles == null ? List.of() : List.copyOf(groupProfiles);
    status = status == null ? DirectoryStatus.ACTIVE : status;
  }

  /**
   * The full name of the user: the display name, or the first and last names.
   *
   * @return name
   */
  public String fullName() {
    if (displayName != null && !displayName.isBlank()) {
      return displayName.trim();
    }
    String name = (firstName == null ? "" : firstName) + " " + (lastName == null ? "" : lastName);
    return name.isBlank() ? userId : name.trim();
  }

  /**
   * The same account with another status.
   *
   * @param newStatus status
   * @return account
   */
  public DirectoryAccount withStatus(DirectoryStatus newStatus) {
    return new DirectoryAccount(
        windowsId,
        userId,
        email,
        firstName,
        lastName,
        displayName,
        adGroup,
        newStatus,
        hierarchy,
        organisation,
        uidmRequestNo,
        groupProfiles);
  }

  /**
   * The names of the user's superiors.
   *
   * @param teamLeader team leader's name
   * @param teamHead team head's name
   * @param sectionHead section head's name
   * @param unitHead unit head's name
   */
  public record Hierarchy(
      String teamLeader, String teamHead, String sectionHead, String unitHead) {}

  /**
   * Where the user works.
   *
   * @param unitSegment unit / segment
   * @param department department
   * @param location location (for example CCO, Makati)
   */
  public record Organisation(String unitSegment, String department, String location) {}
}
