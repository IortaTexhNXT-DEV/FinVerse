package com.iortatechnxt.brokerverse.screening.matching.service;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.codec.language.DoubleMetaphone;

/**
 * The blocking keys of one name (SNSRP-301; design 4.2 {@code scr_name_key}): its normalised
 * tokens, the Double Metaphone codes of each token (primary and alternate) and the sorted token
 * string. Two names are candidates for scoring when they share a phonetic or the exact key. Pure
 * functions.
 *
 * @param tokens normalised tokens ({@link NameNormaliser#tokens(String)})
 * @param phonetic Double Metaphone codes of the tokens
 * @param exact the sorted token string, empty for a blank name
 */
public record NameKeys(List<String> tokens, Set<String> phonetic, String exact) {

  /** Longest phonetic code kept; longer names are distinguished by the scorer. */
  private static final int CODE_LENGTH = 6;

  /** Minimum length of a token kept as a TOKEN key. */
  private static final int MIN_TOKEN = 2;

  /** Longest key value stored ({@code scr_name_key.key_value}). */
  public static final int MAX_KEY = 200;

  /** Defensive copies. */
  public NameKeys {
    tokens = List.copyOf(tokens);
    phonetic = Set.copyOf(phonetic);
  }

  /**
   * The keys of a name.
   *
   * @param name a name, may be {@code null}
   * @return the keys (empty for a blank name)
   */
  public static NameKeys of(String name) {
    List<String> tokens = NameNormaliser.tokens(name);
    Set<String> codes = new LinkedHashSet<>();
    tokens.forEach(t -> codes.addAll(codes(t)));
    return new NameKeys(tokens, codes, NameNormaliser.sorted(tokens));
  }

  /**
   * The phonetic codes of one token.
   *
   * @param token a normalised token
   * @return primary and alternate codes, empty when the token has none (digits)
   */
  public static Set<String> codes(String token) {
    DoubleMetaphone encoder = new DoubleMetaphone();
    encoder.setMaxCodeLen(CODE_LENGTH);
    Set<String> codes = new LinkedHashSet<>();
    String primary = encoder.doubleMetaphone(token);
    String alternate = encoder.doubleMetaphone(token, true);
    if (primary != null && !primary.isEmpty()) {
      codes.add(primary);
    }
    if (alternate != null && !alternate.isEmpty()) {
      codes.add(alternate);
    }
    return codes;
  }

  /**
   * Whether the name has no token.
   *
   * @return true for a blank name
   */
  public boolean isEmpty() {
    return tokens.isEmpty();
  }

  /**
   * The TOKEN keys stored for blocking (tokens of at least two characters).
   *
   * @return token keys
   */
  public Set<String> tokenKeys() {
    Set<String> keys = new LinkedHashSet<>();
    tokens.stream().filter(t -> t.length() >= MIN_TOKEN).map(NameKeys::cap).forEach(keys::add);
    return keys;
  }

  /**
   * The EXACT key stored, capped at {@link #MAX_KEY} characters.
   *
   * @return exact key, empty for a blank name
   */
  public String exactKey() {
    return cap(exact);
  }

  /**
   * Whether this name shares a phonetic or the exact key with another.
   *
   * @param other other keys
   * @return true when the pair is a candidate for scoring
   */
  public boolean blocksWith(NameKeys other) {
    return !isEmpty() && (exact.equals(other.exact) || sharesAny(phonetic, other.phonetic));
  }

  private static boolean sharesAny(Collection<String> a, Collection<String> b) {
    return a.stream().anyMatch(b::contains);
  }

  private static String cap(String value) {
    return value.length() <= MAX_KEY ? value : value.substring(0, MAX_KEY);
  }
}
