package com.iortatechnxt.brokerverse.crm.domain;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Normalised duplicate-detection keys of a client (BRNB.032). Two clients match on a key when the
 * normalised values are equal, so "dela cruz" and "DELA CRUZ", or "0917 123 4567" and "+63 917 123
 * 4567", are recognised as the same.
 */
public final class DuplicateKeys {

  private static final Pattern NON_ALNUM = Pattern.compile("[^A-Z0-9 ]");
  private static final Pattern SPACES = Pattern.compile("\\s+");
  private static final Pattern NON_DIGIT = Pattern.compile("\\D");
  private static final Pattern CORPORATE_SUFFIX =
      Pattern.compile(
          "\\b(INCORPORATED|INC|CORPORATION|CORP|COMPANY|CO|LIMITED|LTD|OPC)\\b",
          Pattern.CASE_INSENSITIVE);

  /** Philippine mobile number without the leading zero: 9 and nine more digits. */
  private static final int NATIONAL_DIGITS = 10;

  private static final String COUNTRY_CODE = "63";
  private static final String TRUNK_PREFIX = "0";

  private DuplicateKeys() {}

  /**
   * ID key: type and number, upper-cased, without spaces or dashes.
   *
   * @param idType ID type code
   * @param idNumber ID number
   * @return key, or null when either part is missing
   */
  public static String idKey(String idType, String idNumber) {
    if (isBlank(idType) || isBlank(idNumber)) {
      return null;
    }
    return idType.trim().toUpperCase(Locale.ROOT)
        + ":"
        + idNumber.replaceAll("[\\s-]", "").toUpperCase(Locale.ROOT);
  }

  /**
   * Mobile key in the national format {@code 09xxxxxxxxx}; {@code +639...} and {@code 639...} are
   * converted.
   *
   * @param mobile mobile number as entered
   * @return key, or null when blank
   */
  public static String mobileKey(String mobile) {
    if (isBlank(mobile)) {
      return null;
    }
    String digits = NON_DIGIT.matcher(mobile).replaceAll("");
    if (digits.startsWith(COUNTRY_CODE) && digits.length() == NATIONAL_DIGITS + 2) {
      return TRUNK_PREFIX + digits.substring(2);
    }
    if (digits.length() == NATIONAL_DIGITS && digits.startsWith("9")) {
      return TRUNK_PREFIX + digits;
    }
    return digits;
  }

  /**
   * Person key: last and first name, case- and space-insensitive (compared together with the birth
   * date).
   *
   * @param lastName last name
   * @param firstName first name
   * @return key, or null when a name is missing
   */
  public static String nameKey(String lastName, String firstName) {
    if (isBlank(lastName) || isBlank(firstName)) {
      return null;
    }
    return squash(lastName) + "|" + squash(firstName);
  }

  /**
   * Corporate key: upper case, punctuation and legal-form words (INC, CORP, CO...) removed.
   *
   * @param corporateName corporate name
   * @return key, or null when blank
   */
  public static String corporateKey(String corporateName) {
    if (isBlank(corporateName)) {
      return null;
    }
    String upper = NON_ALNUM.matcher(corporateName.toUpperCase(Locale.ROOT)).replaceAll(" ");
    String stripped = CORPORATE_SUFFIX.matcher(upper).replaceAll(" ");
    String key = SPACES.matcher(stripped).replaceAll("");
    return key.isEmpty() ? null : key;
  }

  /**
   * E-mail key: trimmed, lower case.
   *
   * @param email e-mail
   * @return key, or null when blank
   */
  public static String emailKey(String email) {
    return isBlank(email) ? null : email.trim().toLowerCase(Locale.ROOT);
  }

  private static String squash(String value) {
    return SPACES.matcher(value.toLowerCase(Locale.ROOT)).replaceAll("");
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
