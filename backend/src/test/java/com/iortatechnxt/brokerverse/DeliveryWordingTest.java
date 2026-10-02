package com.iortatechnxt.brokerverse;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.junit.jupiter.api.Test;

/**
 * Standing check of the delivery wording (client content rules, 26-Sep-2026): the retired word for
 * seed data and the restricted tool and vendor names appear nowhere in {@code backend/src/main},
 * {@code frontend/src}, {@code docs/deliverables/src} or the text of the deliverable outputs (Word,
 * Excel, PowerPoint, Markdown and CSV files of {@code docs/deliverables/out}).
 *
 * <p>The names are stored in ROT13, as in {@code
 * docs/deliverables/src/testplans/build_test_plan.py}, so they are not spelled out here.
 * Third-party names that only look like a restricted name are allowed explicitly (the adjuster
 * company of the BRD-7 adjuster list).
 */
class DeliveryWordingTest {

  private static final Path ROOT = Path.of("").toAbsolutePath().getParent();

  private static final Pattern RESTRICTED =
      Pattern.compile(
          rot13("qrzb")
              + "(?!nstrat|li)|\\b(?:"
              + rot13("pynhqr|naguebcvp|bcranv|pungtcg|trzvav|pbcvybg|yynzn|tvguho")
              + "|"
              + rot13("tcg")
              + "-?\\d)\\b",
          Pattern.CASE_INSENSITIVE);

  /** Allowed third-party names, removed before the check. */
  private static final Pattern ALLOWED =
      Pattern.compile(
          rot13("trzvav")
              + " adjustment company|'"
              + rot13("TRZVAV")
              + "'|com\\."
              + rot13("tvguho")
              + "\\.",
          Pattern.CASE_INSENSITIVE);

  private static final Set<String> TEXT =
      Set.of(
          "java",
          "sql",
          "yml",
          "yaml",
          "properties",
          "factories",
          "xml",
          "json",
          "html",
          "txt",
          "csv",
          "md",
          "ts",
          "tsx",
          "js",
          "cjs",
          "css",
          "py",
          "dot",
          "mmd",
          "svg");
  private static final Set<String> OFFICE = Set.of("docx", "xlsx", "pptx");

  @Test
  void deliveredSourcesAndDocumentsUseTheAgreedWording() throws IOException {
    List<String> hits = new ArrayList<>();
    for (String folder :
        List.of(
            "backend/src/main", "frontend/src", "docs/deliverables/src", "docs/deliverables/out")) {
      Path dir = ROOT.resolve(folder);
      assertThat(dir).as("folder %s", folder).isDirectory();
      try (Stream<Path> files = Files.walk(dir)) {
        files.filter(Files::isRegularFile).forEach(f -> hits.addAll(scan(f)));
      }
    }
    assertThat(hits).isEmpty();
  }

  @Test
  void theCheckRecognisesTheRestrictedWords() {
    assertThat(RESTRICTED.matcher(rot13("Gur qrzb hfref")).find()).isTrue();
    assertThat(RESTRICTED.matcher(rot13("jevggra ol Pynhqr")).find()).isTrue();
    assertThat(RESTRICTED.matcher(rot13("TCG-4")).find()).isTrue();
    assertThat(RESTRICTED.matcher("The seed users, demolition works, demonstrated").find())
        .isFalse();
    assertThat(clean(rot13("Trzvav Nqwhfgzrag Pbzcnal"))).doesNotContainIgnoringCase("adjustment");
  }

  private static List<String> scan(Path file) {
    String name = file.getFileName().toString();
    String ext = name.substring(name.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    String rel = ROOT.relativize(file).toString();
    List<String> hits = new ArrayList<>();
    try {
      if (TEXT.contains(ext)) {
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        for (int i = 0; i < lines.size(); i++) {
          if (RESTRICTED.matcher(clean(lines.get(i))).find()) {
            hits.add(rel + ":" + (i + 1) + ": " + lines.get(i).strip());
          }
        }
      } else if (OFFICE.contains(ext)) {
        String text = officeText(file);
        var m = RESTRICTED.matcher(clean(text));
        while (m.find()) {
          String around =
              text.substring(Math.max(0, m.start() - 40), Math.min(text.length(), m.end() + 40));
          hits.add(rel + ": ..." + around + "...");
        }
      }
    } catch (IOException e) {
      throw new UncheckedIOException(rel, e);
    }
    return hits;
  }

  /** The text of the XML parts of an Office file, tags replaced by spaces. */
  private static String officeText(Path file) throws IOException {
    StringBuilder text = new StringBuilder();
    try (InputStream in = Files.newInputStream(file);
        ZipInputStream zip = new ZipInputStream(in)) {
      for (ZipEntry e = zip.getNextEntry(); e != null; e = zip.getNextEntry()) {
        if (e.getName().endsWith(".xml")) {
          String xml = new String(zip.readAllBytes(), StandardCharsets.UTF_8);
          text.append(xml.replaceAll("<[^>]+>", " ")).append('\n');
        }
      }
    }
    return text.toString();
  }

  private static String clean(String text) {
    return ALLOWED.matcher(text).replaceAll(" ");
  }

  private static String rot13(String text) {
    StringBuilder out = new StringBuilder(text.length());
    for (char c : text.toCharArray()) {
      if (c >= 'a' && c <= 'z') {
        out.append((char) ('a' + (c - 'a' + 13) % 26));
      } else if (c >= 'A' && c <= 'Z') {
        out.append((char) ('A' + (c - 'A' + 13) % 26));
      } else {
        out.append(c);
      }
    }
    return out.toString();
  }
}
