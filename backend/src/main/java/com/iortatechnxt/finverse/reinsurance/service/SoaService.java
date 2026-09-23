package com.iortatechnxt.finverse.reinsurance.service;

import com.iortatechnxt.finverse.audit.domain.AuditAction;
import com.iortatechnxt.finverse.audit.service.AuditTrailService;
import com.iortatechnxt.finverse.common.exception.BusinessRuleException;
import com.iortatechnxt.finverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.finverse.common.security.CurrentUser;
import com.iortatechnxt.finverse.currency.service.CurrencyService;
import com.iortatechnxt.finverse.organization.domain.Branch;
import com.iortatechnxt.finverse.organization.service.OrganizationService;
import com.iortatechnxt.finverse.reinsurance.api.dto.SettlementRequest;
import com.iortatechnxt.finverse.reinsurance.api.dto.SoaRequest;
import com.iortatechnxt.finverse.reinsurance.domain.Soa;
import com.iortatechnxt.finverse.reinsurance.domain.SoaFigures;
import com.iortatechnxt.finverse.reinsurance.domain.SoaPeriod;
import com.iortatechnxt.finverse.reinsurance.domain.SoaRepository;
import com.iortatechnxt.finverse.reinsurance.domain.SoaStatus;
import com.iortatechnxt.finverse.reinsurance.domain.Treaty;
import com.iortatechnxt.finverse.reinsurance.domain.TreatyParticipant;
import com.iortatechnxt.finverse.reinsurance.domain.TreatyType;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Quarterly statements of account per treaty and participant: generation by the reinsurance officer
 * (regenerated while pending), approval by a checker (statement adjustments and, for excess of loss
 * treaties, the deposit premium instalment are accounted for) and settlement.
 */
@Service
@Transactional
public class SoaService {

  static final String ENTITY = "StatementOfAccount";

