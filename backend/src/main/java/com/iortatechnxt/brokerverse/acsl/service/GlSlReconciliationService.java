package com.iortatechnxt.brokerverse.acsl.service;

import com.iortatechnxt.brokerverse.acsl.domain.GlSlControl;
import com.iortatechnxt.brokerverse.acsl.domain.GlSlControlRepository;
import com.iortatechnxt.brokerverse.acsl.domain.GlSlRecon;
import com.iortatechnxt.brokerverse.acsl.domain.GlSlReconRepository;
import com.iortatechnxt.brokerverse.acsl.domain.GlSlRun;
import com.iortatechnxt.brokerverse.acsl.domain.GlSlRunRepository;
import com.iortatechnxt.brokerverse.acsl.domain.SlSource;
import com.iortatechnxt.brokerverse.acsl.service.GlSlQueries.ControlAccount;
import com.iortatechnxt.brokerverse.alert.domain.AlertFacts;
import com.iortatechnxt.brokerverse.alert.domain.ExceptionCode;
import com.iortatechnxt.brokerverse.alert.service.AlertService;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.ledger.service.LedgerQueryService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * GL-SL reconciliation (ACSL 2.13.2): for every control account of a company, the GL balance as of
 * a date against its sub-ledger (the party postings of the account by default, or the configured
 * open items or Operations ledger components), the difference, and an {@code ACSL_GLSL_DIFFERENCE}
 * alert per account whose difference reaches the alert's threshold. Runs are kept for the report
 * {@code ACSL-GL-SL-RECON}.
 */
@Service
@Transactional
public class GlSlReconciliationService {

  /** Alert raised for a difference (V890). */
  public static final String ALERT = "ACSL_GLSL_DIFFERENCE";

