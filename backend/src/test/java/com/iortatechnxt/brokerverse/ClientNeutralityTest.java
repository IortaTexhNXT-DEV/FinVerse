package com.iortatechnxt.brokerverse;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Standing check that the platform code stays client-neutral: iNXT BrokerVerse is a product for
 * many clients and a client's identity lives in data (company master and client profile,
 * parameters, lists of values, seed data) or in the theme pack of the deployment.
 *
 * <ul>
 *   <li>No string literal of {@code backend/src/main/java} or {@code frontend/src} names the client
 *       (its name, short name, group or bank names, e-mail domain). Seed packages and seed data
 *       classes, tests and test fixtures, and the client's theme pack are allowed.
 *   <li>No string literal is a currency code: amounts are in the base currency of the company
 *       (company master) or the currency of the record, and the currencies offered come from the
 *       currency master.
 * </ul>
 *
 * <p>Comments are not checked. The check reads the sources, so it runs without a database.
 */
class ClientNeutralityTest {

  private static final Path ROOT = Path.of("").toAbsolutePath().getParent();

  /** Names of the client of the first deployment; its theme pack and seed data carry them. */
  private static final Pattern CLIENT =
      Pattern.compile(
          "\\bBDOI?\\b|\\bBDO[ -]?(?:Unibank|Insure)|@bdo\\.com|bdo-insure",
          Pattern.CASE_INSENSITIVE);

  /** The client names are upper case codes; "bdo" in lower case only as part of these. */
  private static final Pattern CLIENT_CASE =
      Pattern.compile("BDOI?|(?i:bdo[ -]?(?:unibank|insure)|@bdo\\.com)");

  /** Currency codes that must come from the company master or the record, never from code. */
  private static final Set<String> CURRENCIES =
      Set.of(
          "PHP", "USD", "EUR", "GBP", "JPY", "SGD", "HKD", "AUD", "CAD", "CHF", "CNY", "IDR", "MYR",
          "THB", "INR", "KRW", "NZD");

  /** A three-letter code standing alone (not part of a number series such as INR-2026-1). */
  private static final Pattern CODE =
      Pattern.compile("(?<![-_A-Za-z0-9])[A-Z]{3}(?![-_A-Za-z0-9])");

  /** A Java string literal (text blocks are not used for such values). */
  private static final Pattern JAVA_STRING =
      Pattern.compile("\"([^\"\\\\\\n]*+(?:\\\\.[^\"\\\\\\n]*+)*+)\"");

  /** A TypeScript string or template literal. */
  private static final Pattern TS_STRING =
      Pattern.compile(
          "'([^'\\\\\\n]*+(?:\\\\.[^'\\\\\\n]*+)*+)'|\"([^\"\\\\\\n]*+(?:\\\\.[^\"\\\\\\n]*+)*+)\"|`([^`\\\\]*+(?:\\\\.[^`\\\\]*+)*+)`");

  /** JSX text between tags, e.g. {@code <th>Name</th>}. */
  private static final Pattern JSX_TEXT = Pattern.compile(">([^<>{}]*[A-Za-z][^<>{}]*)<");

