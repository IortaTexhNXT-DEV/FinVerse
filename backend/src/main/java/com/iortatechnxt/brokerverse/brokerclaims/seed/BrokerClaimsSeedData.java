package com.iortatechnxt.brokerverse.brokerclaims.seed;

import com.iortatechnxt.brokerverse.brokerclaims.claim.service.ClaimRecordingService;
import com.iortatechnxt.brokerverse.brokerclaims.claim.service.ClaimRecordingService.NewClaim;
import com.iortatechnxt.brokerverse.brokerclaims.claim.service.PremiumCheckService;
import com.iortatechnxt.brokerverse.brokerclaims.domain.BrokerClaimRepository;
import com.iortatechnxt.brokerverse.brokerclaims.domain.Claim;
import com.iortatechnxt.brokerverse.brokerclaims.domain.ClaimSource;
import com.iortatechnxt.brokerverse.brokerclaims.domain.LossDetails;
import com.iortatechnxt.brokerverse.brokerclaims.insurer.domain.InsurerClaim;
import com.iortatechnxt.brokerverse.brokerclaims.insurer.service.InsurerClaimService;
import com.iortatechnxt.brokerverse.brokerclaims.insurer.service.InsurerClaimService.NewLine;
import com.iortatechnxt.brokerverse.brokerclaims.insurer.service.InsurerUpdateService;
import com.iortatechnxt.brokerverse.brokerclaims.insurer.service.InsurerUpdateService.NewUpdate;
import com.iortatechnxt.brokerverse.brokerclaims.location.service.ClaimLocationService.LocationPick;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.opsledger.seed.SeedUsers;
import com.iortatechnxt.brokerverse.organization.domain.Company;
import com.iortatechnxt.brokerverse.organization.domain.CompanyRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

/**
 * Seed start-up of Claims Handling, wave CL1-A (seed profile only, idempotent), after the booking,
 * Operations and Collections runners: the Claims Officers record claims on the booked seed covers
 * through the real services - a motor claim with the authorization code when its premium is paid, a
 * property claim on a location with its insurer location reference, a typhoon tag, the insurer's
 * claim number and an insurer update, a claim on a direct-payment cover and an insurer-reported
 * claim; the Team Head amends a reserve. Wave CL2 completes the storyline ({@link
 * BrokerClaimsSeedStory}): statuses through the matrix, an adjuster, action plans, diary entries
 * and follow-ups, a temporarily closed claim, a claim settled on the LOA and closed, a claim closed
 * and reopened, and a claim waiting for the premium remittance. A step that cannot run (a seed
 * cover missing) is logged and skipped; start-up never fails.
 */
@Component
@Profile("seed")
@Order(130)
public class BrokerClaimsSeedData implements ApplicationRunner {

  private static final Logger LOG = LoggerFactory.getLogger(BrokerClaimsSeedData.class);
  private static final String OFFICER = "clmofficer";
  private static final String NON_MOTOR = "clmofficer2";
  private static final String HEAD = "clmth";
  private static final String MOTOR = "MOTOR_OWN_DAMAGE";
  private static final String FIRE = "FIRE";
  private static final int LOSS_DAYS_AGO = 3;

  private final CompanyRepository companies;
  private final BrokerClaimRepository claims;
  private final ClaimRecordingService recording;
  private final PremiumCheckService premiums;
  private final InsurerClaimService insurers;
  private final InsurerUpdateService updates;
  private final SeedUsers users;
  private final BrokerClaimsSeedStory story;
  private final Clock clock;

  /**
   * Creates the loader.
   *
   * @param companies companies
   * @param claims claims (idempotency)
   * @param recording claim recording
   * @param premiums authorization code
   * @param insurers insurer lines
   * @param updates insurer updates
   * @param users seed sign-in
   * @param story claim-level storyline (wave CL2)
   * @param clock clock
   */
  @SuppressWarnings("java:S107") // constructor injection
  public BrokerClaimsSeedData(
      CompanyRepository companies,
      BrokerClaimRepository claims,
      ClaimRecordingService recording,
      PremiumCheckService premiums,
      InsurerClaimService insurers,
      InsurerUpdateService updates,
      SeedUsers users,
      BrokerClaimsSeedStory story,
      Clock clock) {
    this.companies = companies;
    this.claims = claims;
    this.recording = recording;
    this.premiums = premiums;
    this.insurers = insurers;
    this.updates = updates;
    this.users = users;
    this.story = story;
    this.clock = clock;
  }

  @Override
  public void run(ApplicationArguments args) {
    if (claims.count() == 0) {
      load();
    }
  }

  /** Records the seed claims of every company; each step is skipped when it cannot run. */
  public void load() {
    for (Company company : companies.findAll()) {
      Long id = company.getId();
      step("motor claim", () -> story.motorInProgress(id, motor(id).getId()));
      step("property claim", () -> story.propertyOffer(id, property(id).getId()));
      step("direct payment claim", () -> story.temporarilyClosed(id, directPayment(id).getId()));
      step("insurer-reported claim", () -> story.settledOnLoa(id, insurerReported(id).getId()));
      step("reopened claim", () -> story.reopened(id, reopenedFire(id).getId()));
      step("claim awaiting remittance", () -> story.awaitingRemittance(id, awaiting(id).getId()));
      step("newly filed claim", () -> story.newlyFiled(id, liability(id).getId()));
    }
  }

