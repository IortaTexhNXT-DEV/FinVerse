package com.iortatechnxt.brokerverse;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.security.seed.SeedPasswords;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Standing check of the client rule that the SIT/UAT password is never written into a file: no file
 * of the project (tracked files, or the working tree without {@code .git}) holds it. The password
 * itself is not stored here either. Every word followed by {@code @} and digits (the shape of the
 * password) is checked against the password hash that the seed scripts give the SIT/UAT users
 * ({@link SeedPasswords#SCRIPT_HASH}), so the check adds no new secret material.
 */
class SeedPasswordGuardTest {

  private static final Path ROOT = Path.of("").toAbsolutePath().getParent();

  /** Candidate tokens: a word followed by {@code @} and digits. */
  static final Pattern CANDIDATE = Pattern.compile("[A-Za-z][A-Za-z0-9]*@\\d+");

  private static final Pattern SEED_HASH =
      Pattern.compile("\\$2[aby]\\$\\d\\d\\$[./A-Za-z0-9]{53}");

  private static final Set<String> SKIPPED_FOLDERS =
      Set.of(".git", "node_modules", "target", "dist", "build", ".claude", "coverage");
  private static final Set<String> OFFICE = Set.of("docx", "xlsx", "pptx");
  private static final long MAX_SIZE = 32L * 1024 * 1024;

  private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();

  @Test
  void noFileHoldsTheSitUatPassword() throws IOException {
    Map<String, List<String>> tokens = new HashMap<>();
    for (Path file : projectFiles()) {
      for (String token : candidates(file)) {
        tokens.computeIfAbsent(token, t -> new ArrayList<>()).add(ROOT.relativize(file).toString());
      }
    }
    List<String> hits = new ArrayList<>();
    tokens.forEach(
        (token, files) -> {
          if (bcrypt.matches(token, SeedPasswords.SCRIPT_HASH)) {
            hits.addAll(files);
          }
        });
    assertThat(hits).as("files holding the SIT/UAT password").isEmpty();
  }

  @Test
  void everySeedScriptUsesTheHashTheGuardAndTheSeedRunnerKnow() throws IOException {
    Path seed = ROOT.resolve("backend/src/main/resources/db/seed");
    Set<String> hashes = new TreeSet<>();
    try (Stream<Path> files = Files.list(seed)) {
      for (Path f : files.filter(p -> p.toString().endsWith(".sql")).toList()) {
        Matcher m = SEED_HASH.matcher(Files.readString(f, StandardCharsets.UTF_8));
        while (m.find()) {
          hashes.add(m.group());
        }
      }
    }
    assertThat(hashes).containsExactly(SeedPasswords.SCRIPT_HASH);
  }

  @Test
  void theCheckFindsAPasswordOfThatShapeInAnyText() {
    String password = "GuardCheck@" + (1000 + (System.nanoTime() & 0xFFF));
    String hash = bcrypt.encode(password);
    String text = "{\"username\":\"auditor\",\"password\":\"" + password + "\"} ops@example.ph";
    List<String> found = new ArrayList<>();
    Matcher m = CANDIDATE.matcher(text);
    while (m.find()) {
      found.add(m.group());
    }
    assertThat(found).contains(password);
    assertThat(found.stream().filter(t -> bcrypt.matches(t, hash))).containsExactly(password);
  }

  /** Tracked files of the project, or the working tree when git is not available. */
  private static List<Path> projectFiles() throws IOException {
    List<Path> tracked = trackedFiles();
    if (!tracked.isEmpty()) {
      return tracked;
    }
    try (Stream<Path> walk = Files.walk(ROOT)) {
      return walk.filter(Files::isRegularFile).filter(p -> !skipped(ROOT.relativize(p))).toList();
    }
  }

  private static boolean skipped(Path relative) {
    for (Path part : relative) {
      if (SKIPPED_FOLDERS.contains(part.toString())) {
        return true;
      }
    }
    return false;
  }

  private static List<Path> trackedFiles() {
    try {
      Process git =
          new ProcessBuilder("git", "ls-files", "-z")
              .directory(ROOT.toFile())
              .redirectErrorStream(false)
              .start();
      byte[] out;
      try (InputStream in = git.getInputStream()) {
        out = in.readAllBytes();
      }
      if (!git.waitFor(60, TimeUnit.SECONDS) || git.exitValue() != 0) {
        return List.of();
      }
      List<Path> files = new ArrayList<>();
      for (String name : new String(out, StandardCharsets.UTF_8).split("\0")) {
        Path file = ROOT.resolve(name);
        if (!name.isEmpty() && Files.isRegularFile(file)) {
          files.add(file);
        }
      }
      return files;
    } catch (IOException e) {
      return List.of();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return List.of();
    }
  }

  private static Set<String> candidates(Path file) {
    String name = file.getFileName().toString();
    String ext = name.substring(name.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    Set<String> found = new TreeSet<>();
    try {
      if (Files.size(file) > MAX_SIZE) {
        return found;
      }
      String text;
      if (OFFICE.contains(ext)) {
        text = officeText(file);
      } else {
        byte[] bytes = Files.readAllBytes(file);
        if (binary(bytes)) {
          return found;
        }
        text = new String(bytes, StandardCharsets.UTF_8);
      }
      Matcher m = CANDIDATE.matcher(text);
      while (m.find()) {
        found.add(m.group());
      }
    } catch (IOException e) {
      throw new UncheckedIOException(file.toString(), e);
    }
    return found;
  }

  private static boolean binary(byte[] bytes) {
    int n = Math.min(bytes.length, 8000);
    for (int i = 0; i < n; i++) {
      if (bytes[i] == 0) {
        return true;
      }
    }
    return false;
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
}
