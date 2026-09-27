package com.iortatechnxt.brokerverse.eb.member.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.eb.domain.EbCodes;
import com.iortatechnxt.brokerverse.eb.domain.EbMember;
import com.iortatechnxt.brokerverse.eb.domain.EbMemberRepository;
import com.iortatechnxt.brokerverse.eb.domain.EbProgramme;
import com.iortatechnxt.brokerverse.eb.domain.EbRosterVersion;
import com.iortatechnxt.brokerverse.eb.domain.EbRosterVersionRepository;
import com.iortatechnxt.brokerverse.eb.service.EbRecords;
import com.iortatechnxt.brokerverse.messaging.domain.Notice;
import com.iortatechnxt.brokerverse.messaging.service.NotificationService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The member roster of a programme (BRID-013, 014; FR-EB-054): the master list the client sends is
 * loaded by the AO through the bulk upload {@code EB_MASTERLIST} as a STAGED version (one upload,
 * one version); the AO reviews the differences with the accepted roster and accepts it (the earlier
 * accepted version is superseded and stays readable) or rejects it. One employee number per version.
 */
@Service
@Transactional
public class RosterService {

  private final EbRosterVersionRepository versions;
  private final EbMemberRepository members;
  private final EbRecords records;
  private final NotificationService notifications;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param versions roster versions
   * @param members members
   * @param records programme look-up
   * @param notifications in-app notices
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  public RosterService(
      EbRosterVersionRepository versions,
      EbMemberRepository members,
      EbRecords records,
      NotificationService notifications,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.versions = versions;
    this.members = members;
    this.records = records;
    this.notifications = notifications;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Adds a member of a master list row to the staged version of its upload (created with the first
   * row).
   *
   * @param programme programme
   * @param policyYear policy year
   * @param uploadNo upload number
   * @param row employee number, member data and effective date
   * @return the staged version
   */
  public EbRosterVersion stage(
      EbProgramme programme, int policyYear, String uploadNo, StagedMember row) {
    EbRosterVersion version =
        versions.findBySourceRef(uploadNo).orElseGet(() -> newVersion(programme, policyYear, uploadNo));
    if (members.findByRosterVersionIdAndEmployeeNoIgnoreCase(version.getId(), row.employeeNo()).isPresent()) {
      throw new BusinessRuleException(
          "EB_MEMBER_TWICE", "Employee " + row.employeeNo() + " appears twice");
    }
    members.save(new EbMember(version, row.employeeNo(), row.data(), row.effectiveFrom()));
    version.counted();
    return version;
  }

  private EbRosterVersion newVersion(EbProgramme programme, int policyYear, String uploadNo) {
    int next =
        versions
            .findFirstByProgrammeIdAndPolicyYearOrderByVersionNoDesc(programme.getId(), policyYear)
            .map(v -> v.getVersionNo() + 1)
            .orElse(1);
    EbRosterVersion version =
        versions.saveAndFlush(new EbRosterVersion(programme, policyYear, next, uploadNo));
    notifications.notifyUser(
        programme.getAccountOfficer(),
        new Notice(
            programme.getProgrammeNo() + ": master list to review",
            "Roster version " + next + " of " + policyYear + " is staged for " + programme.getName(),
            EbCodes.PROGRAMME_LINK + programme.getId() + "?tab=members",
            EbCodes.ENTITY_PROGRAMME,
            programme.getId().toString()),
        EbCodes.EVENT_ROSTER_STAGED);
    return version;
  }

  /**
   * Accepts a staged version: it becomes the roster of its policy year.
   *
   * @param companyId company
   * @param versionId staged version
   * @return the version
   */
  public EbRosterVersion accept(Long companyId, Long versionId) {
    EbRosterVersion version = require(companyId, versionId);
    if (version.getHeadcount() == 0) {
      throw new BusinessRuleException("EB_ROSTER_EMPTY", "The master list has no member");
    }
    versions
        .findFirstByProgrammeIdAndPolicyYearAndStatusOrderByVersionNoDesc(
            version.getProgrammeId(), version.getPolicyYear(), EbRosterVersion.Status.ACCEPTED)
        .ifPresent(
            v -> {
              v.supersede();
              versions.saveAndFlush(v);
            });
    version.accept(currentUser.username(), clock.instant());
    audit.record(
        EbCodes.ENTITY_ROSTER,
        versionId,
        AuditAction.AUTHORIZE,
        "Roster " + version.getPolicyYear() + " version " + version.getVersionNo() + " accepted ("
            + version.getHeadcount() + " members)");
    return version;
  }

  /**
   * Rejects a staged version.
   *
   * @param companyId company
   * @param versionId staged version
   * @param reason why
   * @return the version
   */
  public EbRosterVersion reject(Long companyId, Long versionId, String reason) {
    EbRosterVersion version = require(companyId, versionId);
    version.reject(reason, currentUser.username(), clock.instant());
    audit.record(
        EbCodes.ENTITY_ROSTER,
        versionId,
        AuditAction.REJECT,
        "Roster version " + version.getVersionNo() + " rejected: " + version.getRejectReason());
    return version;
  }

  /**
   * The roster versions of a programme.
   *
   * @param companyId company
   * @param programmeId programme
   * @return versions, latest first
   */
  @Transactional(readOnly = true)
  public List<EbRosterVersion> versions(Long companyId, Long programmeId) {
    return versions.findByProgrammeIdOrderByPolicyYearDescVersionNoDesc(
        records.programme(companyId, programmeId).getId());
  }

  /**
   * Members of a version.
   *
   * @param companyId company
   * @param versionId version
   * @param text employee number or name fragment, may be null
   * @param pageable page
   * @return members
   */
  @Transactional(readOnly = true)
  public Page<EbMember> members(Long companyId, Long versionId, String text, Pageable pageable) {
    EbRosterVersion version = require(companyId, versionId);
    String q = text == null || text.isBlank() ? null : text.strip().toLowerCase(Locale.ROOT);
    return members.search(version.getId(), q, pageable);
  }

  /**
   * The differences of a version with the accepted roster of its year (review before accepting).
   *
   * @param companyId company
   * @param versionId version
   * @return added, removed and changed counts
   */
  @Transactional(readOnly = true)
  public Differences differences(Long companyId, Long versionId) {
    EbRosterVersion version = require(companyId, versionId);
    Map<String, EbMember> staged = byEmployee(version.getId());
    Optional<EbRosterVersion> accepted =
        versions.findFirstByProgrammeIdAndPolicyYearAndStatusOrderByVersionNoDesc(
            version.getProgrammeId(), version.getPolicyYear(), EbRosterVersion.Status.ACCEPTED);
    Map<String, EbMember> current =
        accepted.filter(a -> !a.getId().equals(version.getId())).map(a -> byEmployee(a.getId())).orElse(Map.of());
    Set<String> added = staged.keySet().stream().filter(k -> !current.containsKey(k)).collect(Collectors.toSet());
    Set<String> removed = current.keySet().stream().filter(k -> !staged.containsKey(k)).collect(Collectors.toSet());
    long changed =
        staged.entrySet().stream()
            .filter(e -> current.containsKey(e.getKey()))
            .filter(e -> !e.getValue().getPlanCode().equals(current.get(e.getKey()).getPlanCode()))
            .count();
    return new Differences(staged.size(), current.size(), added.size(), removed.size(), (int) changed);
  }

  private Map<String, EbMember> byEmployee(Long versionId) {
    return members.findByRosterVersionIdOrderByEmployeeNoAsc(versionId).stream()
        .collect(Collectors.toMap(m -> m.getEmployeeNo().toUpperCase(Locale.ROOT), Function.identity(), (a, b) -> a));
  }

  /**
   * The accepted roster of a programme and year.
   *
   * @param programmeId programme
   * @param policyYear policy year
   * @return version
   */
  @Transactional(readOnly = true)
  public Optional<EbRosterVersion> accepted(Long programmeId, int policyYear) {
    return versions.findFirstByProgrammeIdAndPolicyYearAndStatusOrderByVersionNoDesc(
        programmeId, policyYear, EbRosterVersion.Status.ACCEPTED);
  }

  /**
   * A version of a company.
   *
   * @param companyId company
   * @param versionId version
   * @return version
   */
  @Transactional(readOnly = true)
  public EbRosterVersion require(Long companyId, Long versionId) {
    return versions
        .findById(versionId)
        .filter(v -> v.getCompanyId().equals(companyId))
        .orElseThrow(() -> new ResourceNotFoundException(EbCodes.ENTITY_ROSTER, versionId));
  }

  /**
   * A master list row.
   *
   * @param employeeNo employee number
   * @param data member data
   * @param effectiveFrom effective date
   */
  public record StagedMember(String employeeNo, EbMember.Data data, LocalDate effectiveFrom) {}

  /**
   * Differences of a staged version with the accepted roster.
   *
   * @param headcount members of the version
   * @param currentHeadcount members of the accepted roster
   * @param added employees not on the accepted roster
   * @param removed employees of the accepted roster not in the version
   * @param planChanges employees whose plan differs
   */
  public record Differences(
      int headcount, int currentHeadcount, int added, int removed, int planChanges) {}
}
