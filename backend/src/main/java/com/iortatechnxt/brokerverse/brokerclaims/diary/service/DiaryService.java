package com.iortatechnxt.brokerverse.brokerclaims.diary.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.brokerclaims.diary.domain.DiaryEntry;
import com.iortatechnxt.brokerverse.brokerclaims.diary.domain.DiaryEntryRepository;
import com.iortatechnxt.brokerverse.brokerclaims.domain.Claim;
import com.iortatechnxt.brokerverse.brokerclaims.domain.ClaimCodes;
import com.iortatechnxt.brokerverse.brokerclaims.status.service.ClaimAgeing;
import com.iortatechnxt.brokerverse.brokerclaims.status.service.ClaimLookup;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import com.iortatechnxt.brokerverse.messaging.domain.Notice;
import com.iortatechnxt.brokerverse.messaging.service.NotificationService;
import com.iortatechnxt.brokerverse.security.service.UserDirectory;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The diary of the claims handlers (BRCLM.022/034, NFR 15.08; FR-CL-052): entries per claim with
 * type, date, optional due date and assignee (a claims user; default the author), text up to 2,000
 * characters, marked done by the assignee or the author. An entry assigned to someone else notifies
 * that user. Entries are never deleted; closed claims accept them.
 */
@Service
@Transactional
public class DiaryService {

  /** Longest diary text. */
  public static final int MAX_TEXT = 2000;

  private static final String ENTITY = "BrokerClaimDiary";

  private final DiaryEntryRepository entries;
  private final ClaimLookup lookup;
  private final LovService lovs;
  private final UserDirectory directory;
  private final NotificationService notifications;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param entries diary entries
   * @param lookup claims of the company
   * @param lovs lists of values
   * @param directory users (assignees)
   * @param notifications in-app notifications
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  public DiaryService(
      DiaryEntryRepository entries,
      ClaimLookup lookup,
      LovService lovs,
      UserDirectory directory,
      NotificationService notifications,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.entries = entries;
    this.lookup = lookup;
    this.lovs = lovs;
    this.directory = directory;
    this.notifications = notifications;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Adds an entry to a claim.
   *
   * @param companyId company
   * @param claimId claim
   * @param input type, dates, assignee and text
   * @return the entry
   */
  public DiaryEntry add(Long companyId, Long claimId, DiaryInput input) {
    Claim claim = lookup.require(companyId, claimId);
    LocalDate today = ClaimAgeing.today(clock);
    String text = text(input.text());
    if (input.entryType() == null || input.entryType().isBlank()) {
      throw new BusinessRuleException("BCL_DIARY_TYPE_REQUIRED", "Select the type of entry");
    }
    lovs.requireValid(ClaimCodes.LOV_DIARY_TYPE, input.entryType(), today);
    LocalDate entryDate = input.entryDate() == null ? today : input.entryDate();
    if (input.dueDate() != null && input.dueDate().isBefore(entryDate)) {
      throw new BusinessRuleException(
          "BCL_DIARY_DUE_BEFORE_ENTRY", "The due date must be on or after the entry date");
    }
    String assignee = assignee(input.assignee());
    DiaryEntry entry =
        entries.save(
            new DiaryEntry(
                claim.getId(),
                input.entryType(),
                new DiaryEntry.Dates(entryDate, input.dueDate()),
                assignee,
                text));
    audit.record(
        ENTITY,
        entry.getId(),
        AuditAction.CREATE,
        claim.getClaimNo() + ": " + input.entryType() + " for " + assignee);
    notifyAssignee(claim, entry);
    return entry;
  }

  /**
   * The entries of a claim, newest first.
   *
   * @param companyId company
   * @param claimId claim
   * @return entries
   */
  @Transactional(readOnly = true)
  public List<DiaryEntry> entries(Long companyId, Long claimId) {
    Claim claim = lookup.require(companyId, claimId);
    return entries.findByClaimIdOrderByEntryDateDescIdDesc(claim.getId());
  }

  /**
   * Marks an entry done (the assignee or the author).
   *
   * @param companyId company
   * @param entryId entry
   * @param remark remark, may be null
   * @return the entry
   */
  public DiaryEntry markDone(Long companyId, Long entryId, String remark) {
    DiaryEntry entry =
        entries.findById(entryId).orElseThrow(() -> new ResourceNotFoundException(ENTITY, entryId));
    Claim claim = lookup.require(companyId, entry.getClaimId());
    if (entry.isDone()) {
      throw new BusinessRuleException("BCL_DIARY_DONE", "The diary entry is already done");
    }
    String me = currentUser.username();
    if (!CurrentUser.sameUser(me, entry.getAssignee())
        && !CurrentUser.sameUser(me, entry.getCreatedBy())) {
      throw new BusinessRuleException(
          "BCL_DIARY_NOT_YOURS", "Only the assignee or the author can complete this entry");
    }
    entry.markDone(me, clock.instant(), remark == null || remark.isBlank() ? null : remark);
    audit.record(ENTITY, entry.getId(), AuditAction.UPDATE, claim.getClaimNo() + ": done");
    return entry;
  }

  /**
   * Label of an entry type.
   *
   * @param type type code
   * @return label
   */
  @Transactional(readOnly = true)
  public String typeLabel(String type) {
    return lovs.label(ClaimCodes.LOV_DIARY_TYPE, type);
  }

  private static String text(String entered) {
    String text = entered == null ? "" : entered.strip();
    if (text.isEmpty()) {
      throw new BusinessRuleException("BCL_DIARY_TEXT_REQUIRED", "Enter the diary text");
    }
    if (text.length() > MAX_TEXT) {
      throw new BusinessRuleException(
          "BCL_DIARY_TEXT_TOO_LONG", "The diary text can have up to 2000 characters");
    }
    return text;
  }

  private String assignee(String requested) {
    if (requested == null || requested.isBlank()) {
      return currentUser.username();
    }
    boolean claimsUser =
        directory.usersWithPermission("BCL_VIEW").stream()
            .anyMatch(u -> CurrentUser.sameUser(u, requested));
    if (!claimsUser) {
      throw new BusinessRuleException(
          "BCL_DIARY_ASSIGNEE_INVALID", requested + " is not a claims user");
    }
    return requested.strip();
  }

  private void notifyAssignee(Claim claim, DiaryEntry entry) {
    if (CurrentUser.sameUser(entry.getAssignee(), currentUser.username())) {
      return;
    }
    notifications.notifyUser(
        entry.getAssignee(),
        new Notice(
            claim.getClaimNo() + ": diary entry for you",
            typeLabel(entry.getEntryType())
                + (entry.getDueDate() == null ? "" : " due " + entry.getDueDate())
                + " - "
                + entry.getText(),
            "/claims-handling/diary",
            ClaimCodes.ENTITY_TYPE,
            String.valueOf(claim.getId())));
  }

  /**
   * A new diary entry.
   *
   * @param entryType type ({@code BCL_DIARY_TYPE})
   * @param entryDate date of the activity (default today)
   * @param dueDate due date, may be null
   * @param assignee assignee (default the author)
   * @param text text
   */
  public record DiaryInput(
      String entryType, LocalDate entryDate, LocalDate dueDate, String assignee, String text) {}
}
