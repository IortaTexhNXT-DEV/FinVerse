package com.iortatechnxt.brokerverse.receivables.api;

import com.iortatechnxt.brokerverse.receivables.api.dto.AutoMatchRequest;
import com.iortatechnxt.brokerverse.receivables.api.dto.BankStatementLineResponse;
import com.iortatechnxt.brokerverse.receivables.api.dto.BrsResponse;
import com.iortatechnxt.brokerverse.receivables.api.dto.BrsResponse.MatchResponse;
import com.iortatechnxt.brokerverse.receivables.api.dto.BrsResponse.WorkbenchResponse;
import com.iortatechnxt.brokerverse.receivables.api.dto.ManualMatchRequest;
import com.iortatechnxt.brokerverse.receivables.api.dto.ReconciliationRequest;
import com.iortatechnxt.brokerverse.receivables.api.dto.ReconciliationResponse;
import com.iortatechnxt.brokerverse.receivables.api.dto.StatementImportRequest;
import com.iortatechnxt.brokerverse.receivables.api.dto.StatementResponse;
import com.iortatechnxt.brokerverse.receivables.service.BankMatchingService;
import com.iortatechnxt.brokerverse.receivables.service.BankReconciliationService;
import com.iortatechnxt.brokerverse.receivables.service.BankStatementService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Bank reconciliation: statement import, matching workbench (automatic and manual), Bank
 * Reconciliation Statement and reconciliation finalization.
 */
@RestController
@RequestMapping("/api/v1/receivables/bank-rec")
public class BankReconciliationController {

  private final BankStatementService statements;
  private final BankMatchingService matching;
  private final BankReconciliationService reconciliation;

  /**
   * Creates the controller.
   *
   * @param statements statement service
   * @param matching matching service
   * @param reconciliation reconciliation service
   */
  public BankReconciliationController(
      BankStatementService statements,
      BankMatchingService matching,
      BankReconciliationService reconciliation) {
    this.statements = statements;
    this.matching = matching;
    this.reconciliation = reconciliation;
  }

  /**
   * Imports a bank statement (CSV).
   *
   * @param request statement
   * @return statement
   */
  @PostMapping("/statements")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAuthority('RECONCILIATION_MANAGE')")
  public StatementResponse importStatement(@Valid @RequestBody StatementImportRequest request) {
    return StatementResponse.from(statements.importStatement(request));
  }

  /**
   * Lists imported statements.
   *
   * @param companyId company
   * @param bankAccountCode bank account filter
   * @return statements
   */
  @GetMapping("/statements")
  @PreAuthorize("hasAuthority('RECONCILIATION_MANAGE')")
  public List<StatementResponse> statements(
      @RequestParam Long companyId, @RequestParam(required = false) String bankAccountCode) {
    return statements.list(companyId, bankAccountCode).stream()
        .map(StatementResponse::from)
        .toList();
  }

  /**
   * Lists the lines of a statement.
   *
   * @param id statement
   * @return lines
   */
  @GetMapping("/statements/{id}/lines")
  @PreAuthorize("hasAuthority('RECONCILIATION_MANAGE')")
  public List<BankStatementLineResponse> lines(@PathVariable Long id) {
    return statements.lines(id).stream().map(BankStatementLineResponse::from).toList();
  }

  /**
   * Unmatched book entries and bank lines side by side.
   *
   * @param companyId company
   * @param bankAccountCode bank account
   * @param asOf items dated on or before
   * @return workbench
   */
  @GetMapping("/workbench")
  @PreAuthorize("hasAuthority('RECONCILIATION_MANAGE')")
  public WorkbenchResponse workbench(
      @RequestParam Long companyId,
      @RequestParam String bankAccountCode,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf) {
    return WorkbenchResponse.from(matching.workbench(companyId, bankAccountCode, asOf));
  }

  /**
   * Runs automatic matching.
   *
   * @param request run
   * @return matches created
   */
  @PostMapping("/auto-match")
  @PreAuthorize("hasAuthority('RECONCILIATION_MANAGE')")
  public List<MatchResponse> autoMatch(@Valid @RequestBody AutoMatchRequest request) {
    return matching.autoMatch(request).stream().map(MatchResponse::from).toList();
  }

  /**
   * Matches selected items manually.
   *
   * @param request selection
   * @return match
   */
  @PostMapping("/matches")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAuthority('RECONCILIATION_MANAGE')")
  public MatchResponse match(@Valid @RequestBody ManualMatchRequest request) {
    return MatchResponse.from(matching.manualMatch(request));
  }

  /**
   * Lists the latest matches of a bank account.
   *
   * @param companyId company
   * @param bankAccountCode bank account
   * @return matches
   */
  @GetMapping("/matches")
  @PreAuthorize("hasAuthority('RECONCILIATION_MANAGE')")
  public List<MatchResponse> matches(
      @RequestParam Long companyId, @RequestParam String bankAccountCode) {
    return matching.recent(companyId, bankAccountCode).stream().map(MatchResponse::from).toList();
  }

  /**
   * Undoes a match.
   *
   * @param id match
   * @return removed match
   */
  @PostMapping("/matches/{id}/unmatch")
  @PreAuthorize("hasAuthority('RECONCILIATION_MANAGE')")
  public MatchResponse unmatch(@PathVariable Long id) {
    return MatchResponse.from(matching.unmatch(id));
  }

  /**
   * Computes the Bank Reconciliation Statement.
   *
   * @param companyId company
   * @param bankAccountCode bank account
   * @param asOf statement date
   * @return statement
   */
  @GetMapping("/brs")
  @PreAuthorize("hasAuthority('RECONCILIATION_MANAGE')")
  public BrsResponse brs(
      @RequestParam Long companyId,
      @RequestParam String bankAccountCode,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf) {
    return BrsResponse.from(reconciliation.statement(companyId, bankAccountCode, asOf));
  }

  /**
   * Lists saved reconciliations.
   *
   * @param companyId company
   * @return reconciliations
   */
  @GetMapping("/reconciliations")
  @PreAuthorize("hasAuthority('RECONCILIATION_MANAGE')")
  public List<ReconciliationResponse> reconciliations(@RequestParam Long companyId) {
    return reconciliation.list(companyId).stream().map(ReconciliationResponse::from).toList();
  }

  /**
   * Saves (or refreshes) the reconciliation as of a date.
   *
   * @param request bank account and date
   * @return reconciliation
   */
  @PostMapping("/reconciliations")
  @PreAuthorize("hasAuthority('RECONCILIATION_MANAGE')")
  public ReconciliationResponse save(@Valid @RequestBody ReconciliationRequest request) {
    return ReconciliationResponse.from(reconciliation.save(request));
  }

  /**
   * Finalizes a reconciliation (difference must be zero).
   *
   * @param id reconciliation
   * @return reconciliation
   */
  @PostMapping("/reconciliations/{id}/finalize")
  @PreAuthorize("hasAuthority('RECONCILIATION_MANAGE')")
  public ReconciliationResponse finalizeReconciliation(@PathVariable Long id) {
    return ReconciliationResponse.from(reconciliation.finalizeReconciliation(id));
  }
}
