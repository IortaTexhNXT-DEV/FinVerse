package com.iortatechnxt.brokerverse.renewal.channel.service;

import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * The password of a protected file (FRRN.024.01), by {@value #PARAMETER}: BDOI's convention - for
 * an individual, the first three letters of the first name and of the last name, without spaces or
 * special characters, in capitals (fewer when a name is shorter, never padded); for a corporate
 * client, eight random characters with one lower-case letter, one capital, one special character
 * and five digits - or GENERATED, a random password chosen by the messaging service.
 */
@Component
public class PasswordConvention {

  /** Parameter: CLIENT or GENERATED. */
  public static final String PARAMETER = "RNW_PASSWORD_CONVENTION";

  /** Hint of the individual password, for the e-mail. */
  public static final String INDIVIDUAL_HINT =
      "The password is the first three letters of your first name followed by the first three"
          + " letters of your last name, in capital letters.";

  private static final int PART = 3;
  private static final int DIGITS = 5;
  private static final String LOWER = "abcdefghijkmnopqrstuvwxyz";
  private static final String UPPER = "ABCDEFGHJKLMNPQRSTUVWXYZ";
  private static final String SPECIAL = "!@#$%&*?";
  private static final String NUMBERS = "0123456789";

  private final SystemParameterService parameters;
  private final NamedParameterJdbcTemplate jdbc;
  private final SecureRandom random = new SecureRandom();

  /**
   * Creates the convention.
   *
   * @param parameters setting
   * @param jdbc client names and type
   */
  public PasswordConvention(SystemParameterService parameters, NamedParameterJdbcTemplate jdbc) {
    this.parameters = parameters;
    this.jdbc = jdbc;
  }

  /**
   * The password of a file for a client.
   *
   * @param clientId client, may be null
   * @return password and hint, empty when the messaging service chooses one
   */
  public Optional<Password> of(Long clientId) {
    if (!"CLIENT".equals(parameters.text(PARAMETER, "CLIENT").strip()) || clientId == null) {
      return Optional.empty();
    }
    List<Map<String, Object>> rows =
        jdbc.queryForList(
            "select client_type, first_name, last_name from crm_client where id = :id",
            Map.of("id", clientId));
    if (rows.isEmpty()) {
      return Optional.empty();
    }
    Map<String, Object> r = rows.get(0);
    if ("INDIVIDUAL".equals(r.get("client_type"))) {
      return Optional.of(
          new Password(
              individual((String) r.get("first_name"), (String) r.get("last_name")),
              INDIVIDUAL_HINT,
              true));
    }
    return Optional.of(new Password(corporate(), null, false));
  }

  /**
   * BDOI's password of an individual.
   *
   * @param firstName first name
   * @param lastName last name
   * @return password
   */
  public static String individual(String firstName, String lastName) {
    return first(clean(firstName)) + first(clean(lastName));
  }

  /**
   * A corporate password: one lower-case letter, one capital, one special character and five
   * digits, in a random order.
   *
   * @return password of eight characters
   */
  public String corporate() {
    List<Character> chars = new ArrayList<>();
    chars.add(pick(LOWER));
    chars.add(pick(UPPER));
    chars.add(pick(SPECIAL));
    for (int i = 0; i < DIGITS; i++) {
      chars.add(pick(NUMBERS));
    }
    Collections.shuffle(chars, random);
    StringBuilder b = new StringBuilder();
    chars.forEach(b::append);
    return b.toString();
  }

  private char pick(String from) {
    return from.charAt(random.nextInt(from.length()));
  }

  private static String clean(String name) {
    if (name == null) {
      return "";
    }
    StringBuilder b = new StringBuilder();
    name.codePoints()
        .filter(Character::isLetterOrDigit)
        .map(Character::toUpperCase)
        .forEach(b::appendCodePoint);
    return b.toString();
  }

  private static String first(String text) {
    return text.length() <= PART ? text : text.substring(0, PART);
  }

  /**
   * A password of a file.
   *
   * @param value password
   * @param hint how the client finds it, may be null
   * @param derivable true when the client can derive it (no separate password message needed)
   */
  public record Password(String value, String hint, boolean derivable) {

    @Override
    public String toString() {
      return "Password[derivable=" + derivable + "]";
    }
  }
}
