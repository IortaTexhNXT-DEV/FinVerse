package com.iortatechnxt.brokerverse.crm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.crm.domain.Client;
import com.iortatechnxt.brokerverse.crm.domain.ClientDetails;
import com.iortatechnxt.brokerverse.crm.domain.ClientType;
import com.iortatechnxt.brokerverse.crm.service.ClientIdentityChanged;
import com.iortatechnxt.brokerverse.crm.service.ClientRegistered;
import com.iortatechnxt.brokerverse.crm.service.ClientRiskProfile;
import com.iortatechnxt.brokerverse.crm.service.ClientRiskService;
import com.iortatechnxt.brokerverse.crm.service.ClientService;
import com.iortatechnxt.brokerverse.crm.service.RiskProfileChange;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

/**
 * crm contract of Sanction Screening (BRD-10, SANCTION_SCREENING_DESIGN section 9): the events
 * {@link ClientRegistered} and {@link ClientIdentityChanged}, and the risk profile set through
 * {@link ClientRiskService} (SNSRP-302, 304).
 */
@IntegrationTest
@RecordApplicationEvents
class ClientRiskServiceIT {

  @Autowired private ClientService clients;
  @Autowired private ClientRiskService risk;
  @Autowired private CrmFixtures fixtures;
  @Autowired private AsUser as;
  @Autowired private ApplicationEvents events;

  @Test
  void creationAndIdentityChangesArePublished() {
    Client client = fixtures.prospect(CrmFixtures.person());
    assertThat(events.stream(ClientRegistered.class))
        .filteredOn(e -> e.clientId().equals(client.getId()))
        .singleElement()
        .satisfies(
            e -> {
              assertThat(e.code()).isEqualTo(client.getProspectCode());
              assertThat(e.type()).isEqualTo(ClientType.INDIVIDUAL);
              assertThat(e.companyId()).isEqualTo(fixtures.company());
            });

    ClientDetails same = detailsOf(client, client.getLastName(), "contact-" + client.getId());
    as.run("ao", () -> clients.update(client.getId(), same));
    assertThat(events.stream(ClientIdentityChanged.class)).isEmpty();

    ClientDetails renamed = detailsOf(client, "Renamed" + CrmFixtures.word(5), null);
    as.run("ao", () -> clients.update(client.getId(), renamed));
    assertThat(events.stream(ClientIdentityChanged.class))
        .singleElement()
        .satisfies(e -> assertThat(e.clientId()).isEqualTo(client.getId()));
  }

  private static ClientDetails detailsOf(Client c, String lastName, String email) {
    ClientDetails.Contact contact =
        new ClientDetails.Contact(
            email == null ? c.getEmail() : email + "@test-client.ph",
            c.getMobile(),
            null,
            "1 Test Street",
            "Makati",
            "Metro Manila",
            "1200");
    return new ClientDetails(
        ClientType.INDIVIDUAL,
        new ClientDetails.PersonName(lastName, c.getFirstName(), null, null, null),
        c.getBirthDate(),
        new ClientDetails.Identity(c.getTin(), c.getIdType(), c.getIdNumber()),
        contact,
        "CBG",
        false,
        null);
  }

  @Test
  void aHighRatingTagsTheClientAndBringsTheReviewForward() {
    Client client = fixtures.confirmed(CrmFixtures.person());
    Client before = clients.get(client.getId());
    assertThat(before.getKycReviewDue()).isNotNull();

    ClientRiskProfile high =
        as.run(
            "ao",
            () ->
                risk.applyRiskProfile(
                    client.getId(),
                    new RiskProfileChange(
                        "HIGH",
                        Set.of("PEP", "WATCHLIST_REVIEW"),
                        null,
                        ClientRiskService.SOURCE_RULE,
                        "Rule PEP-1",
                        "SCR-2026-000001")));
    assertThat(high.changed()).isTrue();
    assertThat(high.previousRating()).isEqualTo("STANDARD");
    assertThat(high.riskRating()).isEqualTo("HIGH");
    assertThat(high.activeTags()).contains("PEP", "WATCHLIST_REVIEW");
    assertThat(high.tagsAdded()).containsExactlyInAnyOrder("PEP", "WATCHLIST_REVIEW");
    Client after = clients.get(client.getId());
    assertThat(after.getRiskRating()).isEqualTo("HIGH");
    assertThat(after.profile().occupation()).isEqualTo(before.profile().occupation());
    assertThat(after.getKycReviewDue()).isBefore(before.getKycReviewDue());

    ClientRiskProfile again =
        as.run(
            "ao",
            () ->
                risk.applyRiskProfile(
                    client.getId(),
                    new RiskProfileChange(
                        null, Set.of("PEP"), null, ClientRiskService.SOURCE_RULE, null, null)));
    assertThat(again.changed()).as("idempotent").isFalse();

    ClientRiskProfile cleared =
        as.run(
            "ao",
            () ->
                risk.applyRiskProfile(
                    client.getId(),
                    new RiskProfileChange(
                        "STANDARD",
                        null,
                        Set.of("WATCHLIST_REVIEW", "VIP"),
                        ClientRiskService.SOURCE_MANUAL,
                        "False positive confirmed",
                        "SCR-2026-000001")));
    assertThat(cleared.tagsRemoved()).containsExactly("WATCHLIST_REVIEW");
    assertThat(risk.activeTags(client.getId())).containsExactly("PEP");
    assertThat(clients.get(client.getId()).getKycReviewDue()).isEqualTo(after.getKycReviewDue());
  }

  @Test
  void invalidChangesAreRefused() {
    Client client = fixtures.prospect(CrmFixtures.person());
    Long id = client.getId();
    assertThatThrownBy(
            () ->
                as.run(
                    "ao",
                    () ->
                        risk.applyRiskProfile(
                            id,
                            new RiskProfileChange(
                                "HIGH", null, null, ClientRiskService.SOURCE_MANUAL, " ", null))))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("justification");
    assertThatThrownBy(
            () ->
                as.run(
                    "ao",
                    () ->
                        risk.applyRiskProfile(
                            id, new RiskProfileChange("HIGH", null, null, "OTHER", "x", null))))
        .isInstanceOf(BusinessRuleException.class);
    assertThatThrownBy(
            () ->
                as.run(
                    "ao",
                    () ->
                        risk.applyRiskProfile(
                            id,
                            new RiskProfileChange(
                                "EXTREME", null, null, ClientRiskService.SOURCE_RULE, null, null))))
        .isInstanceOf(BusinessRuleException.class);
    ClientRiskProfile prospect =
        as.run(
            "ao",
            () ->
                risk.applyRiskProfile(
                    id,
                    new RiskProfileChange(
                        "HIGH", null, null, ClientRiskService.SOURCE_RULE, null, null)));
    assertThat(prospect.kycReviewDue()).as("no review cycle before verification").isNull();
  }
}
