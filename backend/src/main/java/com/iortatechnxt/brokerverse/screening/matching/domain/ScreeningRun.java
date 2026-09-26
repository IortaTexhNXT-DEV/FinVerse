package com.iortatechnxt.brokerverse.screening.matching.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * The log of one screening run (SNSRP-602 "every activity is logged and traceable"; FR-SS-030):
 * trigger, scope, configuration versions, clients and entries screened, matches recorded, risk
 * changes and cases opened, status, start and end time.
 */
@Entity
@Table(name = "scr_screening_run")
public class ScreeningRun extends BaseEntity {

  private static final int MAX_TEXT = 2000;
  private static final int MAX_REFERENCE = 100;
  private static final int MAX_SCOPE = 200;

  @Column(name = "run_no", nullable = false, length = 30, updatable = false)
  private String runNo;

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Enumerated(EnumType.STRING)
  @Column(name = "trigger_code", nullable = false, length = 30, updatable = false)
  private ScreeningTrigger trigger;

  @Column(name = "reference", length = MAX_REFERENCE, updatable = false)
  private String reference;

  @Column(name = "scope", nullable = false, length = MAX_SCOPE, updatable = false)
  private String scope;

  @Column(name = "full_rescreen", nullable = false, updatable = false)
  private boolean fullRescreen;

  @Column(name = "match_version_id", nullable = false, updatable = false)
  private Long matchVersionId;

  @Column(name = "risk_version_id", updatable = false)
  private Long riskVersionId;

  @Column(name = "clients_screened", nullable = false)
  private int clientsScreened;

  @Column(name = "entries_screened", nullable = false)
  private int entriesScreened;

  @Column(name = "matches", nullable = false)
  private int matches;

  @Column(name = "risk_changes", nullable = false)
  private int riskChanges;

  @Column(name = "cases_opened", nullable = false)
  private int casesOpened;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private ScreeningRunStatus status = ScreeningRunStatus.RUNNING;

  @Column(name = "error", length = MAX_TEXT)
  private String error;

  @Column(name = "job_run_id")
  private Long jobRunId;

  @Column(name = "started_at", nullable = false, updatable = false)
  private Instant startedAt;

  @Column(name = "ended_at")
  private Instant endedAt;

  /** For JPA. */
  protected ScreeningRun() {}

  /**
   * Starts a run.
   *
   * @param runNo run number
   * @param companyId company
   * @param start trigger, reference, scope and whether the whole list is screened
   * @param matchVersionId MATCH_CRITERIA version
   * @param riskVersionId RISK_RULES version, may be {@code null}
   * @param startedAt start time
   */
  public ScreeningRun(
      String runNo,
      Long companyId,
      RunStart start,
      Long matchVersionId,
      Long riskVersionId,
      Instant startedAt) {
    this.runNo = runNo;
    this.companyId = companyId;
    this.trigger = start.trigger();
    this.reference = cap(start.reference(), MAX_REFERENCE);
    this.scope = cap(start.scope(), MAX_SCOPE);
    this.fullRescreen = start.fullRescreen();
    this.matchVersionId = matchVersionId;
    this.riskVersionId = riskVersionId;
    this.startedAt = startedAt;
  }

  /**
   * Adds the counts of a batch of clients.
   *
   * @param clients clients screened
   * @param newMatches matches recorded
   * @param changes risk-profile changes
   */
  public void count(int clients, int newMatches, int changes) {
    this.clientsScreened += clients;
    this.matches += newMatches;
    this.riskChanges += changes;
  }

  /**
   * Ends the run.
   *
   * @param entries entries screened
   * @param when end time
   */
  public void complete(int entries, Instant when) {
    this.entriesScreened = entries;
    this.status = ScreeningRunStatus.SUCCESS;
    this.endedAt = when;
  }

  /**
   * Ends the run as FAILED.
   *
   * @param reason reason
   * @param when end time
   */
  public void fail(String reason, Instant when) {
    this.status = ScreeningRunStatus.FAILED;
    this.error = cap(reason, MAX_TEXT);
    this.endedAt = when;
  }

  /**
   * Adds cases opened from the run's results (case wave).
   *
   * @param count cases opened
   */
  public void casesOpened(int count) {
    this.casesOpened += count;
  }

  /**
   * Links the job run of a batch run.
   *
   * @param id job run id
   */
  public void linkJob(Long id) {
    this.jobRunId = id;
  }

  private static String cap(String value, int max) {
    return value == null || value.length() <= max ? value : value.substring(0, max);
  }

  public String getRunNo() {
    return runNo;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public ScreeningTrigger getTrigger() {
    return trigger;
  }

  public String getReference() {
    return reference;
  }

  public String getScope() {
    return scope;
  }

  public boolean isFullRescreen() {
    return fullRescreen;
  }

  public Long getMatchVersionId() {
    return matchVersionId;
  }

  public Long getRiskVersionId() {
    return riskVersionId;
  }

  public int getClientsScreened() {
    return clientsScreened;
  }

  public int getEntriesScreened() {
    return entriesScreened;
  }

  public int getMatches() {
    return matches;
  }

  public int getRiskChanges() {
    return riskChanges;
  }

  public int getCasesOpened() {
    return casesOpened;
  }

  public ScreeningRunStatus getStatus() {
    return status;
  }

  public String getError() {
    return error;
  }

  public Long getJobRunId() {
    return jobRunId;
  }

  public Instant getStartedAt() {
    return startedAt;
  }

  public Instant getEndedAt() {
    return endedAt;
  }

  /**
   * What a run screens.
   *
   * @param trigger what started it
   * @param reference the trigger's reference (client code, ARN, list change, date)
   * @param scope the clients in scope, e.g. "Client P-2026-000123" or "PROSPECT,CONFIRMED"
   * @param fullRescreen whether the whole list is screened (otherwise the changed entries)
   */
  public record RunStart(
      ScreeningTrigger trigger, String reference, String scope, boolean fullRescreen) {}
}
