package com.iortatechnxt.brokerverse.crm.service;

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
    int at = text.indexOf('@');
    if (at <= 0 || at != text.lastIndexOf('@') || text.chars().anyMatch(Character::isWhitespace)) {
      return false;
    }
    String domain = text.substring(at + 1);
    int dot = domain.lastIndexOf('.');
    return dot > 0 && dot < domain.length() - 2 && !domain.startsWith(".");
  }

  private static boolean present(String value) {
    return value != null && !value.isBlank();
  }
}
