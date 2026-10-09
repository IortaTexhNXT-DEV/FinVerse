package com.iortatechnxt.brokerverse.identity.api.dto;

import com.iortatechnxt.brokerverse.identity.domain.DirectoryProfile;
import java.time.Instant;

/**
 * The directory details of a user and the latest synchronisation (BDOI FRS FRUM.002.01 and
 * FRUM.002.02).
 *
 * @param username user
 * @param windowsId Windows ID
 * @param adEmail AD e-mail
 * @param adGroup AD group
 * @param adStatus AD status
 * @param adSyncAt AD sync date and time
 * @param firstName first name
 * @param lastName last name
 * @param displayName display name
 * @param teamLeaderName team leader's name
 * @param teamHeadName team head's name
 * @param sectionHeadName section head's name
 * @param unitHeadName unit head's name
 * @param unitSegment unit / segment
 * @param department department
 * @param location location
 * @param uidmRequestNo UIDM request number
 * @param lastEventType type of the last event
 * @param lastEventAt time of the last event
 * @param lastEventSource source of the last event in words
 * @param syncStatus SYNCED or FAILED
 * @param syncMessage why the last synchronisation failed
 * @param createdBy who created the profile
 * @param createdAt when
 * @param updatedBy who changed it last
 * @param updatedAt when
 */
public record DirectoryProfileResponse(
    String username,
    String windowsId,
    String adEmail,
    String adGroup,
    String adStatus,
    Instant adSyncAt,
    String firstName,
    String lastName,
    String displayName,
    String teamLeaderName,
    String teamHeadName,
    String sectionHeadName,
    String unitHeadName,
    String unitSegment,
    String department,
    String location,
    String uidmRequestNo,
    String lastEventType,
    Instant lastEventAt,
    String lastEventSource,
    String syncStatus,
    String syncMessage,
    String createdBy,
    Instant createdAt,
    String updatedBy,
    Instant updatedAt) {

  /**
   * Maps a profile.
   *
   * @param p profile
   * @return response
   */
  public static DirectoryProfileResponse from(DirectoryProfile p) {
    return new DirectoryProfileResponse(
        p.getUsername(),
        p.getWindowsId(),
        p.getAdEmail(),
        p.getAdGroup(),
        p.getAdStatus() == null ? null : p.getAdStatus().name(),
        p.getAdSyncAt(),
        p.getFirstName(),
        p.getLastName(),
        p.getDisplayName(),
        p.getTeamLeaderName(),
        p.getTeamHeadName(),
        p.getSectionHeadName(),
        p.getUnitHeadName(),
        p.getUnitSegment(),
        p.getDepartment(),
        p.getLocation(),
        p.getUidmRequestNo(),
        p.getLastEventType() == null ? null : p.getLastEventType().name(),
        p.getLastEventAt(),
        p.getLastEventSource() == null ? null : p.getLastEventSource().label(),
        p.getSyncStatus(),
        p.getSyncMessage(),
        p.getCreatedBy(),
        p.getCreatedAt(),
        p.getUpdatedBy(),
        p.getUpdatedAt());
  }
}
