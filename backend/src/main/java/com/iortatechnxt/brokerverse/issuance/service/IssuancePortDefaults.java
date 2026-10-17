package com.iortatechnxt.brokerverse.issuance.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Default adapters of the issuance ports, registered only when no module provides them. */
@Configuration(proxyBeanMethods = false)
public class IssuancePortDefaults {

  /**
   * No OCR until one is connected (SP SQ03).
   *
   * @return default adapter
   */
  @Bean
  @ConditionalOnMissingBean(OcrEngine.class)
  public OcrEngine defaultOcrEngine() {
    return new NoOcrEngine();
  }
}
