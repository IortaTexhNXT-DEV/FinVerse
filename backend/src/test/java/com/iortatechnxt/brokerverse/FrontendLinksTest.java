package com.iortatechnxt.brokerverse;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * The screen links the backend emits (notifications, workflow cases, approvals, work counts) open a
 * screen of the frontend: every string literal of the main code that starts with the first segment
 * of a frontend route ({@code path: '...'} of the feature modules and their screen lists) matches a
 * route. A literal ending in {@code /} is followed by an id; the query string is ignored.
 */
class FrontendLinksTest {

  private static final Path FEATURES = Path.of("../frontend/src/features");
  private static final Path MAIN = Path.of("src/main/java");
  private static final Pattern ROUTE = Pattern.compile("path:\\s*'(/[^']*)'");
  private static final Pattern LITERAL = Pattern.compile("\"(/[a-z][^\"\\s]*)\"");
  private static final Pattern TEMPLATE = Pattern.compile("%[sd]|\\{[^}]*}");
  private static final String ANY = "{}";

  @Test
  void everyEmittedLinkOpensAScreen() throws IOException {
    Set<String> routes = routes();
    Set<String> sections = new HashSet<>();
    routes.forEach(r -> sections.addAll(segments(r).stream().limit(1).toList()));
    Set<String> broken = new TreeSet<>();
    for (Path file : files(MAIN, ".java")) {
      List<String> lines = Files.readAllLines(file);
      for (int i = 0; i < lines.size(); i++) {
        String line = lines.get(i);
        if (line.contains("Mapping(")) {
          continue;
        }
        Matcher m = LITERAL.matcher(line);
        while (m.find()) {
          String link = m.group(1);
          String path = TEMPLATE.matcher(link.split("[?#]", 2)[0]).replaceAll(ANY);
          List<String> segments = new ArrayList<>(segments(path));
          if (path.endsWith("/")) {
            segments.add(ANY);
          }
          if (sections.contains(segments.get(0))
              && routes.stream().noneMatch(r -> fits(r, segments))) {
            broken.add(link + " (" + MAIN.relativize(file) + ":" + (i + 1) + ")");
          }
        }
      }
    }
    assertThat(routes).hasSizeGreaterThan(100);
    assertThat(broken).as("links without a frontend route").isEmpty();
  }

  private static Set<String> routes() throws IOException {
    Set<String> routes = new HashSet<>();
    for (Path file : files(FEATURES, ".ts")) {
      String name = file.getFileName().toString();
      if (name.equals("module.ts") || name.endsWith("Module.ts") || name.equals("screens.ts")) {
        Matcher m = ROUTE.matcher(Files.readString(file));
        while (m.find()) {
          routes.add(m.group(1));
        }
      }
    }
    return routes;
  }

  private static boolean fits(String route, List<String> link) {
    List<String> parts = segments(route);
    if (parts.size() != link.size()) {
      return false;
    }
    for (int i = 0; i < parts.size(); i++) {
      String part = parts.get(i);
      if (!part.startsWith(":") && !part.equals(link.get(i)) && !ANY.equals(link.get(i))) {
        return false;
      }
    }
    return true;
  }

  private static List<String> segments(String path) {
    return Arrays.stream(path.split("/")).filter(s -> !s.isEmpty()).toList();
  }

  private static List<Path> files(Path root, String suffix) throws IOException {
    try (Stream<Path> walk = Files.walk(root)) {
      return walk.filter(p -> p.toString().endsWith(suffix)).toList();
    }
  }
}
