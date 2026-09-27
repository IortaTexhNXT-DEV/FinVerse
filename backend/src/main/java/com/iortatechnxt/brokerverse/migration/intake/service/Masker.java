package com.iortatechnxt.brokerverse.migration.intake.service;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.migration.intake.service.port.MaskingProvider;
import com.iortatechnxt.brokerverse.migration.mapping.domain.MaskingRule;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Masks the personal data of a staged row outside production (DATA_MIGRATION_DESIGN section 5.3):
 * names and street addresses are replaced by entries of fixed lists chosen by the keyed digest, so
 * the same person keeps the same substitute; TIN, ID, account and phone numbers get keyed digits of
 * the same length with their separators; e-mail becomes {@code <digest>@example.invalid}; birth
 * dates move by a keyed offset of up to 30 days. Amounts, codes and dates of transactions are not
 * masked.
 */
@Component
public class Masker {

  private static final int THIRD = 3;

  private static final List<String> LAST_NAMES =
      List.of(
          "Abad",
          "Bautista",
          "Castillo",
          "Dizon",
          "Estrada",
          "Flores",
          "Garcia",
          "Hernandez",
          "Ilagan",
          "Javier",
          "Katigbak",
          "Lopez",
          "Mendoza",
          "Navarro",
          "Ocampo",
          "Pascual",
          "Quiambao",
          "Ramos",
          "Santos",
          "Torres",
          "Umali",
          "Villanueva",
          "Yap",
          "Zamora");
  private static final List<String> FIRST_NAMES =
      List.of(
          "Adrian", "Bea", "Carlo", "Diana", "Emil", "Faye", "Gino", "Hazel", "Ivan", "Joy", "Karl",
          "Lara", "Migo", "Nina", "Oscar", "Pia", "Rafael", "Sofia", "Tomas", "Ursula", "Vince",
          "Wena", "Xavi", "Ysabel");
  private static final List<String> COMPANY_WORDS =
      List.of(
          "Amber",
          "Bayside",
          "Cedar",
          "Delta",
          "Eastwood",
          "Fairview",
          "Golden",
          "Harbor",
          "Island",
          "Jade",
          "Keystone",
          "Lakeside",
          "Meridian",
          "Northgate",
          "Orchid",
          "Pacific",
          "Quartz",
          "Riverside",
          "Summit",
          "Tanglaw",
          "Unity",
          "Vista",
          "Westfield",
          "Zenith");
  private static final List<String> STREETS =
      List.of(
          "Acacia",
          "Balete",
          "Camia",
          "Dama de Noche",
          "Everlasting",
          "Fortune",
          "Gumamela",
          "Hibiscus",
          "Ilang-Ilang",
          "Jasmine",
          "Kamuning",
          "Lotus",
          "Mabini",
          "Narra",
          "Orchid",
          "Pili",
          "Rosal",
          "Sampaguita",
          "Talisay",
          "Waling-Waling");
  private static final int SHIFT_RANGE = 61;
  private static final int SHIFT_CENTRE = 30;
  private static final int HOUSE_NUMBERS = 999;
  private static final int EMAIL_BYTES = 6;
  private static final int TOKEN_BYTES = 4;
  private static final int BYTE = 0xFF;
  private static final int DIGITS = 10;

  private final MaskingProvider provider;

  /**
   * Creates the masker.
   *
   * @param provider keyed digest
   */
  public Masker(MaskingProvider provider) {
    this.provider = provider;
  }

  /** Refuses the intake outside production when no masking key is configured. */
  public void requireConfigured() {
    if (!provider.configured()) {
      throw new BusinessRuleException(
          "MIG_MASKING_KEY",
          "The masking key of this environment is not configured; extracts cannot be staged");
    }
  }

  /**
   * Masks a row.
   *
   * @param row values by column
   * @param rules active masking rules of the layout
   * @return masked copy
   */
  public Map<String, String> mask(Map<String, String> row, List<MaskingRule> rules) {
    Map<String, String> out = new LinkedHashMap<>(row);
    for (MaskingRule rule : rules) {
      String value = out.get(rule.getColumnName());
      if (value != null && !value.isBlank()) {
        out.put(rule.getColumnName(), apply(rule.getRule(), value));
      }
    }
    return out;
  }

  /**
   * Masks one value.
   *
   * @param kind masking kind
   * @param value value
   * @return masked value
   */
  public String apply(MaskingRule.Kind kind, String value) {
    String norm = value.strip().toUpperCase(Locale.ROOT).replaceAll("\\s+", " ");
    byte[] d = provider.digest(kind.name(), norm);
    return switch (kind) {
      case LAST_NAME -> pick(LAST_NAMES, d, 0);
      case FIRST_NAME -> pick(FIRST_NAMES, d, 0);
      case PERSON_NAME -> pick(LAST_NAMES, d, 0) + ", " + pick(FIRST_NAMES, d, 1);
      case CORPORATE_NAME ->
          pick(COMPANY_WORDS, d, 0) + " " + pick(COMPANY_WORDS, d, 1) + " Corporation";
      case ADDRESS ->
          (1 + unsigned(d, 2) * unsigned(d, THIRD) % HOUSE_NUMBERS)
              + " "
              + pick(STREETS, d, 0)
              + " Street";
      case DIGITS -> digits(value, d);
      case EMAIL -> hex(d, EMAIL_BYTES) + "@example.invalid";
      case BIRTH_DATE -> shift(value, d);
      default -> "Masked " + hex(d, TOKEN_BYTES);
    };
  }

  private static String pick(List<String> list, byte[] d, int index) {
    return list.get(unsigned(d, index) % list.size());
  }

  private static int unsigned(byte[] d, int index) {
    return d[index % d.length] & BYTE;
  }

  private static String hex(byte[] d, int bytes) {
    byte[] part = new byte[bytes];
    System.arraycopy(d, 0, part, 0, bytes);
    return HexFormat.of().formatHex(part);
  }

  private static String digits(String value, byte[] d) {
    StringBuilder out = new StringBuilder(value.length());
    int i = 0;
    for (char c : value.toCharArray()) {
      if (Character.isDigit(c)) {
        out.append((char) ('0' + unsigned(d, i++) % DIGITS));
      } else {
        out.append(c);
      }
    }
    return out.toString();
  }

  private static String shift(String value, byte[] d) {
    try {
      LocalDate date = LocalDate.parse(value.strip());
      return date.plusDays((long) unsigned(d, 0) % SHIFT_RANGE - SHIFT_CENTRE).toString();
    } catch (DateTimeParseException e) {
      return value;
    }
  }
}
