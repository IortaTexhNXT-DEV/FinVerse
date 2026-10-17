package com.iortatechnxt.brokerverse.migration.mapping.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.migration.common.service.MigrationCodes;
import com.iortatechnxt.brokerverse.migration.common.service.MigrationWorkflow;
import com.iortatechnxt.brokerverse.migration.mapping.domain.CodeMapEntry;
import com.iortatechnxt.brokerverse.migration.mapping.domain.CodeMapEntry.EntryData;
import com.iortatechnxt.brokerverse.migration.mapping.domain.CodeMapEntryRepository;
import com.iortatechnxt.brokerverse.migration.mapping.domain.CodeMapSet;
import com.iortatechnxt.brokerverse.migration.mapping.domain.CodeMapSetRepository;
import com.iortatechnxt.brokerverse.migration.mapping.domain.CodeMapVersion;
import com.iortatechnxt.brokerverse.migration.mapping.domain.CodeMapVersionRepository;
import com.iortatechnxt.brokerverse.migration.mapping.domain.MapVersionStatus;
import com.iortatechnxt.brokerverse.workflow.domain.CaseRecord;
import java.time.Clock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Versioned code maps (BRID 3.1; FR-DM-011): the Data Steward creates a DRAFT version (empty, a
 * copy of the approved version, or imported from Excel), edits its entries and submits it; the
 * business owner compares it with the approved version and approves or returns it, never as the
 * submitter. On approval every MAP target must exist and be active in BIBS, and the previous
 * approved version is superseded.
 */
@Service
@Transactional
public class CodeMapService {

  private static final String LINK = "/migration/maps?set=";

  private final CodeMapSetRepository sets;
  private final CodeMapVersionRepository versions;
  private final CodeMapEntryRepository entries;
  private final TargetCodes targets;
  private final MigrationWorkflow workflow;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param sets map sets
   * @param versions versions
   * @param entries entries
   * @param targets target code checks
   * @param workflow workflow cases
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  public CodeMapService(
      CodeMapSetRepository sets,
      CodeMapVersionRepository versions,
      CodeMapEntryRepository entries,
      TargetCodes targets,
      MigrationWorkflow workflow,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.sets = sets;
    this.versions = versions;
    this.entries = entries;
    this.targets = targets;
    this.workflow = workflow;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Every map set.
   *
   * @return sets
   */
  @Transactional(readOnly = true)
  public List<CodeMapSet> sets() {
    return sets.findAllByOrderByCodeAsc();
  }

  /**
   * A map set.
   *
   * @param code set code
   * @return set
   */
  @Transactional(readOnly = true)
  public CodeMapSet set(String code) {
    return sets.findByCode(code)
        .orElseThrow(() -> new ResourceNotFoundException("CodeMapSet", code));
  }

  /**
   * The versions of a set, newest first.
   *
   * @param setCode set
   * @return versions
   */
  @Transactional(readOnly = true)
  public List<CodeMapVersion> versions(String setCode) {
    return versions.findBySetCodeOrderByVersionNoDesc(setCode);
  }

  /**
   * A version.
   *
   * @param id version id
   * @return version
   */
  @Transactional(readOnly = true)
  public CodeMapVersion version(Long id) {
    return versions
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException(MigrationCodes.ENTITY_MAP_VERSION, id));
  }

  /**
   * The approved version of a set.
   *
   * @param setCode set
   * @return version
   */
  @Transactional(readOnly = true)
  public Optional<CodeMapVersion> approved(String setCode) {
    return versions.findBySetCodeAndStatus(setCode, MapVersionStatus.APPROVED).stream().findFirst();
  }

  /**
   * The entries of a version.
   *
   * @param versionId version
   * @return entries
   */
  @Transactional(readOnly = true)
  public List<CodeMapEntry> entries(Long versionId) {
    return entries.findByVersionIdOrderBySourceSystemAscLegacyCodeAscIdAsc(versionId);
  }

