package com.iortatechnxt.brokerverse.opsledger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Every screen link that the Operations modules put in a notice, an alert or a work tile opens a
 * screen of the application: the link, with or without a record number, starts like a route of a
 * frontend module (a notice that opened no screen was found by the business sign-off).
 */
class NoticeLinksTest {

  private static final Path MAIN = Path.of("src/main/java/com/iortatechnxt/brokerverse");
  private static final Path FEATURES = Path.of("../frontend/src/features");
  private static final List<String> MODULES =
      List.of("opsledger", "cashiering", "remittance", "adjustment", "prodrecon", "commission");
  private static final Pattern LINK =
      Pattern.compile(
          "\"(/(?:operations|cashiering|remittance|adjustment|prodrecon|commission)/[A-Za-z0-9/_-]*)");
  private static final Pattern ROUTE = Pattern.compile("path: '(/[^']*)'");

  @Test
  void everyNoticeLinkOpensAScreen() {
    assumeTrue(Files.isDirectory(FEATURES), "frontend sources not in this checkout");
    List<String> routes = routes();
    TreeSet<String> broken = new TreeSet<>();
    for (String module : MODULES) {
      for (String link : links(MAIN.resolve(module))) {
        if (routes.stream().noneMatch(r -> opens(r, link))) {
          broken.add(link);
        }
      }
    }
    assertThat(broken).as("links that open no screen").isEmpty();
  }

  /** A route opens a link when the link is the route, or the route without its record parameter. */
  private static boolean opens(String route, String link) {
    String fixed = route.replaceAll("/:[A-Za-z]+.*$", "/");
    return route.equals(link) || (link.endsWith("/") && fixed.equals(link));
  }

  private static List<String> routes() {
    List<String> out = new ArrayList<>();
    for (Path file : files(FEATURES, "module.ts")) {
      Matcher m = ROUTE.matcher(read(file));
      while (m.find()) {
        out.add(m.group(1));
      }
    }
    return out;
  }

  private static List<String> links(Path dir) {
    List<String> out = new ArrayList<>();
    for (Path file : files(dir, ".java")) {
      Matcher m = LINK.matcher(read(file));
      while (m.find()) {
        out.add(m.group(1));
      }
    }
    return out;
  }

  private static List<Path> files(Path dir, String suffix) {
    try (Stream<Path> walk = Files.walk(dir)) {
      return walk.filter(p -> p.toString().endsWith(suffix)).toList();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static String read(Path file) {
    try {
      return Files.readString(file);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
