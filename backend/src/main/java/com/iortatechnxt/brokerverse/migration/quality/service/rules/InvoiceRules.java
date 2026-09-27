package com.iortatechnxt.brokerverse.migration.quality.service.rules;

import com.iortatechnxt.brokerverse.migration.common.service.Values;
import com.iortatechnxt.brokerverse.migration.intake.domain.StageRow;
import com.iortatechnxt.brokerverse.migration.quality.service.FindingSink;
import com.iortatechnxt.brokerverse.migration.quality.service.ObjectRules;
import com.iortatechnxt.brokerverse.migration.quality.service.ValidationScope;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Open legacy invoice rules (layouts F01, F01S and F01C; DQ-021 to DQ-029): gross premium equal to
 * the booked premium components, consistent component positions, an open balance within the booked
 * amount, at least one open component, shares of 100 percent with one lead, the parent of an
 * endorsement invoice in the file or loaded, realised commission and deferred VAT within bounds, a
 * booking rate for foreign-currency invoices, and the client loaded.
 */
@Component
public class InvoiceRules implements ObjectRules {

  private static final String HEADER = "F01";
  private static final String POSITIONS = "F01C";
  private static final String KEY = "legacy_invoice_no";
  private static final String OPEN = "open_balance";
  private static final String BOOKED = "booked";
  private static final String CLIENT_NO = "legacy_client_no";
  private static final String PARENT = "parent_invoice_no";
  private static final Set<String> PREMIUM =
      Set.of("BASIC", "DST", "PREMIUM_TAX_VAT", "LGT", "FST", "OTHER");

  @Override
  public Set<String> layouts() {
    return Set.of(HEADER, "F01S", POSITIONS);
  }

  @Override
  public void check(ValidationScope scope, FindingSink sink) {
    Map<String, List<StageRow>> positions = new LinkedHashMap<>();
    for (StageRow r : scope.rows(POSITIONS)) {
      positions.computeIfAbsent(r.getRawPayload().get(KEY), k -> new ArrayList<>()).add(r);
      position(r, sink);
    }
    List<StageRow> headers = scope.rows(HEADER);
    Set<String> clients = new HashSet<>();
    Set<String> keys = new HashSet<>();
    Set<String> parents = new HashSet<>();
    for (StageRow r : headers) {
      clients.add(r.getRawPayload().get(CLIENT_NO));
      keys.add(r.getRawPayload().get(KEY));
      parents.add(r.getRawPayload().get(PARENT));
    }
    Set<String> loadedClients = scope.loaded("C01", clients);
    Set<String> loadedParents = scope.loaded(HEADER, parents);
    for (StageRow r : headers) {
      Map<String, String> v = r.getRawPayload();
      totals(r, positions.getOrDefault(v.get(KEY), List.of()), sink);
      header(r, sink);
      if (!loadedClients.contains(v.get(CLIENT_NO))) {
        sink.error(
            r,
            "DQ-016",
            CLIENT_NO,
            v.get(CLIENT_NO),
            "Client " + v.get(CLIENT_NO) + " is not loaded");
      }
      String parent = Values.code(v.get(PARENT));
      if (parent != null && !keys.contains(parent) && !loadedParents.contains(parent)) {
        sink.error(
            r,
            "DQ-026",
            PARENT,
            parent,
            "Original invoice " + parent + " of " + v.get(KEY) + " is not in the file or loaded");
      }
    }
    Shares.check(scope.rows("F01S"), KEY, "DQ-025", sink);
  }

  /** DQ-022 and DQ-023: consistent position, open balance within booked + adjusted. */
  private static void position(StageRow r, FindingSink sink) {
    Map<String, String> v = r.getRawPayload();
    BigDecimal booked = Values.amount(v.get(BOOKED));
    BigDecimal adjusted = Values.amount(v.get("adjusted"));
    BigDecimal open = Values.amount(v.get(OPEN));
    BigDecimal computed =
        booked
            .add(adjusted)
            .subtract(Values.amount(v.get("paid")))
            .subtract(Values.amount(v.get("remitted")))
            .subtract(Values.amount(v.get("written_off")));
    boolean openOnly = booked.signum() == 0 && adjusted.signum() == 0;
    if (!openOnly && computed.compareTo(open) != 0) {
      sink.error(
          r,
          "DQ-022",
          OPEN,
          v.get(OPEN),
          "The open balance of "
              + v.get("component")
              + " is "
              + open.toPlainString()
              + " but booked + adjusted - paid - remitted - written off is "
              + computed.toPlainString());
    }
    BigDecimal ceiling = booked.add(adjusted);
    boolean outside =
        ceiling.signum() >= 0
            ? open.signum() < 0 || open.compareTo(ceiling) > 0
            : open.signum() > 0 || open.compareTo(ceiling) < 0;
    if (!openOnly && outside) {
      sink.error(
          r,
          "DQ-023",
          OPEN,
          v.get(OPEN),
          "The open balance of " + v.get("component") + " is outside 0 and booked + adjusted");
    }
  }

  /** DQ-021 and DQ-024: gross premium equals the booked premium, something is still open. */
  private static void totals(StageRow r, List<StageRow> lines, FindingSink sink) {
    Map<String, String> v = r.getRawPayload();
    BigDecimal premium = BigDecimal.ZERO;
    BigDecimal open = BigDecimal.ZERO;
    BigDecimal bookedAll = BigDecimal.ZERO;
    for (StageRow l : lines) {
      Map<String, String> p = l.getRawPayload();
      BigDecimal booked = Values.amount(p.get(BOOKED));
      bookedAll = bookedAll.add(booked.abs());
      if (PREMIUM.contains(Values.code(p.get("component")))) {
        premium = premium.add(booked);
      }
      open = open.add(Values.amount(p.get(OPEN)).abs());
    }
    BigDecimal gross = Values.amount(v.get("gross_premium"));
    if (bookedAll.signum() != 0 && gross.compareTo(premium) != 0) {
      sink.error(
          r,
          "DQ-021",
          "gross_premium",
          v.get("gross_premium"),
          "The premium components add up to "
              + premium.toPlainString()
              + ", not the gross premium");
    }
    if (open.signum() == 0) {
      sink.warning(r, "DQ-024", KEY, v.get(KEY), "Invoice " + v.get(KEY) + " has no open balance");
    }
  }

  /** DQ-027 and DQ-029: realised commission, deferred VAT and booking rate. */
  private static void header(StageRow r, FindingSink sink) {
    Map<String, String> v = r.getRawPayload();
    BigDecimal commission = Values.amount(v.get("commission"));
    BigDecimal realised = Values.amount(v.get("commission_realised"));
    BigDecimal deferred = Values.amount(v.get("deferred_vat_open"));
    BigDecimal vat = Values.amount(v.get("vat_on_commission"));
    if (realised.abs().compareTo(commission.abs()) > 0 || deferred.abs().compareTo(vat.abs()) > 0) {
      sink.error(
          r,
          "DQ-027",
          "commission_realised",
          v.get("commission_realised"),
          "Realised commission or deferred VAT is above the commission or its VAT");
    }
    String currency = Values.code(v.get("currency"));
    if (currency != null && !"PHP".equals(currency) && Values.blank(v.get("booking_fx_rate"))) {
      sink.error(
          r,
          "DQ-029",
          "booking_fx_rate",
          null,
          "booking_fx_rate is mandatory for currency " + currency);
    }
  }
}
