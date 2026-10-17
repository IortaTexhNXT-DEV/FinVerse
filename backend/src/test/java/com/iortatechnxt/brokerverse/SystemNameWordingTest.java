package com.iortatechnxt.brokerverse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.iortatechnxt.brokerverse.common.office.BrandAssets;
import com.iortatechnxt.brokerverse.messaging.domain.Notice;
import com.iortatechnxt.brokerverse.messaging.service.MessageService;
import com.iortatechnxt.brokerverse.messaging.service.NotificationPreferenceService;
import com.iortatechnxt.brokerverse.messaging.service.NotificationService;
import com.iortatechnxt.brokerverse.messaging.service.OutboundEmail;
import com.iortatechnxt.brokerverse.nbadmin.service.PasswordNoticeMailer;
import com.iortatechnxt.brokerverse.security.service.AuthPasswordService.PasswordExpiry;
import com.iortatechnxt.brokerverse.security.service.PasswordResetRequested;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Texts to users name the system of the deployment (the system name of the theme pack, BIBS for the
 * first client), never the platform: e-mails and their subjects, notifications, messages, report
 * footers and document properties. The platform name stays in package, class, configuration and log
 * names only.
 */
class SystemNameWordingTest {

  private static final Path ROOT = Path.of("").toAbsolutePath().getParent();

  private static final String PLATFORM = "BrokerVerse";

  /** String literals that may keep the platform name, with the reason. */
  private static final Map<String, String> ALLOWED =
      Map.of(
          "iNXT BrokerVerse API",
              "title of the API documentation for developers (off in production)",
          "BrokerVerse", "technical request header of the token refresh, not shown to users",
          "iNXT BrokerVerse", "platform under \"Powered by\" in the About dialog of the screens");

  private static final Pattern JAVA_STRING =
      Pattern.compile("\"([^\"\\\\\\n]*+(?:\\\\.[^\"\\\\\\n]*+)*+)\"");

  private static final Pattern TS_STRING =
      Pattern.compile(
          "'([^'\\\\\\n]*+(?:\\\\.[^'\\\\\\n]*+)*+)'"
              + "|\"([^\"\\\\\\n]*+(?:\\\\.[^\"\\\\\\n]*+)*+)\""
              + "|`([^`\\\\]*+(?:\\\\.[^`\\\\]*+)*+)`");

  private static final Pattern JSX_TEXT = Pattern.compile(">([^<>{}]*[A-Za-z][^<>{}]*)<");

  private static final Pattern BLOCK_COMMENT = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);

  private static final Pattern LINE_COMMENT = Pattern.compile("(?m)^\\s*//.*$|\\s//[^'\"`\\n]*$");

  @Test
  void theThemePackNamesTheSystem() {
    assertThat(BrandAssets.SYSTEM_NAME).isEqualTo("BIBS");
  }

  @Test
  void thePasswordResetEmailNamesTheSystem() {
    MessageService messages = mock(MessageService.class);
    PasswordNoticeMailer mailer =
        new PasswordNoticeMailer(
            messages, mock(NotificationService.class), mock(NotificationPreferenceService.class));

    mailer.onResetRequested(
        new PasswordResetRequested(
            "jdelacruz",
            "Juan dela Cruz",
            "juan@example.ph",
            "https://bibs/reset?t=x",
            Instant.parse("2026-10-02T08:00:00Z")));

    ArgumentCaptor<OutboundEmail> mail = ArgumentCaptor.forClass(OutboundEmail.class);
    verify(messages).queueEmail(mail.capture());
    assertThat(mail.getValue().subject()).isEqualTo("BIBS: reset your password");
    assertThat(mail.getValue().body())
        .contains("A new BIBS password was requested")
        .doesNotContain(PLATFORM);
  }

  @Test
  void thePasswordExpiryNoticeNamesTheSystem() {
    MessageService messages = mock(MessageService.class);
    NotificationService notifications = mock(NotificationService.class);
    NotificationPreferenceService preferences = mock(NotificationPreferenceService.class);
    when(preferences.wantsEmail(anyString(), anyString())).thenReturn(true);
    PasswordNoticeMailer mailer = new PasswordNoticeMailer(messages, notifications, preferences);

    mailer.notifyExpiry(
        new PasswordExpiry(
            "jdelacruz",
            "Juan dela Cruz",
            "juan@example.ph",
            Instant.parse("2026-10-09T08:00:00Z")));

    ArgumentCaptor<OutboundEmail> mail = ArgumentCaptor.forClass(OutboundEmail.class);
    verify(messages).queueEmail(mail.capture());
    assertThat(mail.getValue().subject()).isEqualTo("BIBS: your password expires soon");
    assertThat(mail.getValue().body()).startsWith("Your BIBS password expires on ");
    verify(notifications).notifyUser(eq("jdelacruz"), any(Notice.class), anyString());
  }

  @Test
  void noBackendTextNamesThePlatform() {
    assertThat(findings(files(ROOT.resolve("backend/src/main/java"), ".java"), JAVA_STRING))
        .as("texts to users naming the platform (use BrandAssets.SYSTEM_NAME)")
        .isEmpty();
  }

  @Test
  void noScreenTextNamesThePlatform() {
    List<Path> sources =
        Stream.of(".ts", ".tsx")
            .flatMap(ext -> files(ROOT.resolve("frontend/src"), ext).stream())
            .filter(p -> !p.getFileName().toString().contains(".test."))
            .toList();
    assertThat(findings(sources, TS_STRING))
        .as("screen texts naming the platform (use the product of the theme pack)")
        .isEmpty();
    assertThat(findings(sources, JSX_TEXT))
        .as("screen texts naming the platform (use the product of the theme pack)")
        .isEmpty();
  }

  @Test
  void theCheckFindsThePlatformName() {
    assertThat(names("\"BrokerVerse: reset your password\"", JAVA_STRING)).isTrue();
    assertThat(names("// BrokerVerse rule\n\"Applicable\"", JAVA_STRING)).isFalse();
    assertThat(names("<p>Welcome to BrokerVerse</p>", JSX_TEXT)).isTrue();
    assertThat(names("'iNXT BrokerVerse API'", TS_STRING)).isFalse();
  }

  private static boolean names(String code, Pattern literal) {
    return !scan(code, literal).isEmpty();
  }

  private static List<String> findings(List<Path> files, Pattern literal) {
    List<String> found = new ArrayList<>();
    for (Path file : files) {
      for (String text : scan(read(file), literal)) {
        found.add(ROOT.relativize(file) + ": " + text);
      }
    }
    return found;
  }

  private static List<String> scan(String code, Pattern literal) {
    List<String> found = new ArrayList<>();
    String plain = LINE_COMMENT.matcher(BLOCK_COMMENT.matcher(code).replaceAll(" ")).replaceAll("");
    Matcher m = literal.matcher(plain);
    while (m.find()) {
      String text = firstGroup(m);
      if (text != null && text.contains(PLATFORM) && !ALLOWED.containsKey(text.strip())) {
        found.add(text.strip());
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
