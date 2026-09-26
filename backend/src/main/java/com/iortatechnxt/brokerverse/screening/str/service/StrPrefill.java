package com.iortatechnxt.brokerverse.screening.str.service;

import com.iortatechnxt.brokerverse.crm.domain.Client;
import com.iortatechnxt.brokerverse.screening.cases.domain.ScreeningCase;
import com.iortatechnxt.brokerverse.screening.str.domain.StrTransaction.Line;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The prefill sources of STR template fields (SNSRP-705; FR-SS-017 {@code prefill_source}): the
 * client master (name, code, TIN, address, birth date, ID), the case (number, type, risk category,
 * disposition, recommendation) and the transactions (total). Pure function.
 */
public final class StrPrefill {

  private StrPrefill() {}

  /**
   * The values of the prefill sources for a case.
   *
   * @param client the client
   * @param c the case
   * @param transactions the transactions
   * @return values by prefill source
   */
  public static Map<String, String> sources(
      Client client, ScreeningCase c, List<Line> transactions) {
    Map<String, String> values = new TreeMap<>();
    put(values, "CLIENT_NAME", client.getDisplayName());
    put(values, "CLIENT_CODE", client.getCode());
    put(values, "CLIENT_TYPE", client.getClientType().name());
    put(values, "CLIENT_TIN", client.getTin());
    put(values, "CLIENT_ADDRESS", address(client));
    put(values, "CLIENT_BIRTH_DATE", Objects.toString(client.getBirthDate(), null));
    put(values, "CLIENT_ID", join(client.getIdType(), client.getIdNumber()));
    put(values, "CASE_NO", c.getCaseNo());
    put(values, "CASE_TYPE", c.getCaseType());
    put(values, "RISK_CATEGORY", c.getRiskCategory());
    put(values, "CASE_DISPOSITION", c.getDisposition());
    put(values, "CASE_RECOMMENDATION", c.getRecommendation());
    put(values, "TRANSACTION_TOTAL", total(transactions));
    return values;
  }

  /**
   * The subject snapshot kept on the STR (FR-SS-070 R1): the client facts at preparation.
   *
   * @param client the client
   * @return one "label: value" line per fact
   */
  public static String snapshot(Client client) {
    StringJoiner lines = new StringJoiner("\n");
    Stream.of(
            fact("Name", client.getDisplayName()),
            fact("Code", client.getCode()),
            fact("Type", client.getClientType().name()),
            fact("TIN", client.getTin()),
            fact("ID", join(client.getIdType(), client.getIdNumber())),
            fact("Birth date", Objects.toString(client.getBirthDate(), null)),
            fact("Address", address(client)),
            fact("E-mail", client.getEmail()),
            fact("Mobile", client.getMobile()))
        .filter(Objects::nonNull)
        .forEach(lines::add);
    return lines.toString();
  }

  private static String fact(String label, String value) {
    return value == null || value.isBlank() ? null : label + ": " + value;
  }

  private static String address(Client client) {
    return Stream.of(
            client.getAddressLine(), client.getCity(), client.getProvince(), client.getPostalCode())
        .filter(s -> s != null && !s.isBlank())
        .collect(Collectors.joining(", "));
  }

  private static String join(String a, String b) {
    return Stream.of(a, b).filter(s -> s != null && !s.isBlank()).collect(Collectors.joining(" "));
  }

  private static String total(List<Line> transactions) {
    return transactions.isEmpty()
        ? null
        : transactions.stream()
            .map(Line::amount)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .toPlainString();
  }

  private static void put(Map<String, String> values, String key, String value) {
    if (value != null && !value.isBlank()) {
      values.put(key, value);
    }
  }
}
