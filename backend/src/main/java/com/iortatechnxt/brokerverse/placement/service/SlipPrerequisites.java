package com.iortatechnxt.brokerverse.placement.service;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.account.domain.AccountStatus;
import com.iortatechnxt.brokerverse.account.domain.PaymentStatus;
import com.iortatechnxt.brokerverse.account.service.AccountService;
import com.iortatechnxt.brokerverse.attachment.domain.AttachmentTarget;
import com.iortatechnxt.brokerverse.attachment.service.DocumentService;
import com.iortatechnxt.brokerverse.catalog.service.ProductCatalogService;
import com.iortatechnxt.brokerverse.catalog.service.ProductRuleService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Prerequisites of a placement slip (BRNB.069, Q26): the account is ready for placement, its
 * payment is confirmed (or paid directly to the insurer), its mandatory documents are complete, TSU
 * cleared it when required, and its insurer branch is usable and reachable by e-mail. Each unmet
 * prerequisite is reported separately so the screen can show all of them.
 */
@Component
public class SlipPrerequisites {

  private final ProductCatalogService catalog;
  private final ProductRuleService productRules;
  private final DocumentService documents;
  private final InsurerDirectory insurers;

  /**
   * Creates the check.
   *
   * @param catalog products
   * @param productRules required documents per product
   * @param documents account documents
   * @param insurers insurer addressing
   */
  public SlipPrerequisites(
      ProductCatalogService catalog,
      ProductRuleService productRules,
      DocumentService documents,
      InsurerDirectory insurers) {
    this.catalog = catalog;
    this.productRules = productRules;
    this.documents = documents;
    this.insurers = insurers;
  }

  /**
   * The unmet prerequisites of an account.
   *
   * @param account account
   * @return unmet prerequisites; empty when a slip may be generated
   */
  @Transactional(readOnly = true)
  public List<Unmet> check(Account account) {
    List<Unmet> unmet = new ArrayList<>();
    if (account.getStatus() != AccountStatus.READY_FOR_PLACEMENT) {
      unmet.add(
          new Unmet(
              "NOT_READY_FOR_PLACEMENT",
              "The account is " + account.getStatus() + "; a slip needs Ready for placement"));
    }
    if (!account.isDirectPayment()
        && account.getLifecycle().getPaymentStatus() == PaymentStatus.UNPAID) {
      unmet.add(
          new Unmet("PAYMENT_NOT_CONFIRMED", "The payment or client confirmation is not recorded"));
    }
    missingDocuments(account).ifPresent(unmet::add);
    if (!account.getTsu().satisfied()) {
      unmet.add(
          new Unmet(
              "TSU_NOT_CLEARED",
              "TSU clearance is required (" + account.getTsu().rule() + ") and not given"));
    }
    insurer(account).ifPresent(unmet::add);
    return unmet;
  }

  private Optional<Unmet> missingDocuments(Account account) {
    Set<String> required =
        productRules.requiredDocuments(catalog.requireProduct(account.getProductCode()));
    Set<String> present =
        documents.documentTypesOf(
            new AttachmentTarget(AccountService.ENTITY, String.valueOf(account.getId())));
    Set<String> missing = new TreeSet<>(required);
    missing.removeAll(present);
    return missing.isEmpty()
        ? Optional.empty()
        : Optional.of(
            new Unmet("DOCUMENTS_MISSING", "Missing documents: " + String.join(", ", missing)));
  }

  private Optional<Unmet> insurer(Account account) {
    try {
      insurers.address(
          account.getCompanyId(), account.getInsurerCode(), account.getInsurerBranch());
      return Optional.empty();
    } catch (BusinessRuleException | ResourceNotFoundException e) {
      return Optional.of(new Unmet("INSURER_NOT_USABLE", e.getMessage()));
    }
  }

  /**
   * An unmet prerequisite.
   *
   * @param code code
   * @param message explanation
   */
  public record Unmet(String code, String message) {}
}
