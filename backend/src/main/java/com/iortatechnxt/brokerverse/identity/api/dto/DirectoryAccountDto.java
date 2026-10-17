package com.iortatechnxt.brokerverse.identity.api.dto;

import com.iortatechnxt.brokerverse.identity.domain.DirectoryAccount;
import com.iortatechnxt.brokerverse.identity.domain.DirectoryStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * An account of the Enterprise SSO directory, as shown before a user is created from it and as kept
 * in the simulator.
 *
 * @param windowsId Windows ID
 * @param userId user ID
 * @param email AD e-mail
 * @param firstName first name
 * @param lastName last name
 * @param displayName display name
 * @param adGroup AD group
 * @param status AD status
 * @param teamLeaderName team leader's name
 * @param teamHeadName team head's name
 * @param sectionHeadName section head's name
 * @param unitHeadName unit head's name
 * @param unitSegment unit / segment
 * @param department department
 * @param location location
 * @param uidmRequestNo UIDM request number
 */
public record DirectoryAccountDto(
    @NotBlank @Size(max = 50) String windowsId,
    @NotBlank @Size(max = 50) String userId,
    @NotBlank @Size(max = 120) String email,
    @NotBlank @Size(max = 60) String firstName,
    @NotBlank @Size(max = 60) String lastName,
    @Size(max = 120) String displayName,
    @Size(max = 120) String adGroup,
    DirectoryStatus status,
    @Size(max = 120) String teamLeaderName,
    @Size(max = 120) String teamHeadName,
    @Size(max = 120) String sectionHeadName,
    @Size(max = 120) String unitHeadName,
    @Size(max = 120) String unitSegment,
    @Size(max = 120) String department,
    @Size(max = 120) String location,
    @Size(max = 40) String uidmRequestNo) {

  /**
   * Maps an account.
   *
   * @param a account
   * @return dto
   */
  public static DirectoryAccountDto from(DirectoryAccount a) {
    return new DirectoryAccountDto(
        a.windowsId(),
        a.userId(),
        a.email(),
        a.firstName(),
        a.lastName(),
        a.fullName(),
        a.adGroup(),
        a.status(),
        a.hierarchy().teamLeader(),
        a.hierarchy().teamHead(),
        a.hierarchy().sectionHead(),
        a.hierarchy().unitHead(),
        a.organisation().unitSegment(),
        a.organisation().department(),
        a.organisation().location(),
        a.uidmRequestNo());
  }

  /**
   * The account.
   *
   * @return account
   */
  public DirectoryAccount toAccount() {
    return new DirectoryAccount(
        windowsId.trim(),
        userId.trim(),
        email.trim(),
        firstName,
        lastName,
        displayName,
        adGroup,
        status,
        new DirectoryAccount.Hierarchy(teamLeaderName, teamHeadName, sectionHeadName, unitHeadName),
        new DirectoryAccount.Organisation(unitSegment, department, location),
        uidmRequestNo,
        null);
  }
}
