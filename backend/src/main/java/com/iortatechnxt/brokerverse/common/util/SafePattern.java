package com.iortatechnxt.brokerverse.common.util;

import com.google.re2j.Pattern;
import com.google.re2j.PatternSyntaxException;

/**
 * Regular expressions entered by administrators (field-rule patterns, risk-code patterns). They are
 * evaluated with RE2J, whose matching time is linear in the input, so a hostile or careless pattern
 * such as {@code (a+)+$} cannot stall a request (ReDoS). RE2 syntax is the common regex subset:
 * everything except back-references and look-around.
 */
public final class SafePattern {

  private SafePattern() {}

  /**
   * Whether a pattern is non-blank and valid RE2 syntax.
   *
   * @param regex the pattern
   * @return true when it can be used
   */
  public static boolean isValid(String regex) {
    if (regex == null || regex.isBlank()) {
      return false;
    }
    try {
      Pattern.compile(regex);
      return true;
    } catch (PatternSyntaxException e) {
      return false;
    }
  }

  /**
   * Whether the whole value matches the pattern.
   *
   * @param regex a pattern accepted by {@link #isValid(String)}
   * @param value the value to check
   * @return true on a full match
   */
  public static boolean matches(String regex, String value) {
    return Pattern.compile(regex).matcher(value).matches();
  }
}
