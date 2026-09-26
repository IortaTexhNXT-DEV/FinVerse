package com.iortatechnxt.brokerverse.screening.cases.service;

import com.iortatechnxt.brokerverse.crm.domain.Client;
import com.iortatechnxt.brokerverse.crm.service.ClientService;
import com.iortatechnxt.brokerverse.screening.cases.domain.CaseTypes;
import com.iortatechnxt.brokerverse.screening.cases.service.CaseOpeningService.OpenSpec;
import com.iortatechnxt.brokerverse.screening.matching.domain.ScreeningTrigger;
import com.iortatechnxt.brokerverse.screening.matching.service.ScreenedMatch;
import com.iortatechnxt.brokerverse.screening.matching.service.ScreeningCompleted;
import com.iortatechnxt.brokerverse.screening.matching.service.ScreeningEngine;
import com.iortatechnxt.brokerverse.screening.matching.service.ScreeningResult;
import com.iortatechnxt.brokerverse.screening.risk.service.RiskOutcome;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Opens cases from screening results (SNSRP-303; FR-SS-034): after a run committed, a NAME_MATCH
 * case for the matches that reach their case threshold, the case type of the risk category each
 * client qualified for (MONITOR for a high-risk, PEP or EDD client without an active policy) and,
 * for an account submission of a client whose screening needs a review, an ACCOUNT_APPLICATION
 * case. Works as the system in a transaction of its own; a failure is logged and never reaches the
 * run. The number of new cases is written to the run log.
 */
@Component
public class CaseTriggers {

  private static final Logger LOG = LoggerFactory.getLogger(CaseTriggers.class);

  private final CaseOpeningService opening;
  private final ClientService clients;
  private final ScreeningEngine engine;
  private final TransactionTemplate tx;

  /**
   * Creates the triggers.
   *
   * @param opening case opening
   * @param clients client master (read)
   * @param engine screening engine (run log)
   * @param txManager transactions
   */
  public CaseTriggers(
      CaseOpeningService opening,
      ClientService clients,
      ScreeningEngine engine,
      PlatformTransactionManager txManager) {
    this.opening = opening;
    this.clients = clients;
    this.engine = engine;
    this.tx = new TransactionTemplate(txManager);
    this.tx.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
  }

  /**
   * Opens the cases of a completed run.
   *
   * @param event the run result
   */
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onCompleted(ScreeningCompleted event) {
    ScreeningResult result = event.result();
    try {
      SystemActor.call(() -> tx.execute(s -> open(result)));
    } catch (RuntimeException ex) {
      LOG.warn("Cases of screening run {} not opened: {}", result.runNo(), ex.getMessage());
    }
  }

  /**
   * Opens or joins the cases of a run result.
   *
   * @param result the result
   * @return the number of new cases
   */
  int open(ScreeningResult result) {
    Set<Long> clientIds = new LinkedHashSet<>();
    result.caseMatches().forEach(m -> clientIds.add(m.clientId()));
    result.riskOutcomes().forEach(o -> clientIds.add(o.clientId()));
    Set<Long> linked = new HashSet<>();
    int created = 0;
    for (Long clientId : clientIds) {
      created += openFor(result, clients.get(clientId), linked);
    }
    if (created > 0) {
      engine.recordCasesOpened(result.runId(), created);
    }
    return created;
  }

  private int openFor(ScreeningResult result, Client client, Set<Long> linked) {
    Optional<RiskOutcome> outcome = result.outcomeOf(client.getId());
    String category = outcome.map(RiskOutcome::categoryCode).orElse(null);
    int created = 0;
    List<Long> caseMatches =
        result.caseMatches().stream()
            .filter(m -> m.clientId().equals(client.getId()) && m.caseId() == null)
            .map(ScreenedMatch::matchId)
            .toList();
    if (!caseMatches.isEmpty()) {
      linked.addAll(caseMatches);
      created += count(open(client, result, CaseTypes.NAME_MATCH, category, false, caseMatches));
    }
    if (outcome.isPresent() && outcome.get().caseType() != null) {
      RiskOutcome o = outcome.get();
      String type = typeOf(o, client);
      List<Long> own =
          o.matchId() != null && linked.add(o.matchId()) ? List.of(o.matchId()) : List.of();
      created += count(open(client, result, type, category, o.requiresEdd(), own));
    }
    if (result.trigger() == ScreeningTrigger.ACCOUNT_SUBMITTED
        && (outcome.isPresent() || !caseMatches.isEmpty())) {
      created +=
          count(open(client, result, CaseTypes.ACCOUNT_APPLICATION, category, false, List.of()));
    }
    return created;
  }

  private String typeOf(RiskOutcome outcome, Client client) {
    String type = outcome.caseType();
    return CaseTypes.NEED_ACTIVE_POLICY.contains(type) && !opening.hasActivePolicy(client.getId())
        ? CaseTypes.MONITOR
        : type;
  }

  private CaseOpeningService.Opened open(
      Client client,
      ScreeningResult result,
      String type,
      String category,
      boolean edd,
      List<Long> matchIds) {
    return opening.openOrJoin(
        client,
        new OpenSpec(
            type,
            result.trigger().name(),
            result.reference(),
            category,
            edd,
            matchIds,
            result.matchVersionId(),
            result.riskVersionId()));
  }

  private static int count(CaseOpeningService.Opened opened) {
    return opened.created() ? 1 : 0;
  }
}
