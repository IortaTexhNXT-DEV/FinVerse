package com.iortatechnxt.brokerverse.crm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.crm.domain.Client;
import com.iortatechnxt.brokerverse.crm.domain.ClientDetails;
import com.iortatechnxt.brokerverse.crm.domain.ClientDetails.Contact;
import com.iortatechnxt.brokerverse.crm.domain.ClientDetails.Identity;
import com.iortatechnxt.brokerverse.crm.domain.ClientDetails.PersonName;
import com.iortatechnxt.brokerverse.crm.domain.ClientStatus;
import com.iortatechnxt.brokerverse.crm.domain.ClientType;
import com.iortatechnxt.brokerverse.crm.service.ClientService;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.support.TestData;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@IntegrationTest
class ClientServiceIT {

  @Autowired private ClientService clients;
  @Autowired private AsUser as;
  @Autowired private TestData data;

  private static ClientDetails person(String idType) {
    return new ClientDetails(
        ClientType.INDIVIDUAL,
        new PersonName("Dela Cruz", "Juan", "Santos", "Jr.", null),
        LocalDate.of(1985, 5, 1),
        new Identity("123-456-789-000", idType, "P1234567"),
        new Contact(
            "juan@example.ph",
            "09171234567",
            null,
            "1 Ayala Ave",
            "Makati",
            "Metro Manila",
            "1226"),
        "CBG",
        true,
        "CIF-1");
  }

  @Test
  void prospectGetsAProspectCodeAndIsNotYetConfirmed() {
    Client p =
        as.run("ao", () -> clients.createProspect(data.company().getId(), person("PASSPORT")));
    assertThat(p.getStatus()).isEqualTo(ClientStatus.PROSPECT);
    assertThat(p.getProspectCode()).startsWith("PR-");
    assertThat(p.getCode()).isEqualTo(p.getProspectCode());
    assertThat(p.getDisplayName()).isEqualTo("Dela Cruz, Juan Santos Jr.");
    assertThat(clients.requireByCode(data.company().getId(), p.getProspectCode()).getId())
        .isEqualTo(p.getId());
    assertThat(clients.requireUsable(p.getId()).getId()).isEqualTo(p.getId());
    assertThatThrownBy(() -> clients.requireConfirmed(p.getId()))
        .extracting("code")
        .isEqualTo("CLIENT_NOT_CONFIRMED");

    Client corp =
        as.run(
            "ao",
            () ->
                clients.update(
                    p.getId(),
                    new ClientDetails(
                        ClientType.CORPORATE,
                        new PersonName(null, null, null, null, "Dela Cruz Trading Corp."),
                        null,
                        null,
                        null,
                        null,
                        false,
                        null)));
    assertThat(corp.getDisplayName()).isEqualTo("Dela Cruz Trading Corp.");
    assertThat(corp.getLastName()).isNull();
  }

  @Test
  void validatesListsAndNames() {
    Long company = data.company().getId();
    assertThatThrownBy(
            () -> as.run("ao", () -> clients.createProspect(company, person("NOT_AN_ID"))))
        .extracting("code")
        .isEqualTo("LOV_VALUE_INVALID");
    assertThatThrownBy(
            () ->
                as.run(
                    "ao",
                    () ->
                        clients.createProspect(
                            company,
                            new ClientDetails(
                                ClientType.INDIVIDUAL,
                                new PersonName(" ", "Juan", null, null, null),
                                null,
                                null,
                                null,
                                null,
                                false,
                                null))))
        .extracting("code")
        .isEqualTo("CLIENT_NAME_REQUIRED");
  }
}
