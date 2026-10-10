package com.iortatechnxt.brokerverse;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * The e-mail addresses of the SIT/UAT clients read as business addresses: every address of the
 * placeholder or reserved example domains written by a seed script is rewritten by the last address
 * script, whose replacements carry no such domain.
 */
class SeedAddressTest {

  private static final Path SEED_SQL =
      Path.of("").toAbsolutePath().getParent().resolve("backend/src/main/resources/db/seed");

  private static final String FIX = "V2815__seed_client_addresses.sql";

  private static final Pattern MARKED_ADDRESS =
      Pattern.compile("[A-Za-z0-9._-]+@[A-Za-z0-9.-]*(?:seed-client\\.ph|\\.example)\\b");

  private static final Pattern PAIR = Pattern.compile("\\('([^']+)',\\s*'([^']+)'\\)");

  @Test
  void everyMarkedClientAddressIsRewrittenToABusinessAddress() throws IOException {
    String fix = Files.readString(SEED_SQL.resolve(FIX), StandardCharsets.UTF_8);
    List<String[]> pairs =
        PAIR.matcher(fix).results().map(m -> new String[] {m.group(1), m.group(2)}).toList();
    assertThat(pairs).isNotEmpty();
    for (String[] p : pairs) {
      assertThat(p[1]).doesNotContain("seed").doesNotEndWith(".example");
    }
    try (Stream<Path> files = Files.list(SEED_SQL)) {
      for (Path f : files.filter(x -> x.toString().endsWith(".sql")).toList()) {
        if (f.getFileName().toString().equals(FIX)) {
          continue;
        }
        Matcher m = MARKED_ADDRESS.matcher(Files.readString(f, StandardCharsets.UTF_8));
        while (m.find()) {
          String address = m.group();
          assertThat(pairs)
              .as("address %s of %s", address, f.getFileName())
              .anyMatch(p -> address.contains(p[0]));
        }
      }
    }
  }
}
