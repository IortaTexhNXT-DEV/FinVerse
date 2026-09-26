package com.iortatechnxt.brokerverse.commission.service;

import com.iortatechnxt.brokerverse.alert.domain.AlertFacts;
import com.iortatechnxt.brokerverse.alert.service.AlertService;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.commission.domain.CommissionEnums.Beneficiary;
import com.iortatechnxt.brokerverse.commission.domain.IncentiveRun;
import com.iortatechnxt.brokerverse.commission.domain.IncentiveRunLine;
import com.iortatechnxt.brokerverse.commission.domain.IncentiveRunLine.Production;
import com.iortatechnxt.brokerverse.commission.domain.IncentiveRunLineRepository;
import com.iortatechnxt.brokerverse.commission.domain.IncentiveRunRepository;
import com.iortatechnxt.brokerverse.commission.domain.IncentiveScheme;
import com.iortatechnxt.brokerverse.commission.service.IncentiveEngine.Candidate;
import com.iortatechnxt.brokerverse.commission.service.IncentiveEngine.Result;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.brokerverse.lov.domain.LovValue;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import com.iortatechnxt.brokerverse.opsledger.domain.LedgerComponent;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerQueryService;
import com.iortatechnxt.brokerverse.opsledger.service.LedgerSearch;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Incentive runs (CMRID.003/005/006): the production booked in the period that a scheme covers is
 * read from the Operations ledger, cleared by the active exclusion rules ({@code
 * INCENTIVE_EXCLUSION_RULE}: negative amounts, erroneous bookings; {@code INCENTIVE_EXCLUSION}
 * alert when any line is excluded) and computed by the {@link IncentiveEngine}. A computed run is
 * posted by {@link IncentivePosting} or cancelled.
 */
@Service
@Transactional
public class IncentiveRunService {

  /** Negative production amounts are not eligible (CMRID.003). */
  public static final String NEGATIVE_AMOUNT = "NEGATIVE_AMOUNT";

  /** Cancelled or written-off bookings are not eligible (CMRID.003). */
  public static final String ERRONEOUS_BOOKING = "ERRONEOUS_BOOKING";

  private static final String ENTITY = "IncentiveRun";
  private static final int PAGE = 200;