  private final GlSlQueries queries;
  private final LedgerQueryService ledger;
  private final GlSlControlRepository controls;
  private final GlSlRunRepository runs;
  private final GlSlReconRepository rows;
  private final AlertService alerts;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param queries control accounts and sub-ledger balances
   * @param ledger GL balances
   * @param controls sub-ledger configuration
   * @param runs runs
   * @param rows accounts of the runs
   * @param alerts alerts
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  public GlSlReconciliationService(
      GlSlQueries queries,
      LedgerQueryService ledger,
      GlSlControlRepository controls,
      GlSlRunRepository runs,
      GlSlReconRepository rows,
      AlertService alerts,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.queries = queries;
    this.ledger = ledger;
    this.controls = controls;
    this.runs = runs;
    this.rows = rows;
    this.alerts = alerts;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Reconciles every control account of a company as of a date.
   *
   * @param companyId company
   * @param asOf balances as of
   * @return the run
   */
  public GlSlRun run(Long companyId, LocalDate asOf) {
    GlSlRun run = runs.save(new GlSlRun(companyId, asOf, clock.instant(), currentUser.username()));
    Map<String, GlSlControl> settings =
        controls.findByCompanyIdOrderByAccountCode(companyId).stream()
            .filter(GlSlControl::isActive)
            .collect(Collectors.toMap(GlSlControl::getAccountCode, Function.identity()));
    BigDecimal tolerance =
        alerts.activeCode(ALERT).map(ExceptionCode::getThresholdAmount).orElse(BigDecimal.ONE);
    List<ControlAccount> accounts = queries.controlAccounts(companyId);
    int differences = 0;
    BigDecimal total = BigDecimal.ZERO;
    for (ControlAccount account : accounts) {
      GlSlRecon row = rows.save(reconcile(run, account, settings.get(account.code())));
      BigDecimal gap = row.getDifference().abs();
      if (gap.signum() > 0) {
        differences++;
        total = total.add(gap);
        alertIfNeeded(companyId, row, gap, tolerance);
      }
    }
    run.totals(accounts.size(), differences, total);
    audit.record(
        "GlSlRun",
        companyId + ":" + asOf,
        AuditAction.RUN,
        accounts.size() + " control account(s), " + differences + " with a difference");
    return run;
  }

  private GlSlRecon reconcile(GlSlRun run, ControlAccount account, GlSlControl setting) {
    BigDecimal gl = ledger.netBalance(run.getCompanyId(), account.id(), null, run.getAsOf());
    SlSource source = setting == null ? SlSource.PARTY_LEDGER : setting.getSource();
    BigDecimal sl =
        switch (source) {
          case OPEN_ITEMS ->
              queries.openItems(run.getCompanyId(), setting.documentTypeList(), run.getAsOf());
          case OPS_LEDGER ->
              queries.opsLedger(
                  run.getCompanyId(), setting.componentList(), setting.getCurrency(), account);
          default -> queries.partyLedger(run.getCompanyId(), account.id(), run.getAsOf());
        };
    return new GlSlRecon(run.getId(), account.code(), account.name(), source, gl, sl);
  }

  private void alertIfNeeded(Long companyId, GlSlRecon row, BigDecimal gap, BigDecimal tolerance) {
    if (gap.compareTo(tolerance) >= 0) {
      alerts.raise(
          ALERT,
          new AlertFacts(
              companyId,
              null,
              "GlAccount",
              row.getAccountCode(),
              "GL "
                  + row.getGlBalance().toPlainString()
                  + " and sub-ledger "
                  + row.getSlBalance().toPlainString()
                  + " of "
                  + row.getAccountCode()
                  + " differ by "
                  + row.getDifference().toPlainString(),
              gap,
              ALERT + ":" + companyId + ":" + row.getAccountCode()));
    }
  }

  /**
   * Sets the sub-ledger of a control account (configuration, AQ01).
   *
   * @param companyId company
   * @param accountCode control account
   * @param setting source, components, document types, currency and active flag
   * @return the configuration
   */
  public GlSlControl configure(Long companyId, String accountCode, GlSlControl.Setting setting) {
    requireSetting(setting);
    GlSlControl control =
        controls
            .findByCompanyIdAndAccountCode(companyId, accountCode)
            .orElseGet(() -> controls.save(new GlSlControl(companyId, accountCode, setting)));
    control.change(setting);
    audit.record("GlSlControl", accountCode, AuditAction.UPDATE, "Sub-ledger " + setting.source());
    return control;
  }

  private static void requireSetting(GlSlControl.Setting s) {
    boolean incomplete =
        s.source() == null
            || s.source() == SlSource.OPS_LEDGER && Acsl.blankToNull(s.components()) == null
            || s.source() == SlSource.OPEN_ITEMS && Acsl.blankToNull(s.documentTypes()) == null;
    if (incomplete) {
      throw new BusinessRuleException(
          "ACSL_GLSL_SETTING",
          "Give the sub-ledger, and the components (Operations ledger) or document types (open items)");
    }
  }

  /**
   * Sub-ledger configurations of a company.
   *
   * @param companyId company
   * @return configurations
   */
  @Transactional(readOnly = true)
  public List<GlSlControl> controls(Long companyId) {
    return controls.findByCompanyIdOrderByAccountCode(companyId);
  }

  /**
   * The latest runs of a company.
   *
   * @param companyId company
   * @return runs, newest first
   */
  @Transactional(readOnly = true)
  public List<GlSlRun> runs(Long companyId) {
    return runs.findTop30ByCompanyIdOrderByIdDesc(companyId);
  }

  /**
   * The latest run of a company on or before a date.
   *
   * @param companyId company
   * @param asOf date
   * @return run
   */
  @Transactional(readOnly = true)
  public Optional<GlSlRun> latest(Long companyId, LocalDate asOf) {
    return runs.findFirstByCompanyIdAndAsOfLessThanEqualOrderByAsOfDescIdDesc(companyId, asOf);
  }

  /**
   * The accounts of a run.
   *
   * @param runId run
   * @return accounts
   */
  @Transactional(readOnly = true)
  public List<GlSlRecon> rows(Long runId) {
    if (!runs.existsById(runId)) {
      throw new ResourceNotFoundException("GlSlRun", runId);
    }
    return rows.findByRunIdOrderByAccountCode(runId);
  }
}
