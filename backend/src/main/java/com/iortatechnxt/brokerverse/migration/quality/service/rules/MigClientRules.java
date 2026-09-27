package com.iortatechnxt.brokerverse.migration.quality.service.rules;

import com.iortatechnxt.brokerverse.migration.common.service.Values;
import com.iortatechnxt.brokerverse.migration.intake.domain.StageRow;
import com.iortatechnxt.brokerverse.migration.quality.service.FindingSink;
import com.iortatechnxt.brokerverse.migration.quality.service.ObjectRules;
import com.iortatechnxt.brokerverse.migration.quality.service.ValidationScope;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Client master rules (layouts C01 and C02; workbook rules DQ-006 to DQ-009, DQ-012, DQ-013): TIN
 * of 9 to 12 digits, e-mail and PH mobile formats (loaded blank when invalid), plausible birth
 * date, names by client type, contacts and addresses of a client of the same load, one primary
 * address per client.
 */
@Component
public class MigClientRules implements ObjectRules {

  private static final String BIRTH_DATE = "birth_date";

  private static final int TIN_MIN = 9;
  private static final int TIN_MAX = 12;
  private static final int MAX_AGE = 120;
  private static final String EMAIL = "[^@\\s]+@[^@\\s]+\\.[^@\\s]+";
  private static final String MOBILE = "(09\\d{9}|\\+639\\d{9})";
  private static final String CLIENT_NO = "legacy_client_no";

  @Override
  public Set<String> layouts() {
    return Set.of("C01", "C02");
  }

  @Override
  public void check(ValidationScope scope, FindingSink sink) {
    LocalDate today = scope.today();
    Set<String> clients = new HashSet<>();
    for (StageRow row : scope.rows("C01")) {
      Map<String, String> v = row.getRawPayload();
      clients.add(v.get(CLIENT_NO));
      tin(row, v.get("tin"), sink);
      contact(row, v, sink);
      names(row, v, sink);
      birth(row, v.get(BIRTH_DATE), today, sink);
    }
    Map<String, Integer> primaries = new HashMap<>();
    for (StageRow row : scope.rows("C02")) {
      Map<String, String> v = row.getRawPayload();
      String client = v.get(CLIENT_NO);
      if (!clients.contains(client)) {
        sink.error(row, "DQ-012", CLIENT_NO, client, "Client " + client + " is not loaded");
      }
      if ("ADDRESS".equalsIgnoreCase(v.get("record_type")) && Values.flag(v.get("primary_flag"))) {
        primaries.merge(client, 1, Integer::sum);
        if (primaries.get(client) > 1) {
          sink.warning(
              row,
              "DQ-013",
              "primary_flag",
              "Y",
              "Client "
                  + client
                  + " has "
                  + primaries.get(client)
                  + " primary addresses; the latest is kept");
        }
      }
    }
  }

  private static void tin(StageRow row, String tin, FindingSink sink) {
    if (Values.blank(tin)) {
      return;
    }
    int digits = tin.replaceAll("\\D", "").length();
    if (digits < TIN_MIN || digits > TIN_MAX) {
      sink.error(row, "DQ-006", "tin", tin, "TIN " + tin + " is not valid");
    }
  }

  private static void contact(StageRow row, Map<String, String> v, FindingSink sink) {
    String email = v.get("email");
    if (!Values.blank(email) && !email.strip().matches(EMAIL)) {
      sink.warning(
          row, "DQ-007", "email", email, "email " + email + " is not valid; it is loaded blank");
    }
    String mobile = v.get("mobile");
    if (!Values.blank(mobile) && !mobile.replaceAll("[\\s-]", "").matches(MOBILE)) {
      sink.warning(
          row,
          "DQ-007",
          "mobile",
          mobile,
          "mobile " + mobile + " is not valid; it is loaded blank");
    }
  }

  private static void names(StageRow row, Map<String, String> v, FindingSink sink) {
    String type = Values.code(v.get("client_type"));
    if ("I".equals(type)) {
      requireName(row, v, "last_name", sink);
      requireName(row, v, "first_name", sink);
      if (Values.blank(v.get(BIRTH_DATE))) {
        sink.error(row, "DQ-009", BIRTH_DATE, null, "birth_date is mandatory for client type I");
      }
    } else if ("C".equals(type)) {
      requireName(row, v, "corporate_name", sink);
    }
  }

  private static void requireName(
      StageRow row, Map<String, String> v, String field, FindingSink sink) {
    if (Values.blank(v.get(field))) {
      sink.error(
          row,
          "DQ-009",
          field,
          null,
          field + " is mandatory for client type " + Values.code(v.get("client_type")));
    }
  }

  private static void birth(StageRow row, String value, LocalDate today, FindingSink sink) {
    Values.date(value)
        .filter(d -> d.isAfter(today) || d.isBefore(today.minusYears(MAX_AGE)))
        .ifPresent(
            d ->
                sink.error(
                    row, "DQ-008", BIRTH_DATE, value, "Birth date " + value + " is not plausible"));
  }
}