  private final IncentiveSchemeService schemes;
  private final IncentiveRunRepository runs;
  private final IncentiveRunLineRepository lines;
  private final InvoiceLedgerQueryService ledger;
  private final LovService lovs;
  private final AlertService alerts;
  private final DocumentNumberService numbers;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param schemes schemes
   * @param runs runs
   * @param lines run lines
   * @param ledger Operations ledger
   * @param lovs exclusion rules
   * @param alerts alerts
   * @param numbers run numbers
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  @SuppressWarnings("java:S107") // constructor injection
  public IncentiveRunService(
      IncentiveSchemeService schemes,
      IncentiveRunRepository runs,
      IncentiveRunLineRepository lines,
      InvoiceLedgerQueryService ledger,
      LovService lovs,
      AlertService alerts,
      DocumentNumberService numbers,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.schemes = schemes;
    this.runs = runs;
    this.lines = lines;
    this.ledger = ledger;
    this.lovs = lovs;
    this.alerts = alerts;
    this.numbers = numbers;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Computes a scheme for a period.
   *
   * @param schemeId scheme
   * @param from first booking date
   * @param to last booking date
   * @return the computed run
   */
  public IncentiveRun compute(Long schemeId, LocalDate from, LocalDate to) {
    IncentiveScheme scheme = schemes.require(schemeId);
    if (to.isBefore(from)) {
      throw new BusinessRuleException("INCENTIVE_RUN_PERIOD", "The period ends before it starts");
    }
    if (scheme.getTiers().isEmpty()) {
      throw new BusinessRuleException(
          "INCENTIVE_SCHEME_EMPTY",
          "Scheme " + scheme.getName() + " has no tiers yet (targets and amounts from BDOI, OQ39)");
    }
    Set<String> rules =
        lovs.activeValues("INCENTIVE_EXCLUSION_RULE", LocalDate.now(clock)).stream()
            .map(LovValue::getCode)
            .collect(Collectors.toSet());
    List<Candidate> candidates = new ArrayList<>();
    for (OpsInvoice i : production(scheme, from, to)) {
      candidates.add(new Candidate(production(i), exclusion(i, rules)));
    }
    Result result = IncentiveEngine.compute(scheme.getCalculation(), scheme.getTiers(), candidates);
    IncentiveRun run = save(scheme, from, to, result);
    if (result.excludedCount() > 0) {
      alerts.raise(
          "INCENTIVE_EXCLUSION",
          new AlertFacts(
              scheme.getCompanyId(),
              null,
              ENTITY,
              run.getRunNo(),
              result.excludedCount()
                  + " invoice(s) excluded from "
                  + scheme.getName()
                  + " run "
                  + run.getRunNo(),
              result.excludedAmount(),
              "INCENTIVE_EXCLUSION:" + run.getRunNo()));
    }
    audit.record(
        ENTITY,
        run.getRunNo(),
        AuditAction.CREATE,
        scheme.getCode() + " " + from + " - " + to + ": incentive " + result.incentive());
    return run;
  }

  private IncentiveRun save(IncentiveScheme scheme, LocalDate from, LocalDate to, Result result) {
    IncentiveRun run =
        runs.save(
            new IncentiveRun(
                scheme.getCompanyId(),
                numbers.next("INR-" + to.getYear()),
                scheme.getId(),
                from,
                to));
    result
        .lines()
        .forEach(
            l ->
                lines.save(
                    new IncentiveRunLine(
                        run.getId(), l.production(), l.exclusion(), l.incentive())));
    run.computed(
        new IncentiveRun.Totals(
            result.eligibleCount(),
            result.eligibleProduction(),
            result.excludedCount(),
            result.excludedAmount(),
            result.tierApplied(),
            result.incentive(),
            scheme.getBeneficiary() == Beneficiary.BRANCH ? result.incentive() : BigDecimal.ZERO));
    return run;
  }

  private List<OpsInvoice> production(IncentiveScheme scheme, LocalDate from, LocalDate to) {
    LedgerSearch search =
        new LedgerSearch(
            scheme.getCompanyId(),
            null,
            scheme.getInsurerCode(),
            null,
            null,
            null,
            null,
            null,
            null,
            from,
            to);
    List<OpsInvoice> out = new ArrayList<>();
    Pageable pageable = PageRequest.of(0, PAGE, Sort.by("id"));
    Page<OpsInvoice> page;
    do {
      page = ledger.searchLoaded(search, pageable);
      page.stream()
          .filter(
              i ->
                  scheme.covers(
                      i.getInsurerCode(),
                      i.getClassification().segment(),
                      i.getClassification().productLine()))
          .forEach(out::add);
      pageable = page.nextPageable();
    } while (page.hasNext());
    return out;
  }

  private static Production production(OpsInvoice i) {
    return new Production(
        i.getInvoiceNo(),
        i.getInsurerCode(),
        i.getClassification().salesUnit(),
        i.getClassification().segment(),
        i.getClassification().productLine(),
        i.component(LedgerComponent.BASIC).due(),
        i.getGrossPremium());
  }

  /**
   * The exclusion rule that removes an invoice (CMRID.003).
   *
   * @param i invoice
   * @param rules active rule codes
   * @return rule code, null when eligible
   */
  static String exclusion(OpsInvoice i, Set<String> rules) {
    if (rules.contains(NEGATIVE_AMOUNT) && i.getGrossPremium().signum() <= 0) {
      return NEGATIVE_AMOUNT;
    }
    if (rules.contains(ERRONEOUS_BOOKING) && (i.isCancelled() || i.isWrittenOff())) {
      return ERRONEOUS_BOOKING;
    }
    return null;
  }

  /**
   * Cancels a computed run.
   *
   * @param id run
   * @return run
   */
  public IncentiveRun cancel(Long id) {
    IncentiveRun run = require(id);
    run.cancel();
    audit.record(
        ENTITY, run.getRunNo(), AuditAction.UPDATE, "Cancelled by " + currentUser.username());
    return run;
  }

  /**
   * A run.
   *
   * @param id run
   * @return run
   */
  @Transactional(readOnly = true)
  public IncentiveRun require(Long id) {
    return runs.findById(id).orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
  }

  /**
   * Runs of a company.
   *
   * @param companyId company
   * @param schemeId scheme, null for all
   * @param pageable page
   * @return runs, newest first
   */
  @Transactional(readOnly = true)
  public Page<IncentiveRun> runs(Long companyId, Long schemeId, Pageable pageable) {
    return runs.search(companyId, schemeId, pageable);
  }

  /**
   * Lines of a run.
   *
   * @param id run
   * @param pageable page
   * @return lines
   */
  @Transactional(readOnly = true)
  public Page<IncentiveRunLine> lines(Long id, Pageable pageable) {
    require(id);
    return lines.findByRunIdOrderByIdAsc(id, pageable);
  }
}
