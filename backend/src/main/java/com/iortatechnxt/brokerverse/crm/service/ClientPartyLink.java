package com.iortatechnxt.brokerverse.crm.service;

import com.iortatechnxt.brokerverse.crm.domain.Client;
import com.iortatechnxt.brokerverse.crm.domain.ClientType;
import com.iortatechnxt.brokerverse.organization.service.OrganizationService;
import com.iortatechnxt.brokerverse.party.api.dto.PartyRequest;
import com.iortatechnxt.brokerverse.party.domain.Party;
import com.iortatechnxt.brokerverse.party.domain.PartyType;
import com.iortatechnxt.brokerverse.party.service.PartyService;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;

/**
 * Link between a confirmed client and its sub-ledger party (BRNB.090/099): the party carries the
 * client's premium receivables, its code is the client code.
 */
@Component
public class ClientPartyLink {

  /** Default credit terms of a client party in days. */
  private static final int CREDIT_DAYS = 30;

  private static final int MAX_NAME = 200;
  private static final int MAX_ADDRESS = 300;
  private static final int MAX_PHONE = 40;

  private final PartyService parties;
  private final OrganizationService organization;

  /**
   * Creates the component.
   *
   * @param parties party master
   * @param organization companies
   */
  public ClientPartyLink(PartyService parties, OrganizationService organization) {
    this.parties = parties;
    this.organization = organization;
  }

  /**
   * Opens the client's party (type INDIVIDUAL_CLIENT or CORPORATE_CLIENT) and authorizes it as a
   * system step: the client itself was verified under four eyes.
   *
   * @param client client being confirmed
   * @param clientCode its new client code (party code)
   * @return party code
   */
  public String openParty(Client client, String clientCode) {
    String currency = organization.getCompany(client.getCompanyId()).getBaseCurrency();
    PartyRequest request =
        new PartyRequest(
            client.getCompanyId(),
            clientCode,
            clip(client.getDisplayName(), MAX_NAME),
            client.getClientType() == ClientType.CORPORATE
                ? PartyType.CORPORATE_CLIENT
                : PartyType.INDIVIDUAL_CLIENT,
            client.getTin(),
            clip(address(client), MAX_ADDRESS),
            client.getEmail(),
            clip(client.getMobile() != null ? client.getMobile() : client.getPhone(), MAX_PHONE),
            currency,
            CREDIT_DAYS,
            null,
            null,
            null,
            null,
            null,
            null);
    return parties
        .createAuthorizedBySystem(
            request, "sub-ledger party of confirmed client " + clientCode + " (KYC verified)")
        .getCode();
  }

  /**
   * The party of a confirmed client, when it exists.
   *
   * @param client client
   * @return party
   */
  public Optional<Party> partyOf(Client client) {
    if (client.getPartyCode() == null) {
      return Optional.empty();
    }
    return parties.search(client.getCompanyId(), List.of(), client.getPartyCode()).stream()
        .filter(p -> p.getCode().equals(client.getPartyCode()))
        .findFirst();
  }

  private static String address(Client c) {
    String text =
        Stream.of(c.getAddressLine(), c.getCity(), c.getProvince(), c.getPostalCode())
            .filter(s -> s != null && !s.isBlank())
            .collect(Collectors.joining(", "));
    return text.isEmpty() ? null : text;
  }

  private static String clip(String value, int max) {
    return value == null || value.length() <= max ? value : value.substring(0, max);
  }
}
