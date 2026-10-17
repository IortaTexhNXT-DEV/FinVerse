package com.iortatechnxt.brokerverse.renewal.holdcover;

import static com.iortatechnxt.brokerverse.renewal.RenewalFixtures.PO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.bulk.service.BulkContext;
import com.iortatechnxt.brokerverse.bulk.service.BulkRow;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.renewal.RenewalFixtures;
import com.iortatechnxt.brokerverse.renewal.allocation.service.InsurerAllocationService;
import com.iortatechnxt.brokerverse.renewal.domain.HoldCoverAsk;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.renewal.holdcover.service.HoldCoverBatches;
import com.iortatechnxt.brokerverse.renewal.holdcover.service.HoldCoverRequests;
import com.iortatechnxt.brokerverse.renewal.holdcover.service.HoldCoverResponseHandler;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Hold cover requests and insurer allocation of BDOI's FRS against the real database (FRRN.014.03,
 * FRRN.036, FRRN.037): default durations, one request per insurer, the extension window, the
 * response, and the monthly CBG Home extension.
 */
@IntegrationTest
class RenewalHoldCoverRequestsIT {

  @Autowired private RenewalFixtures fx;
  @Autowired private HoldCoverRequests requests;
  @Autowired private HoldCoverBatches batches;
  @Autowired private HoldCoverResponseHandler responses;
  @Autowired private InsurerAllocationService allocations;
  @Autowired private SystemParameterService parameters;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private AsUser as;
  @Autowired private Clock clock;

  private String otherInsurer(RenewalCandidate c) {
    return jdbc.queryForObject(
        "select party_code from cat_insurer where company_id = ? and party_code <> ?"
            + " and placement_emails is not null order by party_code limit 1",
        String.class,
        fx.company(),
        c.getSnapshot().insurerCode());
  }

  @Test
  void
      theDefaultDurationIsThirtyDaysForNonCbgAndNinetyForCbgAndSixtyForAMotorMovedAfterRejection() {
    RenewalCandidate retail = fx.unassignedRetail();
    assertThat(as.run(PO, () -> requests.defaultDays(fx.reload(retail)))).isEqualTo(30);
    RenewalCandidate cbg = fx.extractedMotor();
    assertThat(as.run(PO, () -> requests.defaultDays(fx.reload(cbg)))).isEqualTo(90);
    List<HoldCoverAsk> asked =
        as.run(PO, () -> requests.request(fx.company(), cbg.getRenewalRef(), null, null));
    assertThat(asked).hasSize(1);
    assertThat(asked.get(0).getDays()).isEqualTo(90);
    as.run(
        PO,
        () ->
            requests.respond(
                fx.company(),
                asked.get(0).getRequestNo(),
                new HoldCoverRequests.Response(false, null, null, "Old unit")));
    assertThat(as.run(PO, () -> requests.defaultDays(fx.reload(cbg)))).isEqualTo(60);
    assertThat(fx.reload(cbg).getFlags().isReturned()).isTrue();
  }

  @Test
  void anAccountAllocatedToTwoInsurersGetsOneRequestPerInsurerAndASecondRequestIsRefused() {
    RenewalCandidate c = fx.unassignedRetail();
    String lead = c.getSnapshot().insurerCode();
    String other = otherInsurer(c);
    assertThatThrownBy(
            () ->
                as.run(
                    PO,
                    () ->
                        allocations.allocate(
                            fx.company(),
                            c.getRenewalRef(),
                            List.of(
                                new InsurerAllocationService.Input(
                                    lead, BigDecimal.valueOf(60), null),
                                new InsurerAllocationService.Input(
                                    other, BigDecimal.valueOf(30), null)))))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("100%");
    var shares =
        as.run(
            PO,
            () ->
                allocations.allocate(
                    fx.company(),
                    c.getRenewalRef(),
                    List.of(
                        new InsurerAllocationService.Input(lead, BigDecimal.valueOf(60), null),
                        new InsurerAllocationService.Input(other, BigDecimal.valueOf(40), null))));
    assertThat(shares).hasSize(2);
    if (shares.get(0).premium() != null) {
      assertThat(shares.get(0).premium().add(shares.get(1).premium()))
          .isEqualByComparingTo(fx.reload(c).getSnapshot().premium().grossPremium());
    }
    List<HoldCoverAsk> asked =
        as.run(PO, () -> requests.request(fx.company(), c.getRenewalRef(), null, null));
    assertThat(asked).extracting(HoldCoverAsk::getInsurerCode).containsExactly(lead, other);
    assertThatThrownBy(
            () -> as.run(PO, () -> requests.request(fx.company(), c.getRenewalRef(), null, null)))
        .isInstanceOf(BusinessRuleException.class);
    as.run(
        PO,
        () -> {
          requests.cancel(fx.company(), c.getRenewalRef());
          return null;
        });
    assertThat(as.run(PO, () -> requests.request(fx.company(), c.getRenewalRef(), 30, null)))
        .hasSize(2);
  }

