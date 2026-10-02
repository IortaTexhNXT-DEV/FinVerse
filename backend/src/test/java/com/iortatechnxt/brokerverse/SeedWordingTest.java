package com.iortatechnxt.brokerverse;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * The SIT/UAT data reads as business data: no name, remark or description carries a "(seed)" marker
 * or starts with "Seed". The start-up seed classes write business wording, and the seed script
 * V2090 corrects every marked value of the earlier seed scripts (which are applied and stay as they
 * are). The check reads the sources, so it runs without a database.
 */
class SeedWordingTest {

  private static final Path ROOT = Path.of("").toAbsolutePath().getParent();

  private static final Path SEED_SQL = ROOT.resolve("backend/src/main/resources/db/seed");

  private static final Path CORRECTION = SEED_SQL.resolve("V2090__seed_business_wording.sql");

  /** Version of the correction script; later seed scripts are checked as written. */
  private static final int CORRECTION_VERSION = 2090;

  /** A seed marker in a value: "(seed)", "(Seed issuer)", a leading "Seed " or "seed data". */
  private static final Pattern MARKED =
      Pattern.compile("\\((?i:seed)[^)]*\\)|^Seed[ -]|(?i:\\bseed data\\b)|\\bseed stories\\b");

  /** Seed values that are not shown to users, with the reason. */
  private static final Map<String, String> NOT_SHOWN =
      Map.of(
          "% BrokerVerse seed KYC document", "content of the seeded PDF file, not displayed",
          "Seed extract ", "failure of the seed loader at start-up, written to the log only",
          "Seed batch ", "failure of the seed loader at start-up, written to the log only",
          "seed", "action code of the seeded workflow entries, corrected by V2090 to start");

  /**
   * Seeded values of insert-only audit trails, left as they are (known limitation): V2090 never
   * switches a guard off, so the screening case timeline (scr_case_event, insert-only by trigger)
   * keeps its "Opened by ... (seed)" entries. Keyed by script and literal.
   */
  private static final Set<String> AUDIT_TRAIL_KEPT =
      Set.of("V1952__seed_screening_cases.sql: (seed)");

  /** Statements that would switch a table guard off. */
  private static final Pattern GUARD_OFF =
      Pattern.compile(
          "(?i)disable\\s+trigger|session_replication_role|drop\\s+trigger|alter\\s+table\\s+\\S+\\s+disable");

