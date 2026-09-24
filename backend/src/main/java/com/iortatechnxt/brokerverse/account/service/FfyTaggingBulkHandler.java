package com.iortatechnxt.brokerverse.account.service;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.account.service.AccountBulkSupport.Headers;
import com.iortatechnxt.brokerverse.bulk.service.BulkColumn;
import com.iortatechnxt.brokerverse.bulk.service.BulkContext;
import com.iortatechnxt.brokerverse.bulk.service.BulkImportHandler;
import com.iortatechnxt.brokerverse.bulk.service.BulkRow;
import com.iortatechnxt.brokerverse.catalog.service.ProductCatalogService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Free First Year tagging by upload (BRNB.113): a serial (chassis), motor (engine), plate or
 * conduction sticker number per row identifies exactly one live account, which is tagged FFY from
 * the start date (the end date is computed) or has its tag cancelled.
 */
@Component
public class FfyTaggingBulkHandler implements BulkImportHandler {

  /** Handler code. */
  public static final String CODE = "FFY_TAGGING";

  private static final String IDENTIFIER = "Vehicle Identifier";
  private static final String ACTION = "Action";
  private static final String REASON = "Cancel Reason";
  private static final String COMMENT = "Comment";
  private static final String TAG = "TAG";
  private static final String CANCEL = "CANCEL";
  private static final Map<String, String> ACTIONS =
      Map.of("tag", TAG, "Tag", TAG, "cancel", CANCEL, "Cancel", CANCEL);

  private final AccountTaggingService tagging;
  private final ProductCatalogService catalog;
  private final LovService lovs;
  private final AccountBulkSupport support;
  private final Clock clock;

  /**
   * Creates the handler.
   *
   * @param tagging FFY tagging
   * @param catalog products
   * @param lovs lists of values
   * @param support shared bulk helpers
   * @param clock clock
   */
  public FfyTaggingBulkHandler(
      AccountTaggingService tagging,
      ProductCatalogService catalog,
      LovService lovs,
      AccountBulkSupport support,
      Clock clock) {
    this.tagging = tagging;
    this.catalog = catalog;
    this.lovs = lovs;
    this.support = support;
    this.clock = clock;
  }

  @Override
  public String code() {
    return CODE;
  }

  @Override
  public String title() {
    return "Free First Year tagging";
  }

  @Override
  public String permission() {
    return "ACCOUNT_MAINTAIN";
  }

  @Override
  public String instructions() {
    return "One row per vehicle: its serial (chassis), motor (engine), plate or conduction sticker"
        + " number must match exactly one live account of an FFY-eligible product. Action TAG"
        + " (default) tags the account from FFY Start (the end is one year later); CANCEL cancels"
        + " its tag with a reason.";
  }

  @Override
  public List<BulkColumn> columns() {
    return List.of(
        BulkColumn.required(
            IDENTIFIER, "Chassis, engine, plate or conduction sticker", "MHF12345678"),
        AccountBulkSupport.date(Headers.FFY_START, "FFY start (TAG)", false),
        BulkColumn.optional(ACTION, "TAG or CANCEL (default TAG)", TAG),
        BulkColumn.optional(REASON, "Cancellation reason (list FFY_CANCEL_REASON)", ""),
        BulkColumn.optional(COMMENT, "Comment", ""));
  }

  @Override
  public String sanitize(String header, String value) {
    String clean = BulkImportHandler.super.sanitize(header, value);
    if (IDENTIFIER.equals(header)) {
      return BulkImportHandler.identifier(clean);
    }
    return ACTION.equals(header) ? ACTIONS.getOrDefault(clean, clean) : clean;
  }

  @Override
  public String duplicateKey(BulkRow row) {
    return row.text(IDENTIFIER);
  }

  @Override
  public List<String> validate(BulkRow row, BulkContext context) {
    return support.safely(
        () -> {
          Account account = account(row, context);
          if (cancel(row)) {
            lovs.requireValid("FFY_CANCEL_REASON", row.text(REASON), LocalDate.now(clock));
            return account.getFreeFirstYear().active()
                ? List.of()
                : List.of("Account " + account.getArn() + " is not tagged Free First Year");
          }
          if (row.date(Headers.FFY_START) == null) {
            return List.of("Enter the FFY Start date");
          }
          return catalog.requireProduct(account.getProductCode()).isFfyEligible()
              ? List.of()
              : List.of("Product " + account.getProductCode() + " is not Free First Year eligible");
        });
  }

  @Override
  public String commit(BulkRow row, BulkContext context) {
    Account account = account(row, context);
    if (cancel(row)) {
      tagging.cancelFreeFirstYear(account.getId(), row.text(REASON), row.text(COMMENT));
    } else {
      tagging.tagFreeFirstYear(account.getId(), row.date(Headers.FFY_START));
    }
    return account.getArn();
  }

  private Account account(BulkRow row, BulkContext context) {
    String action = row.text(ACTION);
    if (action != null && !TAG.equals(action) && !CANCEL.equals(action)) {
      throw new BusinessRuleException("FFY_ACTION_INVALID", "Action must be TAG or CANCEL");
    }
    List<Account> matches = tagging.byVehicle(context.companyId(), row.text(IDENTIFIER));
    if (matches.size() != 1) {
      throw new BusinessRuleException(
          "FFY_VEHICLE_NOT_UNIQUE",
          matches.isEmpty()
              ? "No live account insures vehicle " + row.text(IDENTIFIER)
              : "Several accounts insure vehicle "
                  + row.text(IDENTIFIER)
                  + ": "
                  + matches.stream().map(Account::getArn).collect(Collectors.joining(", ")));
    }
    return matches.get(0);
  }

  private static boolean cancel(BulkRow row) {
    return CANCEL.equals(row.text(ACTION));
  }
}