  private static final Pattern BLOCK_COMMENT = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);
  private static final Pattern LINE_COMMENT = Pattern.compile("(?m)^\\s*//.*$|\\s//[^'\"`\\n]*$");

  @Test
  void theBackendNamesNoClient() {
    assertThat(findings(javaSources(), JAVA_STRING, this::clientName))
        .as("client names in platform code (use the client profile, parameters or lists)")
        .isEmpty();
  }

  @Test
  void theFrontendNamesNoClient() {
    assertThat(findings(frontendSources(), TS_STRING, this::clientName))
        .as("client names in the screens (use the client profile or the theme pack)")
        .isEmpty();
    assertThat(findings(frontendSources(), JSX_TEXT, this::clientName))
        .as("client names in the screens (use the client profile or the theme pack)")
        .isEmpty();
  }

  @Test
  void theBackendWritesNoCurrency() {
    assertThat(findings(javaSources(), JAVA_STRING, ClientNeutralityTest::currency))
        .as("currency literals in platform code (use the base currency of the company)")
        .isEmpty();
  }

  @Test
  void theFrontendWritesNoCurrency() {
    assertThat(findings(frontendSources(), TS_STRING, ClientNeutralityTest::currency))
        .as("currency literals in the screens (use the base currency or the currency master)")
        .isEmpty();
    assertThat(findings(frontendSources(), JSX_TEXT, ClientNeutralityTest::currency))
        .as("currency literals in the screens (use the base currency or the currency master)")
        .isEmpty();
  }

  @Test
  void theChecksRecogniseClientNamesAndCurrencies() {
    assertThat(clientName("Via BDOI")).isTrue();
    assertThat(clientName("BDO Bank Client")).isTrue();
    assertThat(clientName("BDO-CA")).isTrue();
    assertThat(clientName("juan@bdo.com.ph")).isTrue();
    assertThat(clientName("BROKER_ONLY")).isFalse();
    assertThat(clientName("bdoi_location")).isFalse();
    assertThat(currency("PHP")).isTrue();
    assertThat(currency(" USD ")).isTrue();
    assertThat(currency("ALL")).isFalse();
    assertThat(currency("Amount (PHP)")).isTrue();
    assertThat(currency("PHPX")).isFalse();
    assertThat(currency("INR-")).isFalse();
    assertThat(currency("SCH-PR-PHP")).isFalse();
  }

  private boolean clientName(String literal) {
    Matcher m = CLIENT.matcher(literal);
    while (m.find()) {
      if (CLIENT_CASE.matcher(m.group()).matches()) {
        return true;
      }
    }
    return false;
  }

  private static boolean currency(String literal) {
    Matcher m = CODE.matcher(literal);
    while (m.find()) {
      if (CURRENCIES.contains(m.group())) {
        return true;
      }
    }
    return false;
  }

  private interface Check {
    boolean flags(String literal);
  }

  private static List<String> findings(List<Path> files, Pattern literal, Check check) {
    List<String> found = new ArrayList<>();
    for (Path file : files) {
      String code = withoutComments(read(file));
      Matcher m = literal.matcher(code);
      while (m.find()) {
        String text = firstGroup(m);
        if (text != null && check.flags(text)) {
          found.add(ROOT.relativize(file) + ": " + text.strip());
        }
      }
    }
    return found;
  }

  private static String firstGroup(Matcher m) {
    for (int g = 1; g <= m.groupCount(); g++) {
      if (m.group(g) != null) {
        return m.group(g);
      }
    }
    return null;
  }

  private static String withoutComments(String code) {
    return LINE_COMMENT.matcher(BLOCK_COMMENT.matcher(code).replaceAll(" ")).replaceAll("");
  }

  private static List<Path> javaSources() {
    return files(ROOT.resolve("backend/src/main/java"), ".java").stream()
        .filter(p -> !seed(p))
        .toList();
  }

  /** Seed packages and seed data classes carry the client's SIT and UAT data. */
  private static boolean seed(Path p) {
    String path = p.toString().replace('\\', '/');
    String name = p.getFileName().toString();
    return path.contains("/seed/") || name.contains("SeedData") || name.startsWith("Seed");
  }

  private static List<Path> frontendSources() {
    return Stream.of(".ts", ".tsx")
        .flatMap(ext -> files(ROOT.resolve("frontend/src"), ext).stream())
        .filter(p -> !frontendExempt(p))
        .toList();
  }

  /** Tests, test fixtures and the theme packs (the client's branding) are exempt. */
  private static boolean frontendExempt(Path p) {
    String path = p.toString().replace('\\', '/');
    String name = p.getFileName().toString();
    return name.contains(".test.")
        || name.startsWith("testWrapper")
        || name.toLowerCase(java.util.Locale.ROOT).endsWith("fixtures.ts")
        || path.contains("/src/test/")
        || path.contains("/src/theme/packs/");
  }

  private static List<Path> files(Path dir, String ext) {
    if (!Files.isDirectory(dir)) {
      return List.of();
    }
    try (Stream<Path> walk = Files.walk(dir)) {
      return walk.filter(p -> p.toString().endsWith(ext)).sorted().toList();
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  private static String read(Path file) {
    try {
      return Files.readString(file, StandardCharsets.UTF_8);
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }
}