  private static final Pattern SQL_STRING = Pattern.compile("'([^']*+(?:''[^']*+)*+)'");
  private static final Pattern SQL_COMMENT = Pattern.compile("--[^\\n]*");
  private static final Pattern JAVA_STRING =
      Pattern.compile("\"([^\"\\\\\\n]*+(?:\\\\.[^\"\\\\\\n]*+)*+)\"");
  private static final Pattern BLOCK_COMMENT = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);
  private static final Pattern LINE_COMMENT = Pattern.compile("(?m)^\\s*//.*$");
  private static final Pattern MARKER_RULE = Pattern.compile("marker constant text := '([^']+)'");
  private static final Pattern VERSION = Pattern.compile("^V(\\d+)__");

  /** Characters before a literal searched for the start of its log statement. */
  private static final int LOOK_BACK = 200;

  @Test
  void theSeedClassesWriteBusinessWording() {
    List<String> found = new ArrayList<>();
    for (Path file : files(ROOT.resolve("backend/src/main/java"), ".java")) {
      String path = file.toString().replace('\\', '/');
      if (!path.contains("/seed/")) {
        continue;
      }
      String code =
          LINE_COMMENT.matcher(BLOCK_COMMENT.matcher(read(file)).replaceAll(" ")).replaceAll("");
      Matcher m = JAVA_STRING.matcher(code);
      while (m.find()) {
        String text = m.group(1);
        if (marked(text) && !logged(code, m.start()) && !NOT_SHOWN.containsKey(text)) {
          found.add(ROOT.relativize(file) + ": " + text);
        }
      }
    }
    assertThat(found).as("seed markers in values written by the seed classes").isEmpty();
  }

  @Test
  void theCorrectionCoversEveryMarkedValueOfTheSeedScripts() {
    String correction = read(CORRECTION);
    Matcher rule = MARKER_RULE.matcher(correction);
    assertThat(rule.find()).as("marker rule of V2090").isTrue();
    Pattern marker = Pattern.compile(rule.group(1));
    Set<String> corrected = literals(correction);

    List<String> uncovered = new ArrayList<>();
    for (Path file : files(SEED_SQL, ".sql")) {
      if (version(file) >= CORRECTION_VERSION) {
        continue;
      }
      for (String text : literals(read(file))) {
        if (AUDIT_TRAIL_KEPT.contains(file.getFileName() + ": " + text.strip())) {
          continue;
        }
        boolean covered =
            corrected.contains(text)
                || corrected.contains(text + "%")
                || marker.matcher("Name" + text).matches()
                || text.contains("@")
                || NOT_SHOWN.containsKey(text);
        if (marked(text) && !covered) {
          uncovered.add(file.getFileName() + ": " + text);
        }
      }
    }
    assertThat(uncovered).as("seeded values with a marker that V2090 does not correct").isEmpty();
  }

  @Test
  void theCorrectionNeverSwitchesAGuardOff() {
    String sql = SQL_COMMENT.matcher(read(CORRECTION)).replaceAll("");
    assertThat(GUARD_OFF.matcher(sql).find())
        .as("trigger or guard switched off in V2090")
        .isFalse();
    assertThat(sql)
        .as("audit trails with an update guard are left out")
        .contains("raise exception");
    assertThat(sql).doesNotContain("scr_case_event");
  }

  @Test
  void theMarkerRuleDropsTheMarkerOnly() {
    Matcher rule = MARKER_RULE.matcher(read(CORRECTION));
    assertThat(rule.find()).isTrue();
    Pattern marker = Pattern.compile(rule.group(1));
    assertThat(marker.matcher("Sales campaign (seed)").replaceAll("$1"))
        .isEqualTo("Sales campaign");
    assertThat(marker.matcher("Ayala Land Inc. (Seed issuer)").replaceAll("$1"))
        .isEqualTo("Ayala Land Inc.");
    assertThat(marker.matcher("AYALA PROPERTY LEASING (SEED)").replaceAll("$1"))
        .isEqualTo("AYALA PROPERTY LEASING");
    assertThat(marker.matcher("Seed collection").matches()).isFalse();
    assertThat(marker.matcher("Premium (seeded rate)").matches()).isFalse();
  }

  @Test
  void theCheckRecognisesMarkers() {
    assertThat(marked("Sales campaign (seed)")).isTrue();
    assertThat(marked("Seed case in ")).isTrue();
    assertThat(marked("Seed-only role: insurer suite access")).isTrue();
    assertThat(marked("Customer data (seed data)")).isTrue();
    assertThat(marked("Seeded rules")).isFalse();
    assertThat(marked("SIT/UAT users")).isFalse();
  }

  /** Whether a literal is part of a log statement (the log is not shown to users). */
  private static boolean logged(String code, int at) {
    String before = code.substring(Math.max(0, at - LOOK_BACK), at);
    int log = before.lastIndexOf("LOG.");
    return log >= 0 && before.indexOf(';', log) < 0;
  }

  private static boolean marked(String text) {
    return MARKED.matcher(text).find();
  }

  private static Set<String> literals(String sql) {
    Set<String> found = new HashSet<>();
    Matcher m = SQL_STRING.matcher(SQL_COMMENT.matcher(sql).replaceAll(""));
    while (m.find()) {
      found.add(m.group(1).replace("''", "'"));
    }
    return found;
  }

  private static int version(Path file) {
    Matcher m = VERSION.matcher(file.getFileName().toString());
    return m.find() ? Integer.parseInt(m.group(1)) : 0;
  }

  private static List<Path> files(Path dir, String ext) {
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
