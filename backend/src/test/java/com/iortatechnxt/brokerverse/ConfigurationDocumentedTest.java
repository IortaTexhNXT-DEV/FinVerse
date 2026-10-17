package com.iortatechnxt.brokerverse;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * Standing check of the configuration reference: every environment setting read by {@code
 * application.yml} ({@code ${NAME}} or {@code ${NAME:default}}) is documented by its full name in
 * {@code docs/operations/CONFIGURATION.md}, so operations can set every value the application
 * reads.
 */
class ConfigurationDocumentedTest {

  private static final Path ROOT = Path.of("").toAbsolutePath().getParent();

  private static final Pattern SETTING = Pattern.compile("\\$\\{([A-Z][A-Z0-9_]*)[:}]");

  @Test
  void everyEnvironmentSettingOfTheApplicationIsDocumented() throws IOException {
    String yaml =
        Files.readString(
            ROOT.resolve("backend/src/main/resources/application.yml"), StandardCharsets.UTF_8);
    String reference =
        Files.readString(ROOT.resolve("docs/operations/CONFIGURATION.md"), StandardCharsets.UTF_8);
    Set<String> settings = new TreeSet<>();
    Matcher m = SETTING.matcher(yaml);
    while (m.find()) {
      settings.add(m.group(1));
    }
    assertThat(settings).hasSizeGreaterThan(100);
    Set<String> missing = new TreeSet<>();
    for (String name : settings) {
      if (!Pattern.compile("\\b" + name + "\\b").matcher(reference).find()) {
        missing.add(name);
      }
    }
    assertThat(missing).as("settings of application.yml missing in CONFIGURATION.md").isEmpty();
  }
}
