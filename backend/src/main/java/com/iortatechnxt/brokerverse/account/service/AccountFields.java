package com.iortatechnxt.brokerverse.account.service;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.account.domain.AccountData;
import com.iortatechnxt.brokerverse.account.domain.AccountData.ClientRef;
import com.iortatechnxt.brokerverse.account.domain.AccountData.Mortgage;
import com.iortatechnxt.brokerverse.account.domain.AccountData.ProductRef;
import com.iortatechnxt.brokerverse.account.domain.RiskItem;
import com.iortatechnxt.brokerverse.account.domain.RiskItemData;
import com.iortatechnxt.brokerverse.account.domain.RiskItemData.Location;
import com.iortatechnxt.brokerverse.account.domain.RiskItemData.Person;
import com.iortatechnxt.brokerverse.account.domain.RiskItemData.Vehicle;
import com.iortatechnxt.brokerverse.catalog.domain.RiskItemKind;
import com.iortatechnxt.brokerverse.catalog.service.FieldPresence;
import com.iortatechnxt.brokerverse.catalog.service.FieldValues;
import com.iortatechnxt.brokerverse.catalog.service.PeriodBasis;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Field keys of the minimum-field matrix (BRNB.002/003/093) as they apply to accounts, and the
 * conversion of a saved account back to account data.
 *
 * <p>Account keys: clientId, productCode, marketSegment, sourceChannel, periodFrom, periodTo,
 * currency, insurerCode, insurerBranch, mortgageeBank, loanApplicationNo, pnNumbers, contactEmail,
 * contactMobile, items. Item keys: description, sumInsured, rate, plateNo, conductionSticker,
 * engineNo, chassisNo, make, model, yearModel, bodyType, colour, seatingCapacity, address, city,
 * province, occupancy, constructionClass, insuredItems, personName, birthDate.
 */
final class AccountFields {

  private static final String SUM_INSURED = "sumInsured";

  private AccountFields() {}

  /**
   * Which fields hold a value.
   *
   * @param d account data
   * @return presence for the minimum-field check
   */
  static FieldPresence presence(AccountData d) {
    FieldValues values = values(d);
    return new FieldPresence(
        values.record().keySet(), values.items().stream().map(AccountFields::present).toList());
  }

  /**
   * The given values as text, for the typed field rules (BRPM.004).
   *
   * @param d account data
   * @return values of the account and of each item
   */
  static FieldValues values(AccountData d) {
    Map<String, String> keys = new HashMap<>();
    put(keys, "clientId", d.client() == null ? null : d.client().id());
    put(keys, "productCode", d.product() == null ? null : d.product().code());
    put(keys, "marketSegment", d.marketSegment());
    put(keys, "sourceChannel", d.sourceChannel());
    put(keys, "periodFrom", d.periodFrom());
    put(keys, "periodTo", d.periodTo());
    put(keys, "currency", d.currency());
    put(keys, "insurerCode", d.insurerCode());
    put(keys, "insurerBranch", d.insurerBranch());
    Mortgage m = d.mortgage() == null ? Mortgage.NONE : d.mortgage();
    put(keys, "mortgageeBank", m.bank());
    put(keys, "loanApplicationNo", m.loanApplicationNo());
    put(keys, "pnNumbers", m.pnNumbers().isEmpty() ? null : String.join(",", m.pnNumbers()));
    if (d.contact() != null) {
      put(keys, "contactEmail", d.contact().email());
      put(keys, "contactMobile", d.contact().mobile());
    }
    put(keys, "items", d.items().isEmpty() ? null : d.items().size());
    return new FieldValues(keys, d.items().stream().map(AccountFields::itemValues).toList());
  }

  /** Presence of an item: a sum insured counts only when positive. */
  private static Set<String> present(Map<String, String> item) {
    Set<String> keys = new HashSet<>(item.keySet());
    String sum = item.get(SUM_INSURED);
    if (sum != null && new BigDecimal(sum).signum() <= 0) {
      keys.remove(SUM_INSURED);
    }
    return keys;
  }