  @Test
  void anAllocationByAmountMustTotalTheSumInsured() {
    RenewalCandidate c = fx.unassignedRetail();
    BigDecimal tsi = c.getSnapshot().premium().totalSumInsured();
    BigDecimal first = tsi.multiply(new BigDecimal("0.6")).setScale(2, RoundingMode.HALF_UP);
    var shares =
        as.run(
            PO,
            () ->
                allocations.allocate(
                    fx.company(),
                    c.getRenewalRef(),
                    List.of(
                        new InsurerAllocationService.Input(
                            c.getSnapshot().insurerCode(), null, first),
                        new InsurerAllocationService.Input(
                            otherInsurer(c), null, tsi.subtract(first)))));
    assertThat(shares.get(0).percent()).isEqualByComparingTo("60");
  }

  @Test
  void anApprovedHoldCoverMovesTheEffectiveExpiryAndCanBeExtendedFromSevenDaysBeforeItsEnd() {
    RenewalCandidate c = fx.unassignedRetail();
    HoldCoverAsk a =
        as.run(PO, () -> requests.request(fx.company(), c.getRenewalRef(), 30, null)).get(0);
    BulkContext ctx =
        new BulkContext(fx.company(), "HC-" + c.getId(), LocalDate.of(2026, 10, 1), Map.of());
    assertThat(
            responses.validate(
                new BulkRow(
                    2,
                    Map.of("Reference Number", "HCR-0000-999999", "Insurer Response", "Approved")),
                ctx))
        .anyMatch(e -> e.startsWith("Record not found"));
    LocalDate early = a.getStartDate().minusDays(1);
    assertThat(
            responses.validate(
                new BulkRow(
                    4,
                    Map.of(
                        "Reference Number", a.getRequestNo(),
                        "Insurer Response", "Approved",
                        "Hold Cover End Date", early.toString())),
                ctx))
        .anyMatch(e -> e.contains("cannot be before"));
    LocalDate soon = a.getStartDate().plusDays(20);
    BulkRow approve =
        new BulkRow(
            3,
            Map.of(
                "Reference Number", a.getRequestNo(),
                "Insurer Response", "Approved",
                "Hold Cover End Date", soon.toString()));
    assertThat(responses.validate(approve, ctx)).isEmpty();
    as.run(
        PO,
        () ->
            responses.process(
                new BulkRow(
                    3,
                    Map.of(
                        "Reference Number", a.getRequestNo(),
                        "Insurer Response", "Approved",
                        "Hold Cover End Date", soon.toString())),
                ctx));
    assertThat(fx.reload(c).effectiveExpiry())
        .isEqualTo(soon.isAfter(c.getExpiryDate()) ? soon : c.getExpiryDate());
    assertThat(fx.reload(c).getExpiry().getHoldCoverUntil()).isEqualTo(soon);
    assertThatThrownBy(() -> as.run(PO, () -> requests.extend(fx.company(), c.getRenewalRef(), 20)))
        .isInstanceOf(BusinessRuleException.class);
    assertThatThrownBy(() -> as.run(PO, () -> requests.extend(fx.company(), c.getRenewalRef(), 15)))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("can be requested from");
  }

  @Test
  void onTheFifteenthTheExpiredCbgHomeAccountsGetAThirtyDayExtension() {
    RenewalCandidate c = fx.extracted(fx.book("PAR01", "CBG"));
    LocalDate fifteenth = c.getExpiryDate().plusMonths(1).withDayOfMonth(15);
    int created = as.run(PO, () -> batches.cbgHomeExtensions(fifteenth));
    assertThat(created).isPositive();
    assertThat(as.run(PO, () -> requests.of(fx.company(), c.getRenewalRef())))
        .anyMatch(r -> HoldCoverAsk.MONTHLY_EXTENSION.equals(r.getKind()) && r.getDays() == 30);
    assertThat(as.run(PO, () -> batches.cbgHomeExtensions(fifteenth.plusDays(1)))).isZero();
    assertThat(parameters.intValue("RNW_HOLD_COVER_CBG_HOME_EXTENSION_DAY", 0)).isEqualTo(15);
  }
}
