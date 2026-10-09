package com.iortatechnxt.brokerverse.configpromo.engine;

import java.util.regex.Pattern;

/** SQL text of the engine: identifiers come from the database schema and are quoted. */
final class Sql {

  private static final Pattern IDENTIFIER = Pattern.compile("[a-z][a-z0-9_]*");

  private Sql() {}

  /**
   * A quoted identifier.
   *
   * @param identifier table or column name (lower case, digits, underscore)
   * @return quoted identifier
   */
  static String quote(String identifier) {
    if (!IDENTIFIER.matcher(identifier).matches()) {
      throw new IllegalArgumentException("Not a table or column name: " + identifier);
    }
    return '"' + identifier + '"';
  }
}
