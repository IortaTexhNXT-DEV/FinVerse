package com.iortatechnxt.brokerverse.quotation.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iortatechnxt.brokerverse.quotation.domain.QuotationContent;
import org.springframework.stereotype.Component;

/** Writes and reads the JSON content of quotation versions (BRNB.020). */
@Component
public class QuotationContentCodec {

  private final ObjectMapper json;

  /**
   * Creates the codec.
   *
   * @param json JSON mapper
   */
  public QuotationContentCodec(ObjectMapper json) {
    this.json = json;
  }

  /**
   * Content as JSON.
   *
   * @param content content
   * @return JSON
   */
  public String write(QuotationContent content) {
    try {
      return json.writeValueAsString(content);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Quotation content cannot be written", e);
    }
  }

  /**
   * Content from JSON.
   *
   * @param text JSON
   * @return content
   */
  public QuotationContent read(String text) {
    try {
      return json.readValue(text, QuotationContent.class);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Quotation content cannot be read", e);
    }
  }
}