  private Claim motor(Long companyId) {
    Claim claim =
        record(
            OFFICER,
            companyId,
            "ARN-2026-940001",
            ClaimSource.BDOI_NOTICE,
            loss(MOTOR, "Rear-ended at a stoplight on EDSA", "EDSA Guadalupe, Makati"),
            List.of(),
            List.of());
    try {
      users.as(OFFICER, () -> premiums.authorize(companyId, claim.getId(), null));
    } catch (BusinessRuleException ex) {
      LOG.info("Claims seed: {} not authorised: {}", claim.getClaimNo(), ex.getMessage());
    }
    return claim;
  }

  private Claim property(Long companyId) {
    LocalDate today = LocalDate.now(clock);
    LossDetails.Loss loss =
        new LossDetails.Loss(
            today.minusDays(2),
            today.minusDays(1),
            FIRE,
            "PROPERTY",
            "Roof and ceiling damaged by strong winds and rain",
            null,
            "TYPHOON",
            "Typhoon Kristine");
    Claim claim =
        record(
            NON_MOTOR,
            companyId,
            "ARN-2026-940002",
            ClaimSource.BDOI_NOTICE,
            loss,
            List.of(new LocationPick(1, "Roof and second-floor ceiling")),
            List.of());
    InsurerClaim line = insurers.ofClaim(claim.getId()).get(0);
    users.as(
        NON_MOTOR,
        () -> insurers.number(claim, line.getId(), "MGIC-CL-2026-0415", today.minusDays(1), false));
    users.as(
        NON_MOTOR,
        () ->
            updates.record(
                claim,
                new NewUpdate(
                    line.getId(),
                    today,
                    "EMAIL",
                    "MGIC-ACK-0415",
                    "Claim acknowledged; adjuster to inspect",
                    List.of(),
                    null,
                    null)));
    users.as(
        HEAD,
        () ->
            insurers.amendReserve(
                claim, line.getId(), new BigDecimal("250000.00"), "Insurer's initial estimate"));
    return claim;
  }

  private Claim directPayment(Long companyId) {
    return record(
        OFFICER,
        companyId,
        "ARN-2026-940003",
        ClaimSource.BDOI_NOTICE,
        loss(MOTOR, "Side mirror and door damaged in a parking lot", "SM Seaside, Cebu City"),
        List.of(),
        List.of());
  }

  private Claim insurerReported(Long companyId) {
    return record(
        OFFICER,
        companyId,
        "ARN-2026-940005",
        ClaimSource.INSURER_REPORTED,
        loss("MOTOR_THEFT", "Vehicle reported stolen by the assured to the insurer", "Quezon City"),
        List.of(),
        List.of(
            new NewLine(
                "INS-MGIC",
                new BigDecimal("100"),
                "MGIC-CL-2026-0402",
                LocalDate.now(clock),
                null)));
  }

  private Claim reopenedFire(Long companyId) {
    return record(
        NON_MOTOR,
        companyId,
        "ARN-2026-940006",
        ClaimSource.BDOI_NOTICE,
        loss(FIRE, "Electrical fire in the stock room", "Lahug, Cebu City"),
        List.of(),
        List.of());
  }

  private Claim awaiting(Long companyId) {
    return record(
        NON_MOTOR,
        companyId,
        "ARN-2026-940007",
        ClaimSource.BDOI_NOTICE,
        loss(FIRE, "Kitchen fire spread to the dining area", "Kapitolyo, Pasig City"),
        List.of(),
        List.of());
  }

  private Claim liability(Long companyId) {
    return record(
        NON_MOTOR,
        companyId,
        "ARN-2026-940004",
        ClaimSource.BDOI_NOTICE,
        loss("LIABILITY", "Visitor injured by falling crates in the warehouse", "Port Area"),
        List.of(),
        List.of());
  }

  private LossDetails.Loss loss(String nature, String description, String place) {
    LocalDate today = LocalDate.now(clock);
    return new LossDetails.Loss(
        today.minusDays(LOSS_DAYS_AGO),
        today.minusDays(2),
        nature,
        nature,
        description,
        place,
        null,
        null);
  }

  private Claim record(
      String user,
      Long companyId,
      String arn,
      ClaimSource source,
      LossDetails.Loss loss,
      List<LocationPick> locations,
      List<NewLine> lines) {
    return users.as(
        user,
        () ->
            recording.record(
                companyId,
                new NewClaim(
                    arn,
                    1,
                    source,
                    null,
                    loss,
                    new LossDetails.Amounts(null, null, null),
                    locations,
                    lines,
                    true,
                    true)));
  }

  private static void step(String name, Runnable work) {
    try {
      work.run();
    } catch (BusinessRuleException
        | ResourceNotFoundException
        | DataAccessException
        | IllegalStateException ex) {
      LOG.warn("Claims seed step '{}' skipped: {}", name, ex.getMessage());
    }
  }
}
