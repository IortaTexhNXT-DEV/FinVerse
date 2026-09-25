package com.iortatechnxt.brokerverse.commission.service;

import com.iortatechnxt.brokerverse.accounting.service.AccountingEventPublisher;
import com.iortatechnxt.brokerverse.accounting.service.BusinessEvent;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.commission.domain.CommissionEnums.Beneficiary;
import com.iortatechnxt.brokerverse.commission.domain.IncentiveRun;
import com.iortatechnxt.brokerverse.commission.domain.IncentiveRunLine;
import com.iortatechnxt.brokerverse.commission.domain.IncentiveRunLineRepository;
import com.iortatechnxt.brokerverse.commission.domain.IncentiveScheme;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.opsledger.domain.DisbursementRequest;
import com.iortatechnxt.brokerverse.opsledger.service.BookRates;
import com.iortatechnxt.brokerverse.opsledger.service.port.DisbursementGateway;
import com.iortatechnxt.brokerverse.opsledger.service.port.DisbursementGateway.DisbursementTicket;
import com.iortatechnxt.brokerverse.organization.domain.Branch;
import com.iortatechnxt.brokerverse.organization.domain.Company;
import com.iortatechnxt.brokerverse.organization.service.OrganizationService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Posting of a computed incentive run (OPERATIONS_DESIGN 5 rows 25/26): the incentive earned from
 * each insurer is accrued ({@code OPS_INCENTIVE_ACCRUE}, Dr incentive receivable / Cr incentive
 * income, party the insurer); a scheme passed on to branches (Motor Mania, CMRID.006) also moves
 * each branch's share to due to branches ({@code OPS_INCENTIVE_PASS_ON}) and sends a pass-on
 * payment request per branch through the {@link DisbursementGateway} ({@code PASS_ON}).
 */
@Service
@Transactional
public class IncentivePosting {

  private static final String ENTITY = "IncentiveRun";

  private final IncentiveRunService runs;
  private final IncentiveSchemeService schemes;
  private final IncentiveRunLineRepository lines;
  private final AccountingEventPublisher publisher;
  private final BookRates rates;
  private final DisbursementGateway disbursement;
  private final OrganizationService organization;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the posting.
   *
   * @param runs runs
   * @param schemes schemes
   * @param lines run lines
   * @param publisher accounting engine
   * @param rates BOOK rates
   * @param disbursement Disbursement port
   * @param organization company and head office
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  @SuppressWarnings("java:S107") // constructor injection
  public IncentivePosting(
      IncentiveRunService runs,
      IncentiveSchemeService schemes,
      IncentiveRunLineRepository lines,
      AccountingEventPublisher publisher,
      BookRates rates,
      DisbursementGateway disbursement,
      OrganizationService organization,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.runs = runs;
    this.schemes = schemes;
    this.lines = lines;
    this.publisher = publisher;
    this.rates = rates;
    this.disbursement = disbursement;
    this.organization = organization;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Posts a computed run.
   *
   * @param id run
   * @return the posted run
   */
  public IncentiveRun post(Long id) {
    IncentiveRun run = runs.require(id);
    IncentiveScheme scheme = schemes.require(run.getSchemeId());
    if (run.getIncentiveAmount().signum() <= 0) {
      throw new BusinessRuleException(
          "INCENTIVE_RUN_NOTHING", "Run " + run.getRunNo() + " earned no incentive to post");
    }
    Company company = organization.getCompany(run.getCompanyId());
    Long branchId = headOffice(run.getCompanyId());
    List<IncentiveRunLine> earned =
        lines.findByRunIdOrderByIdAsc(id).stream()
            .filter(l -> l.getIncentive().signum() > 0)
            .toList();
    List<String> refs = new ArrayList<>();
    Context ctx = new Context(run, company.getBaseCurrency(), branchId, LocalDate.now(clock));
    group(earned, IncentiveRunLine::getInsurerCode)
        .forEach(
            (insurer, amount) ->
                refs.add(
                    publish(
                        ctx,
                        "OPS_INCENTIVE_ACCRUE",
                        "INC:" + run.getRunNo() + ":" + insurer,
                        insurer,
                        Map.of("INCENTIVE", amount))));
    if (scheme.getBeneficiary() == Beneficiary.BRANCH) {
      group(earned, l -> l.getSalesUnit() == null ? "HO" : l.getSalesUnit())
          .forEach((unit, amount) -> refs.addAll(passOn(ctx, scheme, unit, amount)));
    }
    run.posted(clock.instant(), currentUser.username(), String.join(", ", refs));
    audit.record(ENTITY, run.getRunNo(), AuditAction.POST, "Posted: " + String.join(", ", refs));
    return run;
  }

  private List<String> passOn(Context ctx, IncentiveScheme scheme, String unit, BigDecimal amount) {
    String ref = "INCP:" + ctx.run().getRunNo() + ":" + unit;
    String batch = publish(ctx, "OPS_INCENTIVE_PASS_ON", ref, unit, Map.of("PASS_ON", amount));
    DisbursementTicket ticket =
        disbursement.send(
            ctx.run().getCompanyId(),
            new DisbursementRequest.Spec(
                DisbursementRequest.Type.PASS_ON,
                DpIntakeService.MODULE,
                ref,
                unit,
                "Branch " + unit,
                ctx.currency(),
                amount,
                scheme.getName() + " incentive pass-on " + ctx.run().getRunNo(),
                null));
    return List.of(batch, ticket.requestNo());
  }

  private String publish(
      Context ctx, String type, String ref, String party, Map<String, BigDecimal> amounts) {
    return publisher
        .publish(
            rates.price(
                new BusinessEvent(
                    type,
                    ctx.run().getCompanyId(),
                    ctx.branchId(),
                    ctx.valueDate(),
                    ctx.currency(),
                    DpIntakeService.MODULE,
                    ref,
                    ctx.run().getRunNo(),
                    party,
                    null,
                    null,
                    type + " " + ctx.run().getRunNo(),
                    amounts,
                    Map.of())))
        .getBatchNo();
  }

  private Long headOffice(Long companyId) {
    return organization.listBranches(companyId).stream()
        .filter(Branch::isHeadOffice)
        .map(Branch::getId)
        .findFirst()
        .orElseThrow(
            () ->
                new BusinessRuleException(
                    "INCENTIVE_NO_HEAD_OFFICE", "The company has no head office branch"));
  }

  private static Map<String, BigDecimal> group(
      List<IncentiveRunLine> lines, Function<IncentiveRunLine, String> key) {
    Map<String, BigDecimal> out = new TreeMap<>();
    lines.forEach(l -> out.merge(key.apply(l), l.getIncentive(), BigDecimal::add));
    return out;
  }

  /**
   * What every posting of a run shares.
   *
   * @param run run
   * @param currency company currency
   * @param branchId head office
   * @param valueDate posting date
   */
  private record Context(IncentiveRun run, String currency, Long branchId, LocalDate valueDate) {}
}
