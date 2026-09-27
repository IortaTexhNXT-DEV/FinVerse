package com.iortatechnxt.brokerverse.renewal.rules.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.renewal.check.service.CheckEngine;
import com.iortatechnxt.brokerverse.renewal.check.service.Evaluation;
import com.iortatechnxt.brokerverse.renewal.domain.CandidateSource;
import com.iortatechnxt.brokerverse.renewal.domain.CheckTrigger;
import com.iortatechnxt.brokerverse.renewal.domain.ExtractionTrigger;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalExtractionRunRepository;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalStage;
import com.iortatechnxt.brokerverse.renewal.service.RenewalCodes;
import com.iortatechnxt.brokerverse.renewal.service.RenewalFlow;
import com.iortatechnxt.brokerverse.renewal.service.RenewalParameters;
import com.iortatechnxt.brokerverse.renewal.service.RenewalRecords;
import com.iortatechnxt.brokerverse.workflow.service.TransitionNote;
import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Explicit initiation of renewal processing (BRRN.021; FR-RN-015): an extracted renewal does
 * nothing until an authorised user initiates it. Segments of {@code RNW_BULK_INITIATION_SEGMENTS}
 * (CLG) and the renewals taken over at go-live are initiated in bulk; the others one at a time.
 * Initiation records the user and time (a separate event from the extraction), runs the checks and
 * routes the renewal by its bucket and the decision matrix.
 */
@Service
@Transactional
public class InitiationService {

  private final RenewalRecords records;
  private final CheckEngine engine;
  private final RoutingService routing;
  private final RenewalFlow flow;
  private final RenewalParameters parameters;
  private final RenewalExtractionRunRepository runs;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param records renewals
   * @param engine check engine
   * @param routing routing
   * @param flow workflow
   * @param parameters renewal parameters
   * @param runs extraction runs (go-live run)
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  @SuppressWarnings("java:S107") // constructor injection
  public InitiationService(
      RenewalRecords records,
      CheckEngine engine,
      RoutingService routing,
      RenewalFlow flow,
      RenewalParameters parameters,
      RenewalExtractionRunRepository runs,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.records = records;
    this.engine = engine;
    this.routing = routing;
    this.flow = flow;
    this.parameters = parameters;
    this.runs = runs;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Initiates renewals.
   *
   * @param companyId company
   * @param renewalRefs selected renewals
   * @return initiated renewals and the refused ones with the reason
   */
  public Outcome initiate(Long companyId, List<String> renewalRefs) {
    if (renewalRefs == null || renewalRefs.isEmpty()) {
      throw new BusinessRuleException("RNW_SELECTION_EMPTY", "Select at least one account");
    }
    boolean bulk = renewalRefs.size() > 1;
    List<String> initiated = new ArrayList<>();
    Map<String, String> refused = new LinkedHashMap<>();
    for (String ref : renewalRefs) {
      RenewalCandidate c = records.get(companyId, ref);
      String refusal = refusal(c, bulk);
      if (refusal != null) {
        refused.put(ref, refusal);
        continue;
      }
      initiateOne(c);
      initiated.add(ref);
    }
    return new Outcome(initiated, refused);
  }

  /**
   * Initiates one renewal (also used by the hand-offs, whose scan is the initiation).
   *
   * @param c extracted renewal
   * @return its evaluation
   */
  public Evaluation initiateOne(RenewalCandidate c) {
    RenewalRecords.requireStage(c, RenewalStage.EXTRACTED);
    c.initiate(currentUser.username(), clock.instant());
    flow.act(c, "initiate", TransitionNote.comment("Renewal processing initiated"));
    Evaluation evaluation = engine.run(c, CheckTrigger.INITIATION);
    routing.route(c, evaluation);
    audit.record(
        RenewalCodes.ENTITY,
        c.getRenewalRef(),
        AuditAction.SUBMIT,
        "Initiated by " + currentUser.username() + "; bucket " + evaluation.bucket());
    return evaluation;
  }

  private String refusal(RenewalCandidate c, boolean bulk) {
    if (c.getStage() != RenewalStage.EXTRACTED) {
      return "Renewal " + c.getRenewalRef() + " is already initiated";
    }
    if (!bulk || goLive(c)) {
      return null;
    }
    String segment = c.getSnapshot().product() == null ? null : c.getSnapshot().product().segment();
    return parameters.bulkInitiation(segment)
        ? null
        : "Segment " + segment + " is initiated one account at a time";
  }

  private boolean goLive(RenewalCandidate c) {
    return c.getSource() == CandidateSource.LEGACY
        && c.getExtractionRunId() != null
        && runs.findById(c.getExtractionRunId())
            .map(r -> r.getTrigger() == ExtractionTrigger.GOLIVE)
            .orElse(false);
  }

  /**
   * Result of an initiation.
   *
   * @param initiated renewal references initiated
   * @param refused renewal reference to the reason it was refused
   */
  public record Outcome(List<String> initiated, Map<String, String> refused) {}
}
