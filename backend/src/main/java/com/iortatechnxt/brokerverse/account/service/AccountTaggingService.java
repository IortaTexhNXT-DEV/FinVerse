package com.iortatechnxt.brokerverse.account.service;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.account.domain.AccountStatus;
import com.iortatechnxt.brokerverse.account.domain.FreeFirstYear;
import com.iortatechnxt.brokerverse.account.domain.PaymentArrangement;
import com.iortatechnxt.brokerverse.account.domain.RiskIdentifiers;
import com.iortatechnxt.brokerverse.account.domain.RiskItem;
import com.iortatechnxt.brokerverse.account.domain.RiskItemRepository;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.catalog.domain.RiskItemKind;
import com.iortatechnxt.brokerverse.catalog.domain.RiskProduct;
import com.iortatechnxt.brokerverse.catalog.service.ProductCatalogService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Account tags maintained outside the draft: Free First Year (BRNB.113: tag, edit dates, cancel
 * without deleting the account, automatic end date, audit) and the payment arrangement (BRNB.114:
 * direct payment to the insurer, for eligible products, with audit).
 */
@Service
@Transactional
public class AccountTaggingService {

  private static final Set<AccountStatus> BEFORE_PAYMENT_GATE =
      EnumSet.of(
          AccountStatus.DRAFT,
          AccountStatus.SUBMITTED,
          AccountStatus.RETURNED_TO_MARKETING,
          AccountStatus.AWAITING_PAYMENT);

  private final AccountService accounts;
  private final RiskItemRepository items;
  private final ProductCatalogService catalog;
  private final LovService lovs;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param accounts accounts
   * @param items risk items (identifier look-up)
   * @param catalog products
   * @param lovs lists of values
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  public AccountTaggingService(
      AccountService accounts,
      RiskItemRepository items,
      ProductCatalogService catalog,
      LovService lovs,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.accounts = accounts;
    this.items = items;
    this.catalog = catalog;
    this.lovs = lovs;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Tags an account Free First Year from a start date, or changes the dates of its tag; the end is
   * one year later (BRNB.113). Only eligible products and live accounts qualify.
   *
   * @param id account
   * @param start FFY start
   * @return the account
   */
  public Account tagFreeFirstYear(Long id, LocalDate start) {
    Account account = accounts.get(id);
    requireLive(account);
    RiskProduct product = catalog.requireProduct(account.getProductCode());
    if (!product.isFfyEligible()) {
      throw new BusinessRuleException(
          "FFY_NOT_ELIGIBLE", "Product " + product.getCode() + " is not Free First Year eligible");
    }
    if (start == null) {
      throw new BusinessRuleException("FFY_START_REQUIRED", "Enter the Free First Year start");
    }
    FreeFirstYear ffy = FreeFirstYear.startingOn(start);
    account.setFreeFirstYear(ffy);
    audit.record(
        AccountService.ENTITY,
        account.getArn(),
        AuditAction.UPDATE,
        "Free First Year " + ffy.start() + " to " + ffy.end());
    return account;
  }

  /**
   * Cancels the Free First Year tag; the account and the tag's dates stay for audit.
   *
   * @param id account
   * @param reasonCode reason (list FFY_CANCEL_REASON)
   * @param comment comment
   * @return the account
   */
  public Account cancelFreeFirstYear(Long id, String reasonCode, String comment) {
    Account account = accounts.get(id);
    if (!account.getFreeFirstYear().active()) {
      throw new BusinessRuleException(
          "FFY_NOT_TAGGED", "Account " + account.getArn() + " is not tagged Free First Year");
    }
    String reason =
        lovs.requireValid("FFY_CANCEL_REASON", reasonCode, LocalDate.now(clock)).getLabel();
    String text = comment == null || comment.isBlank() ? reason : reason + " - " + comment.strip();
    account.setFreeFirstYear(
        account.getFreeFirstYear().cancelled(clock.instant(), currentUser.username(), text));
    audit.record(
        AccountService.ENTITY,
        account.getArn(),
        AuditAction.DEACTIVATE,
        "Free First Year cancelled: " + text);
    return account;
  }

  /**
   * Sets the payment arrangement (BRNB.114) until the payment gate is passed.
   *
   * @param id account
   * @param arrangement via BDOI or direct to insurer
   * @return the account
   */
  public Account setPaymentArrangement(Long id, PaymentArrangement arrangement) {
    Account account = accounts.get(id);
    if (!BEFORE_PAYMENT_GATE.contains(account.getStatus())) {
      throw new BusinessRuleException(
          "PAYMENT_ARRANGEMENT_LOCKED", "The payment gate of " + account.getArn() + " is passed");
    }
    if (arrangement == PaymentArrangement.DIRECT_TO_INSURER
        && !catalog.requireProduct(account.getProductCode()).isDirectPaymentEligible()) {
      throw new BusinessRuleException(
          "DIRECT_PAYMENT_NOT_ELIGIBLE",
          "Product " + account.getProductCode() + " cannot be paid directly to the insurer");
    }
    account.setPaymentArrangement(arrangement, currentUser.username(), clock.instant());
    audit.record(
        AccountService.ENTITY,
        account.getArn(),
        AuditAction.UPDATE,
        "Payment arrangement " + arrangement);
    return account;
  }

  /**
   * Live accounts insuring a vehicle with this plate, conduction sticker, engine or chassis number
   * (FFY tagging upload).
   *
   * @param companyId company
   * @param identifier identifier in any format
   * @return accounts
   */
  @Transactional(readOnly = true)
  public List<Account> byVehicle(Long companyId, String identifier) {
    String id = RiskIdentifiers.vehicleId(identifier);
    if (id == null) {
      return List.of();
    }
    return items
        .findVehicles(
            companyId, -1L, DuplicateCheckService.CLOSED, RiskItemKind.VEHICLE, Set.of(id))
        .stream()
        .map(RiskItem::getAccount)
        .distinct()
        .toList();
  }

  private static void requireLive(Account account) {
    if (DuplicateCheckService.CLOSED.contains(account.getStatus())) {
      throw new BusinessRuleException(
          "ACCOUNT_NOT_ACTIVE", "Account " + account.getArn() + " is " + account.getStatus());
    }
  }
}
