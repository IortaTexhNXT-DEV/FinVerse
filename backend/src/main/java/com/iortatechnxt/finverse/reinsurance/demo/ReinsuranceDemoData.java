package com.iortatechnxt.finverse.reinsurance.demo;

import com.iortatechnxt.finverse.organization.domain.Company;
import com.iortatechnxt.finverse.organization.service.OrganizationService;
import com.iortatechnxt.finverse.reinsurance.api.dto.FacAssignRequest;
import com.iortatechnxt.finverse.reinsurance.api.dto.TreatyRequest;
import com.iortatechnxt.finverse.reinsurance.domain.FacPlacement;
import com.iortatechnxt.finverse.reinsurance.domain.FacStatus;
import com.iortatechnxt.finverse.reinsurance.domain.Treaty;
import com.iortatechnxt.finverse.reinsurance.service.AllocationRunResult;
import com.iortatechnxt.finverse.reinsurance.service.AllocationRunService;
import com.iortatechnxt.finverse.reinsurance.service.ClaimRecoveryService;
import com.iortatechnxt.finverse.reinsurance.service.FacPlacementService;
import com.iortatechnxt.finverse.reinsurance.service.TreatyService;
import com.iortatechnxt.finverse.system.domain.JobTrigger;
import com.iortatechnxt.finverse.underwriting.demo.DemoUserContext;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * DEMO PROFILE ONLY: creates the reinsurance demo data of the demo company (FVI) at start-up
 * through the services, after the underwriting portfolio ({@code @Order(10)}) and before claims
 * ({@code @Order(20)}), so that every demo claim movement finds the cession of its policy:
 *
 * <ol>
 *   <li>the 2026 treaty programme (made by the reinsurance officer "reinsurer", authorized by the
 *       finance manager "fmanager");
 *   <li>an RI allocation run over the approved underwriting portfolio;
 *   <li>facultative placements: most placed (a few closed), some submitted, the rest provisional;
 *   <li>claim movements posted before any treaty existed (none in the standard demo order),
 *       replayed through the claims port when the claims module is deployed.
 * </ol>
 *
 * <p>The statements of account come later, from {@link ReinsuranceStatementsDemoData}, so that they
 * include the reinsurers' share of the demo claims.
 *
 * <p>Idempotent: nothing is created when the company already has treaties.
 */
@Component
@Profile("demo")
@Order(15)
public class ReinsuranceDemoData implements ApplicationRunner {

  static final String MAKER = "reinsurer";
  static final String CHECKER = "fmanager";

  private static final Logger LOG = LoggerFactory.getLogger(ReinsuranceDemoData.class);
  private static final String DEMO_COMPANY = "FVI";
  private static final LocalDate YEAR_START = LocalDate.of(DemoTreaties.YEAR, 1, 1);
  private static final LocalDate LAST_POSTING = LocalDate.of(DemoTreaties.YEAR, 9, 22);
  private static final int PLACEMENT_LAG_DAYS = 14;
  private static final int SUBMIT_EVERY = 4;
  private static final int PROVISIONAL_EVERY = 5;
  private static final int CLOSE_EVERY = 3;
  private static final BigDecimal LEAD_SHARE = BigDecimal.valueOf(60);
  private static final BigDecimal FOLLOW_SHARE = BigDecimal.valueOf(30);
  private static final BigDecimal FAC_COMMISSION = BigDecimal.valueOf(20);

  private final OrganizationService organization;
  private final TreatyService treaties;
  private final AllocationRunService allocation;
  private final FacPlacementService placements;
  private final ClaimRecoveryService claims;
  private final DemoUserContext users;

  /**
   * Creates the loader.
   *
   * @param organization companies
   * @param treaties treaties
   * @param allocation allocation run
   * @param placements facultative placements
   * @param claims claim movement catch-up
   * @param users demo user context
   */
  public ReinsuranceDemoData(
      OrganizationService organization,
      TreatyService treaties,
      AllocationRunService allocation,
      FacPlacementService placements,
      ClaimRecoveryService claims,
      DemoUserContext users) {
    this.organization = organization;
    this.treaties = treaties;
    this.allocation = allocation;
    this.placements = placements;
    this.claims = claims;
    this.users = users;
  }

  @Override
  public void run(ApplicationArguments args) {
    organization.listCompanies().stream()
        .filter(c -> DEMO_COMPANY.equals(c.getCode()))
        .findFirst()
        .map(Company::getId)
        .ifPresent(this::loadIfEmpty);
  }

  private void loadIfEmpty(Long companyId) {
    if (treaties.list(companyId).isEmpty()) {
      load(companyId);
    } else {
      LOG.info("Reinsurance demo data already present, skipped");
    }
  }

  /**
   * Creates the demo data (no idempotency check).
   *
   * @param companyId demo company
   * @return treaties created
   */
  public List<Treaty> load(Long companyId) {
    List<Treaty> created = new ArrayList<>();
    for (TreatyRequest r : DemoTreaties.requests(companyId)) {
      Treaty draft = users.runAs(MAKER, () -> treaties.create(r));
      created.add(users.runAs(CHECKER, () -> treaties.authorize(draft.getId())));
    }
    AllocationRunResult run =
        users.runAs(
            MAKER, () -> allocation.post(companyId, YEAR_START, LAST_POSTING, JobTrigger.MANUAL));
    int placed = placeFacultative(companyId);
    int replayed = users.runAs(MAKER, () -> claims.catchUp(companyId, YEAR_START, LAST_POSTING));
    LOG.info(
        "Reinsurance demo data created: {} treaties, {} cessions, {} FAC placed, {} claim"
            + " movements",
        created.size(),
        run.ceded(),
        placed,
        replayed);
    return created;
  }

  private int placeFacultative(Long companyId) {
    List<FacPlacement> provisional =
        users.runAs(MAKER, () -> placements.list(companyId, FacStatus.PROVISIONAL));
    int placed = 0;
    for (int i = 0; i < provisional.size(); i++) {
      if (i % PROVISIONAL_EVERY == PROVISIONAL_EVERY - 1) {
        continue;
      }
      Long id = provisional.get(i).getId();
      LocalDate on =
          capped(provisional.get(i).getCession().getRiDate().plusDays(PLACEMENT_LAG_DAYS));
      users.runAs(
          MAKER,
          () ->
              placements.assign(
                  id,
                  new FacAssignRequest(
                      List.of(
                          new FacAssignRequest.Line(
                              DemoTreaties.LONDON, LEAD_SHARE, FAC_COMMISSION),
                          new FacAssignRequest.Line(
                              DemoTreaties.ASIA, FOLLOW_SHARE, FAC_COMMISSION)),
                      "Slip placed through " + DemoTreaties.BROKER)));
      users.runAs(MAKER, () -> placements.submit(id));
      if (i % SUBMIT_EVERY != SUBMIT_EVERY - 1) {
        users.runAs(CHECKER, () -> placements.approve(id, on));
        placed++;
        if (i % CLOSE_EVERY == 0) {
          users.runAs(MAKER, () -> placements.close(id, capped(on.plusDays(PLACEMENT_LAG_DAYS))));
        }
      }
    }
    return placed;
  }

  private static LocalDate capped(LocalDate date) {
    return date.isAfter(LAST_POSTING) ? LAST_POSTING : date;
  }
}