  private final SoaRepository statements;
  private final SoaCalculator calculator;
  private final SoaSettlement settlement;
  private final TreatyService treaties;
  private final ReinsuranceAccounting accounting;
  private final ReinsuranceNumbers numbers;
  private final OrganizationService organization;
  private final CurrencyService currencies;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param statements repository
   * @param calculator statement lines
   * @param settlement settlement and matching
   * @param treaties treaties
   * @param accounting reinsurance postings
   * @param numbers document numbers
   * @param organization head office branch
   * @param currencies currency names
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  public SoaService(
      SoaRepository statements,
      SoaCalculator calculator,
      SoaSettlement settlement,
      TreatyService treaties,
      ReinsuranceAccounting accounting,
      ReinsuranceNumbers numbers,
      OrganizationService organization,
      CurrencyService currencies,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.statements = statements;
    this.calculator = calculator;
    this.settlement = settlement;
    this.treaties = treaties;
    this.accounting = accounting;
    this.numbers = numbers;
    this.organization = organization;
    this.currencies = currencies;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Lists a company's statements.
   *
   * @param companyId company
   * @return statements, latest first
   */
  @Transactional(readOnly = true)
  public List<Soa> list(Long companyId) {
    return statements.findByCompanyIdOrderBySoaYearDescQuarterDescSoaNoAsc(companyId);
  }

  /**
   * Gets a statement.
   *
   * @param id id
   * @return statement
   */
  @Transactional(readOnly = true)
  public Soa get(Long id) {
    return statements.findById(id).orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
  }

  /**
   * Printed layout of a statement (income / outgo, balance on the smaller side, in words).
   *
   * @param soa statement
   * @return layout
   */
  @Transactional(readOnly = true)
  public SoaLayout layout(Soa soa) {
    return SoaLayout.of(soa.figures(), currencies.requireActive(soa.getCurrency()).getName());
  }

  /**
   * Generates the statements of a treaty for a quarter: one per participant, or only the one
   * requested. A statement still pending approval is recomputed; an approved one is kept.
   *
   * @param r request
   * @return statements generated or recomputed
   */
  public List<Soa> generate(SoaRequest r) {
    Treaty treaty = treaties.getByCode(r.companyId(), r.treatyCode());
    if (!treaty.isActive()) {
      throw new BusinessRuleException(
          "TREATY_NOT_ACTIVE", "Treaty " + r.treatyCode() + " is not authorized");
    }
    SoaPeriod period = period(r);
    boolean single = r.reinsurerCode() != null && !r.reinsurerCode().isBlank();
    List<Soa> out = new ArrayList<>();
    treaty.getParticipants().stream()
        .filter(p -> !single || p.getParty().getCode().equals(r.reinsurerCode()))
        .forEach(p -> generateOne(treaty, p, period, single).ifPresent(out::add));
    if (single && out.isEmpty()) {
      throw new BusinessRuleException(
          "SOA_PARTICIPANT", r.reinsurerCode() + " does not participate in " + r.treatyCode());
    }
    return out;
  }

  private static SoaPeriod period(SoaRequest r) {
    LocalDate date =
        r.statementDate() != null
            ? r.statementDate()
            : new SoaPeriod(r.year(), r.quarter(), null).to().plusDays(1);
    return new SoaPeriod(r.year(), r.quarter(), date);
  }

  private Optional<Soa> generateOne(
      Treaty treaty, TreatyParticipant p, SoaPeriod period, boolean single) {
    SoaFigures figures = calculator.compute(treaty, p, period);
    Optional<Soa> existing =
        statements.findByTreatyIdAndPartyIdAndSoaYearAndQuarter(
            treaty.getId(), p.getParty().getId(), period.year(), period.quarter());
    if (existing.isPresent()) {
      Soa soa = existing.get();
      if (soa.getStatus() != SoaStatus.PENDING_APPROVAL) {
        if (single) {
          throw new BusinessRuleException(
              "SOA_STATUS", "Statement " + soa.getSoaNo() + " is already " + soa.getStatus());
        }
        return Optional.empty();
      }
      soa.refresh(figures, period.statementDate());
      audit.record(ENTITY, soa.getSoaNo(), AuditAction.UPDATE, "Regenerated statement");
      return Optional.of(soa);
    }
    Soa soa =
        statements.save(
            new Soa(
                numbers.statement(treaty.getCompanyId(), period.year()),
                treaty,
                p.getParty(),
                period,
                figures));
    audit.record(
        ENTITY,
        soa.getSoaNo(),
        AuditAction.CREATE,
        "Statement " + treaty.getCode() + " " + p.getParty().getCode() + " Q" + period.quarter());
    return Optional.of(soa);
  }

  /**
   * Approves a statement (checker): posts the levy, reserves and interest, and the deposit premium
   * instalment of an excess of loss treaty.
   *
   * @param id statement
   * @return statement
   */
  public Soa approve(Long id) {
    Soa soa = get(id);
    if (soa.getStatus() != SoaStatus.PENDING_APPROVAL) {
      throw new BusinessRuleException(
          "SOA_STATUS", "Statement " + soa.getSoaNo() + " is " + soa.getStatus());
    }
    PostingContext ctx = context(soa, "Statement of account " + soa.getSoaNo());
    Long partyId = soa.getParty().getId();
    String ref = "RI:SOA:" + soa.getSoaNo();
    if (soa.getTreaty().getTreatyType() == TreatyType.XOL) {
      SoaFigures f = soa.figures();
      accounting.cede(ctx, ref + ":PREM", partyId, f.premium(), f.commission());
    }
    String batch = accounting.statementAdjustments(ctx, ref, partyId, soa.figures());
    soa.approve(currentUser.username(), clock.instant(), batch);
    audit.record(ENTITY, soa.getSoaNo(), AuditAction.AUTHORIZE, "Approved statement");
    return soa;
  }

  /**
   * Settles an approved statement: pays or receives the outstanding balance of its items and
   * matches the reinsurer's open items.
   *
   * @param id statement
   * @param r settlement date and bank account
   * @return statement
   */
  public Soa settle(Long id, SettlementRequest r) {
    Soa soa = get(id);
    if (soa.getStatus() != SoaStatus.APPROVED) {
      throw new BusinessRuleException(
          "SOA_STATUS",
          "Only an approved statement can be settled; "
              + soa.getSoaNo()
              + " is "
              + soa.getStatus());
    }
    PostingContext ctx =
        new PostingContext(
            soa.getCompanyId(),
            headOffice(soa.getCompanyId()),
            r.settlementDate(),
            soa.getTreaty().getBusinessLine(),
            soa.getSoaNo(),
            "Settlement of statement " + soa.getSoaNo());
    String batch = settlement.settle(soa, ctx, r.bankAccountCode());
    soa.settle(r.settlementDate(), batch, r.bankAccountCode());
    audit.record(ENTITY, soa.getSoaNo(), AuditAction.POST, "Settled statement");
    return soa;
  }

  private PostingContext context(Soa soa, String narration) {
    return new PostingContext(
        soa.getCompanyId(),
        headOffice(soa.getCompanyId()),
        soa.getStatementDate(),
        soa.getTreaty().getBusinessLine(),
        soa.getSoaNo(),
        narration);
  }

  private Long headOffice(Long companyId) {
    List<Branch> branches = organization.listBranches(companyId);
    return branches.stream()
        .filter(Branch::isHeadOffice)
        .findFirst()
        .orElse(branches.get(0))
        .getId();
  }
}
