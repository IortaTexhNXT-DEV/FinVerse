package com.iortatechnxt.brokerverse.migration.matching.service;

import java.text.Normalizer;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * The matching keys of a client (DATA_MIGRATION_DESIGN section 9): K1 TIN digits, K2 ID type and
 * number, K3 last name, first name and birth date, K4 normalised corporate name with the
 * registration number, K5 bank CIF, K6 e-mail and PH mobile (soft keys) and the name used by the
 * trigram similarity K7.
 *
 * @param tin TIN digits (K1)
 * @param id ID type and number (K2)
 * @param person last name, first name and birth date (K3)
 * @param corporate normalised corporate name (K4)
 * @param registration registration number (K4)
 * @param cif bank CIF (K5)
 * @param email e-mail (K6)
 * @param mobile mobile digits (K6)
 * @param name normalised name for the similarity (K7)
 * @param birthDate birth date (K7 block)
 * @param city city (K7 block)
 */
public record MatchKeys(
    String tin,
    String id,
    String person,
    String corporate,
    String registration,
    String cif,
    String email,
    String mobile,
    String name,
    String birthDate,
    String city) {

  private static final int MIN_TIN = 9;
  private static final Pattern MARKS = Pattern.compile("\\p{M}+");
  private static final List<String> SUFFIXES =
      List.of(
          "INCORPORATED",
          "INC",
          "CORPORATION",
          "CORP",
          "COMPANY",
          "CO",
          "LIMITED",
          "LTD",
          "PHILS",
          "PHILIPPINES",
          "OPC",
          "LLC");

  /**
   * The keys of the values of a client row.
   *
   * @param v values by column (layout C01)
   * @return keys
   */
  public static MatchKeys of(Map<String, String> v) {
    String tinDigits = digits(v.get("tin"));
    String idNo = norm(v.get("id_number"));
    String last = norm(v.get("last_name"));
    String first = norm(v.get("first_name"));
    String birth = blankToNull(v.get("birth_date"));
    String corp = corporate(v.get("corporate_name"));
    String mobileDigits = digits(v.get("mobile"));
    return new MatchKeys(
        tinDigits != null && tinDigits.length() >= MIN_TIN ? tinDigits : null,
        idNo == null ? null : norm(v.get("id_type")) + ":" + idNo,
        last == null || first == null || birth == null ? null : last + "|" + first + "|" + birth,
        corp,
        norm(v.get("registration_no")),
        digits(v.get("bank_cif")),
        v.get("email") == null ? null : v.get("email").strip().toLowerCase(Locale.ROOT),
        mobileDigits == null ? null : mobileDigits.replaceFirst("^63", "0"),
        corp != null ? corp : join(last, first),
        birth,
        norm(v.get("city")));
  }

  private static String join(String last, String first) {
    return last == null ? null : last + (first == null ? "" : " " + first);
  }

  /**
   * A normalised corporate name: upper case, accents and punctuation removed, company suffixes
   * dropped.
   *
   * @param name corporate name
   * @return normalised name, null when blank
   */
  public static String corporate(String name) {
    String n = norm(name);
    if (n == null) {
      return null;
    }
    StringBuilder out = new StringBuilder();
    for (String word : n.split(" ")) {
      if (!SUFFIXES.contains(word)) {
        out.append(out.isEmpty() ? "" : " ").append(word);
      }
    }
    return out.isEmpty() ? n : out.toString();
  }

  private static String norm(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    String plain = MARKS.matcher(Normalizer.normalize(value, Normalizer.Form.NFD)).replaceAll("");
    String cleaned =
        plain
            .toUpperCase(Locale.ROOT)
            .replaceAll("[^A-Z0-9 ]", " ")
            .replaceAll("\\s+", " ")
            .strip();
    return cleaned.isEmpty() ? null : cleaned;
  }

  private static String digits(String value) {
    if (value == null) {
      return null;
    }
    String d = value.replaceAll("\\D", "");
    return d.isEmpty() ? null : d;
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.strip();
  }

  /**
   * Trigram similarity of two names (Jaccard index of their three-letter groups).
   *
   * @param a name
   * @param b name
   * @return similarity from 0 to 1
   */
  public static double similarity(String a, String b) {
    if (a == null || b == null) {
      return 0;
    }
    Set<String> ta = trigrams(a);
    Set<String> tb = trigrams(b);
    Set<String> union = new HashSet<>(ta);
    union.addAll(tb);
    ta.retainAll(tb);
    return union.isEmpty() ? 0 : (double) ta.size() / union.size();
  }

  private static Set<String> trigrams(String s) {
    String padded = "  " + s + " ";
    Set<String> out = new HashSet<>();
    for (int i = 0; i + 2 < padded.length(); i++) {
      out.add(padded.substring(i, i + 3));
    }
    return out;
  }
}
