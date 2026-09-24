package com.iortatechnxt.brokerverse.account.service;

import com.iortatechnxt.brokerverse.account.domain.AccountContact;
import com.iortatechnxt.brokerverse.account.domain.AccountData;
import com.iortatechnxt.brokerverse.account.domain.AccountData.ClientRef;
import com.iortatechnxt.brokerverse.account.domain.AccountData.ProductRef;
import com.iortatechnxt.brokerverse.account.domain.RiskItemData;
import com.iortatechnxt.brokerverse.catalog.domain.ProductLine;
import com.iortatechnxt.brokerverse.catalog.domain.RiskProduct;
import com.iortatechnxt.brokerverse.catalog.service.InsurerService;
import com.iortatechnxt.brokerverse.catalog.service.ProductCatalogService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.crm.domain.Client;
import com.iortatechnxt.brokerverse.crm.service.ClientService;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;

/**
 * Validates and resolves an account draft against the client, the product and the lists of values
 * (BRNB.051/053/109/112/113/114): usable client and product, the product's terms ({@link
 * AccountTerms}), active insurer branch, valid codes, and the account contact defaulted from the
 * client.
 */
@Component
public class AccountRules {

  private final ClientService clients;
  private final ProductCatalogService catalog;
  private final InsurerService insurers;
  private final LovService lovs;
  private final Clock clock;

  /**
   * Creates the rules.
   *
   * @param clients clients
   * @param catalog products
   * @param insurers insurers
   * @param lovs lists of values
   * @param clock clock
   */
  public AccountRules(
      ClientService clients,
      ProductCatalogService catalog,
      InsurerService insurers,
      LovService lovs,
      Clock clock) {
    this.clients = clients;
    this.catalog = catalog;
    this.insurers = insurers;
    this.lovs = lovs;
    this.clock = clock;
  }

  /**
   * Resolves a draft into account data.
   *
   * @param companyId company
   * @param draft draft
   * @return resolved data with the product
   */
  public Resolved resolve(Long companyId, AccountDraft draft) {
    if (draft.clientId() == null) {
      throw new BusinessRuleException("ACCOUNT_CLIENT_REQUIRED", "Select the client");
    }
    Client client = clients.requireUsable(draft.clientId());
    return resolve(
        companyId,
        draft,
        new ClientRef(client.getId(), client.getCode(), client.getDisplayName()),
        contactOf(draft.contact(), client));
  }

  /**
   * Resolves a draft for a client that does not exist yet (bulk validation of a row that will
   * create a prospect): everything but the client is checked.
   *
   * @param companyId company
   * @param draft draft (client id ignored)
   * @param clientName name of the prospect to create
   * @return resolved data (client id null) with the product
   */
  public Resolved resolveForNewClient(Long companyId, AccountDraft draft, String clientName) {
    AccountContact contact = draft.contact() == null ? AccountContact.NONE : draft.contact();
    return resolve(companyId, draft, new ClientRef(null, null, clientName), contact);
  }

  private Resolved resolve(
      Long companyId, AccountDraft draft, ClientRef client, AccountContact contact) {
    if (draft.productCode() == null || draft.productCode().isBlank()) {
      throw new BusinessRuleException("ACCOUNT_PRODUCT_REQUIRED", "Select the product");
    }
    RiskProduct product = catalog.requireUsableProduct(draft.productCode());
    ProductLine line = catalog.requireLine(product.getLineCode());
    AccountTerms.check(product, draft);
    checkCodes(draft);
    checkInsurer(companyId, draft);
    ProductRef ref =
        new ProductRef(
            product.getCode(),
            product.getLineCode(),
            product.getCoverTypeCode(),
            line.getRiskItemKind());
    return new Resolved(AccountTerms.dataOf(draft, client, ref, contact), product);
  }

  private void checkInsurer(Long companyId, AccountDraft draft) {
    if (draft.insurerCode() == null) {
      return;
    }
    insurers.requireUsableInsurer(companyId, draft.insurerCode());
    if (draft.insurerBranch() != null) {
      insurers.requireUsableBranch(companyId, draft.insurerCode(), draft.insurerBranch());
    }
  }

  private void checkCodes(AccountDraft draft) {
    LocalDate today = LocalDate.now(clock);
    lovs.validateOptional("MARKET_SEGMENT", draft.marketSegment(), today);
    lovs.validateOptional("SOURCE_CHANNEL", draft.sourceChannel(), today);
    if (draft.mortgage() != null) {
      lovs.validateOptional("MORTGAGEE_BANK", draft.mortgage().bank(), today);
    }
    for (RiskItemData item : draft.items()) {
      if (item.vehicle() != null) {
        lovs.validateOptional("VEHICLE_BODY_TYPE", item.vehicle().bodyType(), today);
      }
      if (item.location() != null) {
        lovs.validateOptional("OCCUPANCY", item.location().occupancy(), today);
        lovs.validateOptional("CONSTRUCTION_CLASS", item.location().constructionClass(), today);
      }
    }
  }

  private static AccountContact contactOf(AccountContact given, Client client) {
    if (given != null && !given.isEmpty()) {
      return given;
    }
    String address =
        Stream.of(client.getAddressLine(), client.getCity(), client.getProvince())
            .filter(s -> s != null && !s.isBlank())
            .collect(Collectors.joining(", "));
    return new AccountContact(
        client.getDisplayName(),
        client.getEmail(),
        client.getMobile(),
        address.isEmpty() ? null : address);
  }

  /**
   * A resolved draft.
   *
   * @param data account data
   * @param product product
   */
  public record Resolved(AccountData data, RiskProduct product) {}
}
