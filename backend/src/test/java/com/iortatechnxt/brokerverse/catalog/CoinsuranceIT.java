package com.iortatechnxt.brokerverse.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.audit.service.MasterChangeLog;
import com.iortatechnxt.brokerverse.audit.service.MasterChangeLog.Entry;
import com.iortatechnxt.brokerverse.catalog.domain.InsurerParticipation;
import com.iortatechnxt.brokerverse.catalog.domain.InsurerParticipation.ParticipationDetails;
import com.iortatechnxt.brokerverse.catalog.domain.InsurerProfile;
import com.iortatechnxt.brokerverse.catalog.domain.InsurerProfile.InsurerDetails;
import com.iortatechnxt.brokerverse.catalog.domain.PlacementChannel;
import com.iortatechnxt.brokerverse.catalog.service.CatalogKind;
import com.iortatechnxt.brokerverse.catalog.service.CatalogRecords;
import com.iortatechnxt.brokerverse.catalog.service.CoinsuranceService;
import com.iortatechnxt.brokerverse.catalog.service.CoinsuranceService.DefaultShare;
import com.iortatechnxt.brokerverse.catalog.service.InsurerService;
import com.iortatechnxt.brokerverse.catalog.service.InsurerService.PartyContact;
import com.iortatechnxt.brokerverse.common.domain.RecordStatus;
import com.iortatechnxt.brokerverse.party.domain.PartyType;
import com.iortatechnxt.brokerverse.party.service.ReinsuranceFunctions;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.support.TestData;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Default co-insurance participation of an insurer (BRD-00 CRQ08) and its use at booking. */
@IntegrationTest
class CoinsuranceIT {

  private static final LocalDate FROM = LocalDate.of(2026, 1, 1);

  @Autowired private InsurerService insurers;
  @Autowired private CoinsuranceService coinsurance;
  @Autowired private CatalogRecords records;
  @Autowired private MasterChangeLog log;
  @Autowired private AsUser as;
  @Autowired private TestData data;

  private InsurerProfile newInsurer(String name) {
    String code = "INS-CO" + Long.toString(System.nanoTime() % 1_000_000L, 36).toUpperCase();
    InsurerProfile insurer =
        as.run(
            "badmin",
            () ->
                insurers.create(
                    data.company().getId(),
                    code,
                    new PartyContact(null, null, null, null),
                    new InsurerDetails(
                        name,
                        null,
                        null,
                        LocalDate.of(2027, 12, 31),
                        PlacementChannel.EMAIL,
                        List.of(),
                        30)));
    as.run("approver", () -> records.authorize(CatalogKind.INSURER, insurer.getId()));
    return insurer;
  }

  private InsurerParticipation add(InsurerProfile of, String code, String pct, boolean lead) {
    return as.run(
        "badmin",
        () ->
            coinsurance.add(
                of.getId(), new ParticipationDetails(code, new BigDecimal(pct), lead, FROM, null)));
  }

  @Test
  void sharesFollowMakerCheckerAndBecomeTheDefaultSplitAtOneHundredPercent() {
    InsurerProfile lead = newInsurer("Lead Mutual");
    InsurerProfile follower = newInsurer("Follow Assurance");
    InsurerParticipation own = add(lead, lead.getPartyCode(), "60", true);
    InsurerParticipation other = add(lead, follower.getPartyCode(), "40", false);
    assertThat(own.getRecordStatus()).isEqualTo(RecordStatus.PENDING_AUTHORIZATION);

    LocalDate on = LocalDate.of(2026, 10, 17);
    assertThat(coinsurance.defaultShares(data.company().getId(), lead.getPartyCode(), on))
        .as("pending shares are not used")
        .isEmpty();

    assertThatThrownBy(() -> add(lead, follower.getPartyCode(), "10", false))
        .extracting("code")
        .isEqualTo("COINSURANCE_SHARE_INVALID");
    InsurerProfile third = newInsurer("Third General");
    assertThatThrownBy(() -> add(lead, third.getPartyCode(), "1", false))
        .as("the period would total above 100%")
        .extracting("code")
        .isEqualTo("COINSURANCE_SHARE_INVALID");

    as.run("approver", () -> records.authorize(CatalogKind.INSURER_PARTICIPATION, own.getId()));
    assertThat(coinsurance.defaultShares(data.company().getId(), lead.getPartyCode(), on))
        .as("60% alone is not a complete arrangement")
        .isEmpty();
    as.run("approver", () -> records.authorize(CatalogKind.INSURER_PARTICIPATION, other.getId()));

    List<DefaultShare> shares =
        coinsurance.defaultShares(data.company().getId(), lead.getPartyCode(), on).orElseThrow();
    assertThat(shares)
        .extracting(DefaultShare::insurerCode)
        .containsExactly(lead.getPartyCode(), follower.getPartyCode());
    assertThat(shares.get(0).lead()).isTrue();
    assertThat(coinsurance.totalOn(lead.getId(), on)).isEqualByComparingTo("100");
    assertThat(
            coinsurance.defaultShares(
                data.company().getId(), lead.getPartyCode(), FROM.minusDays(1)))
        .as("before the effective date")
        .isEmpty();
    assertThat(log.history("InsurerParticipation", own.getId()))
        .as("the change log (G05) records the participation")
        .extracting(Entry::field)
        .isNotEmpty();
  }

  @Test
  void coInsurersAreNotPartOfTheDeferredReinsuranceFunctions() {
    assertThat(ReinsuranceFunctions.PARTY_TYPES).doesNotContain(PartyType.COINSURER);
  }
}
