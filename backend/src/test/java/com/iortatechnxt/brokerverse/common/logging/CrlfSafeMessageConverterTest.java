package com.iortatechnxt.brokerverse.common.logging;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.OutputStreamAppender;
import ch.qos.logback.core.encoder.LayoutWrappingEncoder;
import ch.qos.logback.core.joran.spi.JoranException;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

class CrlfSafeMessageConverterTest {

  @Test
  void everyLineBreakBecomesAnUnderscore() {
    String separators = "d" + (char) 0x85 + "e" + (char) 0x2028 + "f" + (char) 0x2029 + "g";
    assertThat(CrlfSafeMessageConverter.neutralise("a\r\nb\nc\r" + separators))
        .isEqualTo("a__b_c_d_e_f_g");
    assertThat(CrlfSafeMessageConverter.neutralise("one line")).isEqualTo("one line");
    assertThat(CrlfSafeMessageConverter.neutralise(null)).isNull();
  }

  @Test
  void theLogbackConfigurationProtectsEveryMessageWordOfAnyPattern() throws JoranException {
    LoggerContext context = new LoggerContext();
    // A deployment's own pattern (logging.pattern.console) that uses all three message words.
    context.putProperty("CONSOLE_LOG_PATTERN", "%level %m|%msg|%message%n");
    JoranConfigurator configurator = new JoranConfigurator();
    configurator.setContext(context);
    configurator.doConfigure(getClass().getResource("/logback-spring.xml"));

    Logger logger = context.getLogger("audit");
    OutputStreamAppender<ILoggingEvent> console =
        (OutputStreamAppender<ILoggingEvent>)
            context.getLogger(Logger.ROOT_LOGGER_NAME).getAppender("CONSOLE");
    PatternLayout layout =
        (PatternLayout) ((LayoutWrappingEncoder<ILoggingEvent>) console.getEncoder()).getLayout();
    LoggingEvent event =
        new LoggingEvent(
            Logger.class.getName(),
            context.getLogger(logger.getName()),
            Level.WARN,
            "Sign-in failed for {}",
            null,
            new Object[] {"eve\r\nINFO Sign-in succeeded for admin"});

    String line = layout.doLayout(event);

    String safe = "Sign-in failed for eve__INFO Sign-in succeeded for admin";
    assertThat(line).isEqualTo("WARN " + safe + "|" + safe + "|" + safe + System.lineSeparator());
    context.stop();
  }
}