  /**
   * Creates a DRAFT version, empty or copied from the approved version.
   *
   * @param companyId company of the approval work case
   * @param setCode set
   * @param copyApproved copy the entries of the approved version
   * @param comment comment
   * @return the draft
   */
  public CodeMapVersion createDraft(
      Long companyId, String setCode, boolean copyApproved, String comment) {
    CodeMapSet set = set(setCode);
    if (!versions.findBySetCodeAndStatus(setCode, MapVersionStatus.DRAFT).isEmpty()
        || !versions.findBySetCodeAndStatus(setCode, MapVersionStatus.SUBMITTED).isEmpty()) {
      throw new BusinessRuleException(
          "MIG_MAP_DRAFT_EXISTS",
          "Set " + setCode + " already has a version being edited or waiting for approval");
    }
    CodeMapVersion draft =
        versions.save(new CodeMapVersion(setCode, versions.maxVersionNo(setCode) + 1, comment));
    if (copyApproved) {
      approved(setCode)
          .ifPresent(
              a ->
                  entries(a.getId())
                      .forEach(e -> entries.save(new CodeMapEntry(draft.getId(), e.data()))));
    }
    workflow.open(
        companyId,
        MigrationCodes.WF_MAP_VERSION,
        new CaseRecord(
            MigrationCodes.ENTITY_MAP_VERSION,
            String.valueOf(draft.getId()),
            draft.label(),
            set.getName(),
            LINK + setCode,
            null));
    audit.record(
        MigrationCodes.ENTITY_MAP_VERSION, draft.label(), AuditAction.CREATE, "Draft created");
    return draft;
  }

  /**
   * Creates a DRAFT version from imported entries.
   *
   * @param companyId company of the approval work case
   * @param setCode set
   * @param imported entries
   * @param comment comment
   * @return the draft
   */
  public CodeMapVersion importDraft(
      Long companyId, String setCode, List<EntryData> imported, String comment) {
    CodeMapVersion draft = createDraft(companyId, setCode, false, comment);
    Set<String> keys = new HashSet<>();
    for (EntryData data : imported) {
      data.requireValid();
      CodeMapEntry entry = new CodeMapEntry(draft.getId(), data);
      if (!keys.add(entry.key())) {
        throw duplicate(entry);
      }
      entries.save(entry);
    }
    return draft;
  }

  /**
   * Adds or changes an entry of a draft.
   *
   * @param versionId draft
   * @param entryId entry, null to add
   * @param data entry data
   * @return the entry
   */
  public CodeMapEntry saveEntry(Long versionId, Long entryId, EntryData data) {
    CodeMapVersion version = version(versionId);
    version.requireDraft();
    data.requireValid();
    CodeMapEntry entry =
        entryId == null
            ? new CodeMapEntry(versionId, data)
            : entries
                .findById(entryId)
                .filter(e -> e.getVersionId().equals(versionId))
                .orElseThrow(() -> new ResourceNotFoundException("CodeMapEntry", entryId));
    if (entryId != null) {
      entry.apply(data);
    }
    String key = entry.key();
    boolean clash =
        entries(versionId).stream()
            .anyMatch(e -> e.key().equals(key) && !e.getId().equals(entry.getId()));
    if (clash) {
      throw duplicate(entry);
    }
    return entries.save(entry);
  }

  /**
   * Removes an entry of a draft.
   *
   * @param versionId draft
   * @param entryId entry
   */
  public void deleteEntry(Long versionId, Long entryId) {
    version(versionId).requireDraft();
    entries
        .findById(entryId)
        .filter(e -> e.getVersionId().equals(versionId))
        .ifPresent(entries::delete);
  }

  private static BusinessRuleException duplicate(CodeMapEntry e) {
    return new BusinessRuleException(
        "MIG_MAP_DUPLICATE",
        "Legacy code " + e.getLegacyCode() + " of " + e.getSourceSystem() + " is mapped twice");
  }

  /**
   * Submits a draft to the business owner.
   *
   * @param versionId draft
   * @return the version
   */
  public CodeMapVersion submit(Long versionId) {
    CodeMapVersion version = version(versionId);
    if (entries.countByVersionId(versionId) == 0) {
      throw new BusinessRuleException("MIG_MAP_EMPTY", "Add at least one entry before submitting");
    }
    version.submit(currentUser.username(), clock.instant());
    workflow.move(MigrationCodes.ENTITY_MAP_VERSION, String.valueOf(versionId), "submit", null);
    audit.record(
        MigrationCodes.ENTITY_MAP_VERSION, version.label(), AuditAction.SUBMIT, "Submitted");
    return version;
  }

