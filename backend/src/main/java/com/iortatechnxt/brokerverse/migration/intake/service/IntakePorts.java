package com.iortatechnxt.brokerverse.migration.intake.service;

import com.iortatechnxt.brokerverse.migration.intake.service.port.ExtractInbox;
import com.iortatechnxt.brokerverse.migration.intake.service.port.MaskingProvider;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Default adapters of the intake ports: the keyed HMAC masking and the console upload inbox (the
 * SFTP drop is parked). A deployment replaces either with its own bean.
 */
@Configuration(proxyBeanMethods = false)
public class IntakePorts {

  /**
   * Keyed HMAC masking.
   *
   * @param key masking key ({@code brokerverse.migration.masking-key})
   * @return provider
   */
  @Bean
  @ConditionalOnMissingBean(MaskingProvider.class)
  public MaskingProvider hmacMaskingProvider(
      @Value("${brokerverse.migration.masking-key:}") String key) {
    return new HmacMaskingProvider(key);
  }

  /**
   * Console upload: nothing to collect.
   *
   * @return inbox
   */
  @Bean
  @ConditionalOnMissingBean(ExtractInbox.class)
  public ExtractInbox consoleUploadInbox() {
    return new ExtractInbox() {
      @Override
      public boolean connected() {
        return false;
      }

      @Override
      public List<InboxFile> waiting() {
        return List.of();
      }

      @Override
      public void taken(InboxFile file) {
        // the console upload keeps no inbox
      }
    };
  }
}
