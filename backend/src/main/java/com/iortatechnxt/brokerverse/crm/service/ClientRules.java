package com.iortatechnxt.brokerverse.crm.service;

import com.iortatechnxt.brokerverse.common.util.EmailAddresses;
import com.iortatechnxt.brokerverse.crm.domain.ClientDetails;
import com.iortatechnxt.brokerverse.crm.domain.ClientType;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Format and plausibility rules of the client master (BRNB.030/048): TIN {@code 000-000-000-000},
 * e-mail, Philippine mobile {@code 09xxxxxxxxx} or {@code +639xxxxxxxxx}, birth date in the past
 * and minimum age of an individual policyholder. Pure functions, shared by the screens, the API and
 * bulk client upload.
 */
public final class ClientRules {

  /** TIN format. */
  public static final Pattern TIN = Pattern.compile("\\d{3}-\\d{3}-\\d{3}-\\d{3}");

  /** Philippine mobile number. */
  public static final Pattern MOBILE = Pattern.compile("(09\\d{9}|\\+639\\d{9})");

  /** Landline: digits with the usual separators (contact-only changes, BRCSF-004). */
  public static final Pattern PHONE = Pattern.compile("[0-9+() ./-]{3,30}");

  /** Longest address line of the client master. */
  public static final int ADDRESS_MAX = 300;

  /** Longest city or province of the client master. */
  public static final int PLACE_MAX = 80;

  /** Longest postal code of the client master. */
  public static final int POSTAL_CODE_MAX = 10;

  private ClientRules() {}

  /**
   * A broken rule.
   *
   * @param code stable error code
   * @param message readable message
   */
  public record Violation(String code, String message) {}

  /**
   * Checks client data.
   *
   * @param details client data
   * @param today business date
   * @param minimumAge minimum age of an individual (system parameter CLIENT_MIN_AGE)
   * @return violations, empty when valid
   */
  public static List<Violation> violations(ClientDetails details, LocalDate today, int minimumAge) {
    List<Violation> found = new ArrayList<>();
    String tin = details.identity() == null ? null : details.identity().tin();
    if (present(tin) && !TIN.matcher(tin).matches()) {
      found.add(new Violation("CLIENT_TIN_FORMAT", "Enter the TIN as 000-000-000-000"));
    }
    if (details.contact() != null) {
      checkContact(details.contact(), found);
    }
    if (details.birthDate() != null) {
      checkBirthDate(details, today, minimumAge, found);
    }
    return found;
  }

  /**
   * Checks the contact details of a contact-only change (BRCSF-004): the e-mail and mobile rules of
   * the client master, a landline of digits and separators, and the column lengths of the address.
   *
   * @param contact new contact details
   * @return violations, empty when valid
   */
  public static List<Violation> contactViolations(ClientDetails.Contact contact) {
    List<Violation> found = new ArrayList<>();
    checkContact(contact, found);
    if (present(contact.phone()) && !PHONE.matcher(contact.phone().trim()).matches()) {
      found.add(
          new Violation(
              "CLIENT_PHONE_FORMAT", "Enter the phone number with digits and separators"));
    }
    tooLong(contact.addressLine(), ADDRESS_MAX, "address line", found);
    tooLong(contact.city(), PLACE_MAX, "city", found);
    tooLong(contact.province(), PLACE_MAX, "province", found);
    tooLong(contact.postalCode(), POSTAL_CODE_MAX, "postal code", found);
    return found;
  }

  private static void tooLong(String value, int max, String label, List<Violation> found) {
    if (value != null && value.length() > max) {
      found.add(
          new Violation(
              "CLIENT_FIELD_TOO_LONG",
              "Enter the " + label + " in at most " + max + " characters"));
    }
  }

  private static void checkContact(ClientDetails.Contact contact, List<Violation> found) {
    if (present(contact.email()) && !isEmail(contact.email().trim())) {
      found.add(new Violation("CLIENT_EMAIL_FORMAT", "Enter a valid e-mail address"));
    }
    if (present(contact.mobile()) && !MOBILE.matcher(contact.mobile().trim()).matches()) {
      found.add(
          new Violation(
              "CLIENT_MOBILE_FORMAT", "Enter the mobile number as 09xxxxxxxxx or +639xxxxxxxxx"));
    }
  }

  private static void checkBirthDate(
      ClientDetails details, LocalDate today, int minimumAge, List<Violation> found) {
    if (!details.birthDate().isBefore(today)) {
      found.add(new Violation("CLIENT_BIRTH_DATE", "The birth date must be in the past"));
    } else if (details.clientType() == ClientType.INDIVIDUAL
        && Period.between(details.birthDate(), today).getYears() < minimumAge) {
      found.add(
          new Violation(
              "CLIENT_UNDER_AGE",
              "An individual policyholder must be at least " + minimumAge + " years old"));
    }
  }

  /**
   * Whether a text is a plausible e-mail address: one {@code @}, a local part, and a domain with a
   * dot that neither starts nor ends it, without spaces (checked without a backtracking regex).
   *
   * @param text text
   * @return true when plausible
   */
  public static boolean isEmail(String text) {
    return EmailAddresses.isValid(text);
  }

  private static boolean present(String value) {
    return value != null && !value.isBlank();
  }
}