  /**
   * Approves a submitted version (gate G2 of its objects): every MAP target must exist; the
   * previous approved version is superseded.
   *
   * @param versionId version
   * @param comment comment
   * @return the approval result with the values to create
   */
  public Approval approve(Long versionId, String comment) {
    CodeMapVersion version = version(versionId);
    CodeMapSet set = set(version.getSetCode());
    List<CodeMapEntry> list = entries(versionId);
    CodeMapEntry.Targets found = CodeMapEntry.targetsOf(list);
    Set<String> missing = targets.missing(set, found.mapped());
    if (!missing.isEmpty()) {
      throw new BusinessRuleException(
          "MIG_MAP_TARGET_MISSING",
          "Target code " + new TreeSet<>(missing) + " does not exist in " + set.getTargetDomain());
    }
    String user = currentUser.username();
    List<CodeMapVersion> previous =
        versions.findBySetCodeAndStatus(version.getSetCode(), MapVersionStatus.APPROVED).stream()
            .filter(p -> !p.getId().equals(versionId))
            .toList();
    for (CodeMapVersion p : previous) {
      p.supersede();
      versions.saveAndFlush(p);
      workflow.move(
          MigrationCodes.ENTITY_MAP_VERSION,
          String.valueOf(p.getId()),
          "supersede",
          version.label());
    }
    version.approve(user, clock.instant());
    versions.saveAndFlush(version);
    workflow.move(MigrationCodes.ENTITY_MAP_VERSION, String.valueOf(versionId), "approve", comment);
    audit.record(
        MigrationCodes.ENTITY_MAP_VERSION,
        version.label(),
        AuditAction.AUTHORIZE,
        "Approved with " + list.size() + " entries" + (comment == null ? "" : ": " + comment));
    return new Approval(version, found.created());
  }

  /**
   * Returns a submitted version to DRAFT.
   *
   * @param versionId version
   * @param reason reason
   * @return the version
   */
  public CodeMapVersion returnVersion(Long versionId, String reason) {
    CodeMapVersion version = version(versionId);
    version.returnWith(currentUser.username(), reason);
    workflow.move(MigrationCodes.ENTITY_MAP_VERSION, String.valueOf(versionId), "return", reason);
    audit.record(MigrationCodes.ENTITY_MAP_VERSION, version.label(), AuditAction.REJECT, reason);
    return version;
  }

  /**
   * Differences of a version against the approved version of its set.
   *
   * @param versionId version
   * @return added, changed and removed entries
   */
  @Transactional(readOnly = true)
  public Diff diff(Long versionId) {
    CodeMapVersion version = version(versionId);
    Map<String, CodeMapEntry> base = new HashMap<>();
    approved(version.getSetCode())
        .filter(a -> !a.getId().equals(versionId))
        .ifPresent(a -> entries(a.getId()).forEach(e -> base.put(e.key(), e)));
    List<CodeMapEntry> added = new ArrayList<>();
    List<CodeMapEntry> changed = new ArrayList<>();
    for (CodeMapEntry e : entries(versionId)) {
      CodeMapEntry old = base.remove(e.key());
      if (old == null) {
        added.add(e);
      } else if (!old.data().equals(e.data())) {
        changed.add(e);
      }
    }
    return new Diff(added, changed, new ArrayList<>(base.values()));
  }

  /**
   * Result of an approval.
   *
   * @param version approved version
   * @param toCreate reference values to create through the reference-data load
   */
  public record Approval(CodeMapVersion version, List<String> toCreate) {}

  /**
   * Differences of a version.
   *
   * @param added entries not in the approved version
   * @param changed entries changed from the approved version
   * @param removed approved entries missing from the version
   */
  public record Diff(
      List<CodeMapEntry> added, List<CodeMapEntry> changed, List<CodeMapEntry> removed) {}
}
