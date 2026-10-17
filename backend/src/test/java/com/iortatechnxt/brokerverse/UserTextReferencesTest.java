package com.iortatechnxt.brokerverse;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Standing check that the text the server shows to users carries no internal project references:
 * requirement identifiers ({@code BRNB.108}, {@code CSHID.016}), open-question numbers ({@code
 * Q06}, {@code OQ42}, {@code AQ19}), change requests ({@code DCR-12}), {@code FR-} numbers, "Annex"
 * and "BRD" page references, or "pending decision".
 *
 * <p>Scanned: every string literal of {@code backend/src/main/java} (exception and validation
 * messages, notification and e-mail texts, bulk template column descriptions, report titles and
 * notes, job descriptions) and the values of {@code application.yml}. Code comments and Javadoc may
 * keep the references. The reference data written by the Flyway migrations is checked on the
 * migrated database by {@code UserTextReferencesIT}, as applied migrations are never edited.
 */
class UserTextReferencesTest {

  /** Internal references that must not reach a screen, a document or a message. */
  static final Pattern INTERNAL_REFERENCE =
      Pattern.compile(
          "\\b[A-Z]{2,6}ID\\.\\d{3}" // CSHID.016, ADJID.021, RMTID.039
              + "|\\bBR[A-Z]{1,5}\\.\\d{3}" // BRNB.108, BRCLM.010, BRCLXN.049
              + "|\\b[A-Z]{0,3}Q\\d{2}\\b" // Q06, OQ42, AQ19, CLQ16
              + "|\\bBRD\\b|\\bAnnex\\b|\\bAppendix [A-Z]\\b"
              + "|\\bDCR-?\\d|\\bFR-\\d|\\bUAM-NFR-\\d|\\bSNSRP-\\d|\\bBRID-\\d|\\bPMADD\\d"
              + "|(?i:pending decision)|\\((?:to confirm|parked)\\)");

  private static final Path BACKEND = Path.of("").toAbsolutePath();

  @Test
  void serverTextCarriesNoInternalReferences() throws IOException {
    List<String> hits = new ArrayList<>();
    try (Stream<Path> files = Files.walk(BACKEND.resolve("src/main/java"))) {
      files.filter(f -> f.toString().endsWith(".java")).forEach(f -> hits.addAll(scanJava(f)));
    }
    hits.addAll(scanYaml(BACKEND.resolve("src/main/resources/application.yml")));
    assertThat(hits).isEmpty();
  }

  @Test
  void theCheckRecognisesTheReferences() {
    for (String text :
        List.of(
            "A cost centre is required (BRNB.108)",
            "Only e-mail placement is available (Q06)",
            "Needs the BDO account number (10 to 16 digits, AQ19)",
            "Layout of the report (CSHID.023 Annex II #1, draft)",
            "Layout of BRD p.59-61",
            "Pending decision on the format",
            "Motor theft (to confirm)",
            "Access requests by status (UAM-NFR-40)")) {
      assertThat(INTERNAL_REFERENCE.matcher(text).find()).as(text).isTrue();
    }
    for (String text :
        List.of(
            "Quarter Q1 of 2026",
            "Form 1702Q worksheet",
            "COST_CENTER_REQUIRED",
            "The booking date 2026-09-26 is in the future",
            "Draft voucher saved")) {
      assertThat(INTERNAL_REFERENCE.matcher(text).find()).as(text).isFalse();
    }
  }

  @Test
  void stringLiteralsAreReadOutsideComments() {
    String source =
        "/** Javadoc (BRNB.108). */\n"
            + "class A { // note OQ42\n"
            + "  String a = \"Give the reason\"; /* AQ19 */\n"
            + "  char c = '\"';\n"
            + "  String b = \"Escaped \\\" quote\";\n"
            + "}\n";
    assertThat(literals(source)).containsExactly("Give the reason", "Escaped \\\" quote");
  }

  private static List<String> scanJava(Path file) {
    List<String> hits = new ArrayList<>();
    String source = read(file);
    for (String literal : literals(source)) {
      if (INTERNAL_REFERENCE.matcher(literal).find()) {
        hits.add(BACKEND.relativize(file) + ": \"" + literal.strip() + "\"");
      }
    }
    return hits;
  }

  private static List<String> scanYaml(Path file) {
    List<String> hits = new ArrayList<>();
    List<String> lines = List.of(read(file).split("\n", -1));
    for (int i = 0; i < lines.size(); i++) {
      String line = lines.get(i);
      String value = line.strip().startsWith("#") ? "" : line;
      if (INTERNAL_REFERENCE.matcher(value).find()) {
        hits.add(BACKEND.relativize(file) + ":" + (i + 1) + ": " + line.strip());
      }
    }
    return hits;
  }

  /** The string literals (plain and text blocks) of a Java source, comments skipped. */
  static List<String> literals(String source) {
    List<String> literals = new ArrayList<>();
    int i = 0;
    int n = source.length();
    while (i < n) {
      if (source.startsWith("//", i)) {
        int end = source.indexOf('\n', i);
        i = end < 0 ? n : end;
      } else if (source.startsWith("/*", i)) {
        i = source.indexOf("*/", i + 2) + 2;
      } else if (source.startsWith("\"\"\"", i)) {
        int end = source.indexOf("\"\"\"", i + 3);
        literals.add(source.substring(i + 3, end));
        i = end + 3;
      } else if (source.charAt(i) == '"') {
        int end = closingQuote(source, i);
        literals.add(source.substring(i + 1, end));
        i = end + 1;
      } else if (source.charAt(i) == '\'') {
        i = closingQuote(source, i) + 1;
      } else {
        i++;
      }
    }
    return literals;
  }

  /** Index of the quote closing the character or string literal opened at {@code start}. */
  private static int closingQuote(String source, int start) {
    char quote = source.charAt(start);
    int j = start + 1;
    while (source.charAt(j) != quote) {
      j += source.charAt(j) == '\\' ? 2 : 1;
    }
    return j;
  }

  private static String read(Path file) {
    try {
      return Files.readString(file, StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException(file.toString(), e);
    }
  }
}
