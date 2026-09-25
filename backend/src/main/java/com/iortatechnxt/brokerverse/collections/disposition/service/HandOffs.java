package com.iortatechnxt.brokerverse.collections.disposition.service;

import com.iortatechnxt.brokerverse.collections.common.domain.ClxEnums.OpsAction;
import com.iortatechnxt.brokerverse.collections.common.domain.ClxEnums.TaggingOwner;
import com.iortatechnxt.brokerverse.collections.common.domain.CollectionItem;
import com.iortatechnxt.brokerverse.collections.feed.domain.OutboxItem.FeedKey;
import com.iortatechnxt.brokerverse.collections.feed.service.OutboxService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * The Operations hand-off of a PR collector disposition (COLLECTIONS_DESIGN 2.2): validation of the
 * details the hand-off needs and the outbox item in the layout its consumer reads.
 *
 * <ul>
 *   <li>CHECK_PICKUP - {@code COLLECTION_CHECK_PICKUP} for the Cashiering pick-up queue
 *       (CSHID.009), with the pick-up date, address, contact, check and amount.
 *   <li>CWT2307_REVERSAL - {@code COLLECTION_CWT2307} for Cashiering's 2307 tags (CSHID.026,
 *       MKTID.013) in the {@code CWT_TAGS} columns, once the tag is an Operations action
 *       (certificate received) or at once for the cash path (p.41).
 *   <li>DP_REVERSAL - {@code COLLECTION_DP_LIST} for Commission's DP list (CMRID.001, MKTID.012) in
 *       the DP list columns.
 *   <li>CANCEL_REQUEST and NONE - no feed; a cancellation is raised in Adjustment.
 * </ul>
 */
final class HandOffs {

  static final String PICKUP_DATE = "pickupDate";
  static final String PICKUP_ADDRESS = "pickupAddress";
  static final String CONTACT_PERSON = "contactPerson";
  static final String CHECK_NO = "checkNo";
  static final String CHECK_BANK = "checkBank";
  static final String AMOUNT = "amount";
  static final String PATH = "path";
  static final String CERTIFICATE_NO = "certificateNo";
  static final String PERIOD_FROM = "periodFrom";
  static final String PERIOD_TO = "periodTo";

  private static final String CERTIFICATE = "CERTIFICATE";
  private static final String CASH = "CASH";
  private static final Set<String> PATHS = Set.of(CASH, CERTIFICATE);
  private static final String REMARKS = "Remarks";

  private HandOffs() {}

  /**
   * Validates the details a hand-off needs.
   *
   * @param action Operations action
   * @param details details entered
   * @param today business date
   */
  static void validate(OpsAction action, Map<String, String> details, LocalDate today) {
    if (action == OpsAction.CHECK_PICKUP) {
      validatePickup(details, today);
    } else if (action == OpsAction.CWT2307_REVERSAL) {
      validateCwt(details);
    }
  }

  private static void validatePickup(Map<String, String> details, LocalDate today) {
    LocalDate pickup = date(details, PICKUP_DATE);
    if (pickup == null || pickup.isBefore(today)) {
      throw new BusinessRuleException(
          "CLX_PICKUP_DATE", "Give a pick-up date from today on for the check pick-up");
    }
    require(details, PICKUP_ADDRESS, "the pick-up address");
    BigDecimal amount = amount(details);
    if (amount == null || amount.signum() <= 0) {
      throw new BusinessRuleException("CLX_PICKUP_AMOUNT", "Give the check amount");
    }
  }

  private static void validateCwt(Map<String, String> details) {
    String path = details.getOrDefault(PATH, CERTIFICATE);
    if (!PATHS.contains(path)) {
      throw new BusinessRuleException("CLX_CWT_PATH", "The 2307 path is CASH or CERTIFICATE");
    }
    if (CERTIFICATE.equals(path)) {
      require(details, CERTIFICATE_NO, "the BIR 2307 certificate number");
    }
    date(details, PERIOD_FROM);
    date(details, PERIOD_TO);
    amount(details);
  }

