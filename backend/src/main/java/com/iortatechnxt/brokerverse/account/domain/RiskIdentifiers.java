package com.iortatechnxt.brokerverse.account.domain;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Normalisation of risk identifiers (BRNB.051 "alphanumeric normalisation"): vehicle identifiers
 * keep only upper-case letters and digits ("abc-1234" and "ABC 1234" are the same plate); a
 * location key keeps lower-case letters, digits and single spaces of the address and city.
 */
public final class RiskIdentifiers {

  private static final Pattern NOT_ALPHANUMERIC = Pattern.compile("[^A-Z0-9]");
  private static final Pattern NOT_WORD = Pattern.compile("[^a-z0-9]+");

  private RiskIdentifiers() {}

  /**
   * A vehicle identifier (plate, conduction sticker, engine or chassis number) in canonical form.
   *
   * @param value raw value, may be null
   * @return upper-case letters and digits, null when nothing is left
   */
  public static String vehicleId(String value) {
    if (value == null) {
      return null;
    }
    String clean = NOT_ALPHANUMERIC.matcher(value.toUpperCase(Locale.ROOT)).replaceAll("");
    return clean.isEmpty() ? null : clean;
  }

  /**
   * Canonical text for comparisons (addresses, insured item descriptions).
   *
   * @param value raw text, may be null
   * @return lower-case words separated by one space, null when nothing is left
   */
  public static String text(String value) {
    if (value == null) {
      return null;
    }
    String clean = NOT_WORD.matcher(value.toLowerCase(Locale.ROOT)).replaceAll(" ").strip();
    return clean.isEmpty() ? null : clean;
  }

  /**
   * Location key of a property risk: address and city in canonical form.
   *
   * @param address address of risk
   * @param city city
   * @return key, null when no address is given
   */
  public static String locationKey(String address, String city) {
    String street = text(address);
    if (street == null) {
      return null;
    }
    String town = text(city);
    return town == null ? street : street + " | " + town;
  }
}