  private static Map<String, String> itemValues(RiskItemData item) {
    Map<String, String> keys = new HashMap<>();
    put(keys, "description", item.description());
    put(keys, SUM_INSURED, item.sumInsured());
    put(keys, "rate", item.rate());
    vehicleValues(keys, item.vehicle());
    locationValues(keys, item.location());
    Person p = item.person();
    if (p != null) {
      put(keys, "personName", p.name());
      put(keys, "birthDate", p.birthDate());
    }
    return keys;
  }

  private static void vehicleValues(Map<String, String> keys, Vehicle v) {
    if (v == null) {
      return;
    }
    put(keys, "plateNo", v.plateNo());
    put(keys, "conductionSticker", v.conductionSticker());
    put(keys, "engineNo", v.engineNo());
    put(keys, "chassisNo", v.chassisNo());
    put(keys, "make", v.make());
    put(keys, "model", v.model());
    put(keys, "yearModel", v.yearModel());
    put(keys, "bodyType", v.bodyType());
    put(keys, "colour", v.colour());
    put(keys, "seatingCapacity", v.seatingCapacity());
  }

  private static void locationValues(Map<String, String> keys, Location l) {
    if (l == null) {
      return;
    }
    put(keys, "address", l.address());
    put(keys, "city", l.city());
    put(keys, "province", l.province());
    put(keys, "occupancy", l.occupancy());
    put(keys, "constructionClass", l.constructionClass());
    put(keys, "insuredItems", l.insuredItems().isEmpty() ? null : l.insuredItems().size());
  }

  private static void put(Map<String, String> keys, String key, Object value) {
    if (value == null || value instanceof String s && s.isBlank()) {
      return;
    }
    keys.put(key, value instanceof BigDecimal b ? b.toPlainString() : String.valueOf(value));
  }

  /**
   * The data of a saved account (minimum-field check, bulk updates).
   *
   * @param a account
   * @param kind item kind of the product line
   * @return account data
   */
  static AccountData snapshot(Account a, RiskItemKind kind) {
    return new AccountData(
        new ClientRef(a.getClientId(), a.getClientCode(), a.getClientName()),
        new ProductRef(a.getProductCode(), a.getLineCode(), a.getCoverTypeCode(), kind),
        a.getMarketSegment(),
        a.getSourceChannel(),
        a.getInsurerCode(),
        a.getInsurerBranch(),
        a.getPeriodFrom(),
        a.getPeriodTo(),
        a.isMultiYear(),
        a.getTermYears(),
        a.getCurrency(),
        a.getPaymentArrangement(),
        new Mortgage(a.getMortgageeBank(), a.getLoanApplicationNo(), a.getPnNumbers()),
        a.getContact(),
        items(a.getItems()));
  }

  /**
   * The items of a saved account as item data.
   *
   * @param items items
   * @return item data
   */
  static List<RiskItemData> items(List<RiskItem> items) {
    return items.stream()
        .map(
            i ->
                new RiskItemData(
                    i.getDescription(),
                    i.getSumInsured(),
                    i.getRate(),
                    i.getBiLimit(),
                    i.getPdLimit(),
                    i.getKind() == RiskItemKind.VEHICLE ? i.vehicle() : null,
                    i.getKind() == RiskItemKind.PROPERTY_LOCATION ? i.location() : null,
                    i.getKind() == RiskItemKind.PERSON ? i.person() : null))
        .toList();
  }

  /**
   * A draft carrying the data of a saved account (bulk updates change a few fields of it).
   *
   * @param a account
   * @param kind item kind
   * @return draft
   */
  static AccountDraft draftOf(Account a, RiskItemKind kind) {
    AccountData d = snapshot(a, kind);
    String basis = a.getPremium().ratingBasis();
    return new AccountDraft(
        d.client().id(),
        d.product().code(),
        d.marketSegment(),
        d.sourceChannel(),
        d.insurerCode(),
        d.insurerBranch(),
        d.periodFrom(),
        d.periodTo(),
        d.multiYear(),
        d.termYears(),
        d.currency(),
        d.paymentArrangement(),
        d.mortgage(),
        d.contact(),
        d.items(),
        basis == null ? null : PeriodBasis.valueOf(basis),
        null,
        a.getFreeFirstYear().active() ? a.getFreeFirstYear().start() : null);
  }
}