  /**
   * The outbox item of a disposition, when its action hands work to Operations.
   *
   * @param item collection item
   * @param action Operations action
   * @param owner tagging owner after the disposition
   * @param context disposition id, remarks, details, requestor and branch code
   * @return feed key and fields; empty when nothing is handed over
   */
  static Optional<Queued> of(
      CollectionItem item, OpsAction action, TaggingOwner owner, Context context) {
    String inv = item.getInvoiceNo();
    return switch (action) {
      case CHECK_PICKUP ->
          Optional.of(
              new Queued(
                  new FeedKey(OutboxService.CHECK_PICKUP, "PU:" + inv + ":" + context.id()),
                  pickup(item, context)));
      case CWT2307_REVERSAL -> {
        boolean cash = CASH.equals(context.details().get(PATH));
        yield owner == TaggingOwner.OPERATIONS || cash
            ? Optional.of(
                new Queued(
                    new FeedKey(OutboxService.CWT2307, "CWT:" + inv + ":" + context.id()),
                    cwt(item, context)))
            : Optional.empty();
      }
      case DP_REVERSAL ->
          Optional.of(
              new Queued(
                  new FeedKey(OutboxService.DP_LIST, "DP:" + inv + ":" + context.id()),
                  dp(item, context)));
      default -> Optional.empty();
    };
  }

  private static Map<String, String> pickup(CollectionItem item, Context c) {
    Map<String, String> d = c.details();
    Map<String, String> f = new LinkedHashMap<>();
    f.put("reference", item.getInvoiceNo());
    f.put("clientCode", item.getParties().clientCode());
    f.put("payorName", item.getParties().assuredName());
    f.put("assuredName", item.getParties().assuredName());
    f.put(PICKUP_DATE, d.get(PICKUP_DATE));
    f.put(AMOUNT, amount(d).toPlainString());
    f.put("currency", item.getClassification().currency());
    f.put(CHECK_NO, d.getOrDefault(CHECK_NO, ""));
    f.put(CHECK_BANK, d.getOrDefault(CHECK_BANK, ""));
    f.put("requestor", c.requestor());
    f.put("address", d.get(PICKUP_ADDRESS));
    f.put(CONTACT_PERSON, d.getOrDefault(CONTACT_PERSON, ""));
    f.put("remarks", blank(c.remarks()));
    return f;
  }

  private static Map<String, String> cwt(CollectionItem item, Context c) {
    Map<String, String> d = c.details();
    BigDecimal amount = amount(d);
    Map<String, String> f = new LinkedHashMap<>();
    f.put("Invoice no", item.getInvoiceNo());
    f.put("Amount", amount == null ? "" : amount.toPlainString());
    f.put("Path", d.getOrDefault(PATH, CERTIFICATE));
    f.put("Certificate no", d.getOrDefault(CERTIFICATE_NO, ""));
    f.put("Period from", d.getOrDefault(PERIOD_FROM, ""));
    f.put("Period to", d.getOrDefault(PERIOD_TO, ""));
    f.put(REMARKS, blank(c.remarks()));
    return f;
  }

  private static Map<String, String> dp(CollectionItem item, Context c) {
    Map<String, String> f = new LinkedHashMap<>();
    f.put("Invoice No.", item.getInvoiceNo());
    f.put("Policy No.", blank(item.getParties().policyNo()));
    f.put("Insurer", item.getParties().insurerCode());
    f.put("Premium", item.getFigures().grossPremium().toPlainString());
    f.put(REMARKS, blank(c.remarks()));
    f.put("Branch", c.branchCode());
    return f;
  }

  private static void require(Map<String, String> details, String key, String what) {
    String v = details.get(key);
    if (v == null || v.isBlank()) {
      throw new BusinessRuleException("CLX_DISPOSITION_DETAIL", "Give " + what);
    }
  }

  private static LocalDate date(Map<String, String> details, String key) {
    String v = details.get(key);
    if (v == null || v.isBlank()) {
      return null;
    }
    try {
      return LocalDate.parse(v.strip());
    } catch (DateTimeParseException ex) {
      throw new BusinessRuleException(
          "CLX_DISPOSITION_DETAIL", "'" + v + "' is not a date (yyyy-mm-dd)", ex);
    }
  }

  private static BigDecimal amount(Map<String, String> details) {
    String v = details.get(AMOUNT);
    if (v == null || v.isBlank()) {
      return null;
    }
    try {
      return new BigDecimal(v.strip().replace(",", ""));
    } catch (NumberFormatException ex) {
      throw new BusinessRuleException("CLX_DISPOSITION_DETAIL", "'" + v + "' is not an amount", ex);
    }
  }

  private static String blank(String v) {
    return v == null ? "" : v;
  }

  /**
   * What the outbox item is built from.
   *
   * @param id disposition id (part of the idempotency key)
   * @param remarks disposition remarks
   * @param details hand-off details
   * @param requestor user who recorded it
   * @param branchCode invoicing branch code (HO for the head office)
   */
  record Context(
      Long id, String remarks, Map<String, String> details, String requestor, String branchCode) {}

  /**
   * An outbox item to queue.
   *
   * @param key feed and idempotency key
   * @param fields fields in the consumer's layout
   */
  record Queued(FeedKey key, Map<String, String> fields) {}
}
