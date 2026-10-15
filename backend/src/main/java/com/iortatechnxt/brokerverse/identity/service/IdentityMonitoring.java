package com.iortatechnxt.brokerverse.identity.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.identity.domain.IdentityEvent;
import com.iortatechnxt.brokerverse.identity.domain.IdentityEventDismissal;
import com.iortatechnxt.brokerverse.identity.domain.IdentityEventDismissalRepository;
import com.iortatechnxt.brokerverse.identity.domain.IdentityEventRepository;
import com.iortatechnxt.brokerverse.identity.domain.IdentityEventStatus;
import com.iortatechnxt.brokerverse.system.domain.JobRun;
import com.iortatechnxt.brokerverse.system.domain.JobRunRepository;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Monitoring of the identity synchronisation on the Identity Sync screen: the state of the
 * connection with UIDM-ISC (last call, last success, errors of the last 24 hours, the last
 * synchronisation run), the dismissal of a refused event with a remark, and the reconciliation
 * mismatches with their resolution.
 */
@Service
@Transactional
public class IdentityMonitoring {

  private static final int REMARK = 500;
  private static final Duration ERROR_WINDOW = Duration.ofHours(24);
  private static final String MISMATCH_ENTITY = "IdentityMismatch";

  private final JdbcTemplate jdbc;
  private final IdentityEventRepository events;
  private final IdentityEventDismissalRepository dismissals;
  private final JobRunRepository runs;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param jdbc database
   * @param events provisioning events
   * @param dismissals dismissals of refused events
   * @param runs job runs (the last synchronisation)
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  public IdentityMonitoring(
      JdbcTemplate jdbc,
      IdentityEventRepository events,
      IdentityEventDismissalRepository dismissals,
      JobRunRepository runs,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.jdbc = jdbc;
    this.events = events;
    this.dismissals = dismissals;
    this.runs = runs;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * The state of the connection with UIDM-ISC.
   *
   * @return status
   */
  @Transactional(readOnly = true)
  public Status status() {
    Instant since = clock.instant().minus(ERROR_WINDOW);
    Instant lastCall =
        toInstant(
            jdbc.queryForObject(
                "select max(received_at) from idn_identity_event where source in ('UIDM_ISC',"
                    + " 'ENTERPRISE_SSO')",
                Timestamp.class));
    Instant lastSuccess =
        toInstant(
            jdbc.queryForObject(
                "select max(received_at) from idn_identity_event where source in ('UIDM_ISC',"
                    + " 'ENTERPRISE_SSO') and status in ('APPLIED', 'NO_CHANGE')",
                Timestamp.class));
    Integer errors =
        jdbc.queryForObject(
            "select count(*) from idn_identity_event where status in ('REFUSED', 'FAILED')"
                + " and received_at >= ?",
            Integer.class,
            Timestamp.from(since));
    Integer refused =
        jdbc.queryForObject(
            "select count(*) from idn_identity_event e where e.status in ('REFUSED', 'FAILED')"
                + " and not exists (select 1 from idn_event_dismissal d where d.event_id = e.id)",
            Integer.class);
    Integer mismatches =
        jdbc.queryForObject(
            "select count(*) from idn_reconciliation_mismatch where resolved_at is null",
            Integer.class);
    Optional<JobRun> last =
        runs.findFirstByJobNameOrderByStartedAtDesc(IdentityReconciliationJob.JOB_NAME);
    return new Status(
        lastCall,
        lastSuccess,
        errors == null ? 0 : errors,
        refused == null ? 0 : refused,
        mismatches == null ? 0 : mismatches,
        last.map(JobRun::getStartedAt).orElse(null),
        last.map(r -> r.getStatus().name()).orElse(null),
        last.map(JobRun::getMessage).orElse(null));
  }

  /**
   * Dismisses a refused or failed event with a remark: it needs no further action.
   *
   * @param eventId event
   * @param remark why
   * @return the dismissal
   */
  public IdentityEventDismissal dismiss(Long eventId, String remark) {
    IdentityEvent event =
        events
            .findById(eventId)
            .orElseThrow(() -> new ResourceNotFoundException("IdentityEvent", eventId));
    if (event.getStatus() != IdentityEventStatus.REFUSED
        && event.getStatus() != IdentityEventStatus.FAILED) {
      throw new BusinessRuleException(
          "IDENTITY_NOT_REFUSED", "Only a refused or failed event can be dismissed");
    }
    if (dismissals.existsByEventId(eventId)) {
      throw new BusinessRuleException(
          "IDENTITY_ALREADY_DISMISSED", "The event has already been dismissed");
    }
    String text = requireRemark(remark);
    IdentityEventDismissal d =
        dismissals.save(
            new IdentityEventDismissal(eventId, text, currentUser.username(), clock.instant()));
    audit.record(
        IdentitySyncService.ENTITY,
        eventId,
        AuditAction.UPDATE,
        "Refused event dismissed: " + text);
    return d;
  }

  /**
   * Reconciliation mismatches, newest first.
   *
   * @param open only the mismatches not resolved
   * @return mismatches (at most 500)
   */
  @Transactional(readOnly = true)
  public List<Mismatch> mismatches(boolean open) {
    return jdbc.query(
        "select id, run_date, username, windows_id, kind, detail, found_at, resolved_by,"
            + " resolved_at, resolution_remark from idn_reconciliation_mismatch"
            + " where (? = false or resolved_at is null) order by found_at desc, id desc limit 500",
        (rs, i) ->
            new Mismatch(
                rs.getLong("id"),
                rs.getObject("run_date", java.time.LocalDate.class),
                rs.getString("username"),
                rs.getString("windows_id"),
                rs.getString("kind"),
                rs.getString("detail"),
                toInstant(rs.getTimestamp("found_at")),
                rs.getString("resolved_by"),
                toInstant(rs.getTimestamp("resolved_at")),
                rs.getString("resolution_remark")),
        open);
  }

  /**
   * Resolves a mismatch with a remark (corrected by UIDM-ISC, or explained).
   *
   * @param id mismatch
   * @param remark how it was resolved
   */
  public void resolve(Long id, String remark) {
    String text = requireRemark(remark);
    int updated =
        jdbc.update(
            "update idn_reconciliation_mismatch set resolved_by = ?, resolved_at = ?,"
                + " resolution_remark = ? where id = ? and resolved_at is null",
            currentUser.username(),
            Timestamp.from(clock.instant()),
            text,
            id);
    if (updated == 0) {
      throw new BusinessRuleException(
          "IDENTITY_MISMATCH_RESOLVED", "The mismatch is unknown or already resolved");
    }
    audit.record(MISMATCH_ENTITY, id, AuditAction.UPDATE, "Mismatch resolved: " + text);
  }

  private static String requireRemark(String remark) {
    String text = remark == null ? "" : remark.trim();
    if (text.isEmpty()) {
      throw new BusinessRuleException("IDENTITY_REMARK", "Enter the remark");
    }
    return text.length() <= REMARK ? text : text.substring(0, REMARK);
  }

  private static Instant toInstant(Timestamp t) {
    return t == null ? null : t.toInstant();
  }

  /**
   * The state of the connection with UIDM-ISC.
   *
   * @param lastCall last event received from UIDM-ISC or the Enterprise SSO platform
   * @param lastSuccess last event applied (or without change)
   * @param errorsLast24Hours events refused or failed in the last 24 hours
   * @param refusedOpen refused or failed events not dismissed
   * @param mismatchesOpen reconciliation mismatches not resolved
   * @param lastSyncAt start of the last synchronisation run
   * @param lastSyncStatus outcome of the last run (SUCCEEDED, FAILED ...)
   * @param lastSyncMessage result of the last run in words
   */
  public record Status(
      Instant lastCall,
      Instant lastSuccess,
      int errorsLast24Hours,
      int refusedOpen,
      int mismatchesOpen,
      Instant lastSyncAt,
      String lastSyncStatus,
      String lastSyncMessage) {}

  /**
   * A reconciliation mismatch.
   *
   * @param id id
   * @param runDate business date of the run that found it
   * @param username user ID
   * @param windowsId Windows ID
   * @param kind NOT_IN_DIRECTORY, STATUS or GROUP_PROFILES
   * @param detail the difference in words
   * @param foundAt when it was found
   * @param resolvedBy who resolved it
   * @param resolvedAt when
   * @param resolutionRemark how
   */
  public record Mismatch(
      Long id,
      java.time.LocalDate runDate,
      String username,
      String windowsId,
      String kind,
      String detail,
      Instant foundAt,
      String resolvedBy,
      Instant resolvedAt,
      String resolutionRemark) {}
}
