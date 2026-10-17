package com.iortatechnxt.brokerverse.common.storage;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Object key naming: {@code <company>/<entity-type>/<yyyy>/<mm>/<uuid>}. The file name never
 * appears in a key (it lives in the metadata row and in the {@code Content-Disposition} of a link),
 * so keys carry no personal data.
 */
public final class ObjectKeys {

  /** Prefix of objects moved aside after a malware scan found a threat or failed. */
  public static final String QUARANTINE_PREFIX = "quarantine/";

  /** Prefix of large inbound files uploaded by presigned PUT (unscanned until the tag is read). */
  public static final String INCOMING_PREFIX = "incoming/";

  private static final Pattern NOT_KEY_CHARS = Pattern.compile("[^a-z0-9]+");
  private static final Pattern CAMEL = Pattern.compile("([a-z0-9])([A-Z])");
  private static final int MAX_SEGMENT = 40;

  private ObjectKeys() {}

  /**
   * A new key.
   *
   * @param company company code (not personal data)
   * @param entityType owner entity type, e.g. {@code BrokerClaim}
   * @param at time of storage (UTC year and month)
   * @param id unique id
   * @return key, e.g. {@code bdoi/broker-claim/2026/09/5d0c...}
   */
  public static String newKey(String company, String entityType, Instant at, UUID id) {
    ZonedDateTime utc = at.atZone(ZoneOffset.UTC);
    return segment(company)
        + '/'
        + segment(entityType)
        + '/'
        + utc.getYear()
        + '/'
        + String.format(Locale.ROOT, "%02d", utc.getMonthValue())
        + '/'
        + id;
  }

  /**
   * A key segment: lower case letters and digits, other characters as single hyphens.
   *
   * @param text text
   * @return segment, {@code x} when nothing is left
   */
  static String segment(String text) {
    String base = text == null ? "" : CAMEL.matcher(text).replaceAll("$1-$2");
    String clean = NOT_KEY_CHARS.matcher(base.toLowerCase(Locale.ROOT)).replaceAll("-");
    clean = trimHyphens(clean);
    if (clean.isEmpty()) {
      return "x";
    }
    return clean.length() > MAX_SEGMENT ? clean.substring(0, MAX_SEGMENT) : clean;
  }

  private static String trimHyphens(String text) {
    int start = 0;
    int end = text.length();
    while (start < end && text.charAt(start) == '-') {
      start++;
    }
    while (end > start && text.charAt(end - 1) == '-') {
      end--;
    }
    return text.substring(start, end);
  }
}
