package com.iortatechnxt.brokerverse.account.service;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.account.domain.AccountData;
import com.iortatechnxt.brokerverse.account.domain.RiskItemData;
import com.iortatechnxt.brokerverse.account.service.RiskDuplicateService.DuplicateSubject;
import com.iortatechnxt.brokerverse.attachment.domain.AttachmentTarget;
import com.iortatechnxt.brokerverse.attachment.service.DocumentService;
import com.iortatechnxt.brokerverse.catalog.domain.RiskItemKind;
import com.iortatechnxt.brokerverse.catalog.domain.RiskProduct;
import com.iortatechnxt.brokerverse.catalog.domain.TsuRule.TsuFacts;
import com.iortatechnxt.brokerverse.catalog.service.ProductCatalogService;
import com.iortatechnxt.brokerverse.catalog.service.ProductRuleService;
import com.iortatechnxt.brokerverse.catalog.service.TsuRoutingService;
import com.iortatechnxt.brokerverse.catalog.service.TsuRoutingService.TsuDecision;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.FieldValidationException;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Completeness checks of an account: minimum-field matrix (BRNB.002/003/093), mandatory documents
 * of the product (BRNB.026), duplicate fall-out (BRNB.051/066), premium computed, and the TSU
 * routing decision (BRNB.098).
 */
@Component
public class AccountChecks {

  private final ProductCatalogService catalog;
  private final ProductRuleService rules;
  private final RiskDuplicateService duplicates;
  private final TsuRoutingService tsu;
  private final DocumentService documents;
  private final LovService lovs;

  /**
   * Creates the checks.
   *
   * @param catalog products and lines
   * @param rules minimum fields and documents
   * @param duplicates duplicate fall-out
   * @param tsu TSU routing
   * @param documents account documents
   * @param lovs lists of values (document type labels)
   */
  public AccountChecks(
      ProductCatalogService catalog,
      ProductRuleService rules,
      RiskDuplicateService duplicates,
      TsuRoutingService tsu,
      DocumentService documents,
      LovService lovs) {
    this.catalog = catalog;
    this.rules = rules;
    this.duplicates = duplicates;
    this.tsu = tsu;
    this.documents = documents;
    this.lovs = lovs;
  }

  /**
   * Full readiness of a saved account.
   *
   * @param account account
   * @return check result
   */
  public AccountCheck check(Account account) {
    RiskProduct product = catalog.requireProduct(account.getProductCode());
    AccountData data = snapshot(account);
    TsuDecision decision = tsu.evaluate(product, tsuFacts(product, data));
    return new AccountCheck(
        rules.missingFields(product, AccountFields.presence(data)),
        missingDocuments(product, account),
        duplicates.findings(subject(account.getCompanyId(), account.getId(), data)),
        account.getPremium().isRated(),
        decision.required(),
        decision.ruleCode(),
        decision.reason(),
        account.getTsu().clearedAt() != null);
  }

  /**
   * Checks of an unsaved record (bulk validation): minimum fields and duplicates.
   *
   * @param companyId company
   * @param data resolved data
   * @param product product
   * @return field errors and duplicates
   */
  public AccountCheck checkData(Long companyId, AccountData data, RiskProduct product) {
    TsuDecision decision = tsu.evaluate(product, tsuFacts(product, data));
    return new AccountCheck(
        rules.missingFields(product, AccountFields.presence(data)),
        List.of(),
        duplicates.findings(subject(companyId, null, data)),
        true,
        decision.required(),
        decision.ruleCode(),
        decision.reason(),
        false);
  }

  /**
   * Rejects an account that is not complete: missing fields (with field errors), missing documents,
   * no premium, or a duplicate of a live account (naming its ARN).
   *
   * @param account account
   */
  public void requireComplete(Account account) {
    AccountCheck check = check(account);
    if (!check.fieldErrors().isEmpty()) {
      throw new FieldValidationException(
          "ACCOUNT_INCOMPLETE",
          "Complete the mandatory fields of "
              + account.getProductCode()
              + ": "
              + String.join("; ", check.fieldErrors().values()),
          check.fieldErrors());
    }
    if (!check.missingDocuments().isEmpty()) {
      throw new BusinessRuleException(
          "MISSING_DOCUMENTS",
          "Upload the mandatory documents: " + String.join(", ", check.missingDocuments()));
    }
    if (!check.premiumRated()) {
      throw new BusinessRuleException(
          "PREMIUM_NOT_RATED", "Compute the premium (sum insured and rate of every item) first");
    }
    rejectDuplicates(account);
  }

  /**
   * Rejects an account whose risks are already on a live account, naming the existing ARN(s).
   *
   * @param account account (saved or not)
   */
  public void rejectDuplicates(Account account) {
    duplicates.check(subject(account.getCompanyId(), account.getId(), snapshot(account)), false);
  }

  /**
   * The TSU decision for an account, stored on it.
   *
   * @param account account
   * @return decision
   */
  public TsuDecision applyTsu(Account account) {
    RiskProduct product = catalog.requireProduct(account.getProductCode());
    TsuDecision decision = tsu.evaluate(product, tsuFacts(product, snapshot(account)));
    account.setTsuRequirement(decision.required(), decision.ruleCode());
    return decision;
  }

  /**
   * Document types present on an account (own and linked files).
   *
   * @param account account
   * @return document type codes
   */
  public Set<String> documentTypes(Account account) {
    return documents.documentTypesOf(target(account));
  }

  private List<String> missingDocuments(RiskProduct product, Account account) {
    Set<String> present = documentTypes(account);
    return rules.requiredDocuments(product).stream()
        .filter(type -> !present.contains(type))
        .map(type -> lovs.label("DOCUMENT_TYPE", type))
        .toList();
  }

  private AccountData snapshot(Account account) {
    RiskItemKind kind = catalog.requireLine(account.getLineCode()).getRiskItemKind();
    return AccountFields.snapshot(account, kind);
  }

  private static DuplicateSubject subject(Long companyId, Long accountId, AccountData data) {
    return new DuplicateSubject(
        companyId, accountId, data.client().id(), data.product().code(), data.items());
  }

  /**
   * Facts for the TSU routing rules.
   *
   * @param product product
   * @param data account data
   * @return facts
   */
  static TsuFacts tsuFacts(RiskProduct product, AccountData data) {
    int vehicles = (int) data.items().stream().filter(i -> i.vehicle() != null).count();
    long locations =
        data.items().stream()
            .map(RiskItemData::location)
            .filter(Objects::nonNull)
            .map(l -> l.address() + "|" + l.city())
            .collect(Collectors.toSet())
            .size();
    return new TsuFacts(
        product.isPackaged(),
        product.getLineCode(),
        vehicles,
        (int) locations,
        data.items().stream()
            .map(RiskItemData::sumInsured)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add),
        null);
  }

  /**
   * Attachment target of an account.
   *
   * @param account account
   * @return target
   */
  static AttachmentTarget target(Account account) {
    return new AttachmentTarget(AccountService.ENTITY, String.valueOf(account.getId()));
  }
}
